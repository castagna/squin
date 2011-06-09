/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.lookup.impl;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.squin.common.Priority;
import org.squin.common.TaskListener;
import org.squin.common.impl.TaskBase;
import org.squin.dataset.Triple;
import org.squin.lookup.DataImporter;
import org.squin.lookup.URILookUpResult;
import org.squin.lookup.URILookUpStatus;
import org.squin.lookup.deref.DataAnalyzer;
import org.squin.lookup.deref.DereferencingResult;
import org.squin.lookup.deref.DiscoveredURI;
import org.squin.lookup.urisearch.URISearchResult;


/**
 * Represents a task to look up a URI.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
public class URILookUpTask extends TaskBase<URILookUpResult>
{
	// members

	final private Logger log = LoggerFactory.getLogger( URILookUpTask.class );

	final public int uriID;
	final public URILookUpStatus previousStatus;
	final protected URILookUpContext lookupCxt;
	final protected DataImporter importer;

	final protected DataAnalyzer analyzer;
	final protected Map<Integer,Integer> stepCountersOfPendingDerefs = new HashMap<Integer,Integer> ();
	final protected AtomicBoolean pendingURISearch = new AtomicBoolean( false );

	final protected DereferencingResultListener derefListener = new DereferencingResultListener ();
	final protected URISearchResultListener searchListener = new URISearchResultListener ();

	// this member must only be accessed when stepCountersOfPendingDerefs is locked
	final protected Map<Integer,DereferencingResult> resultsOfDereferencedURIs = new HashMap<Integer,DereferencingResult> ();

	protected boolean stopped = false;

	protected int maxStepsReachedCounter = 0;
	protected int successfulDerefTaskCounter = 0;
	protected int failedDerefTaskCounter = 0;


	// initialization

	/**
	 * Creates a look-up tasks for the given URI with the given priority.
	 */
	public URILookUpTask ( int uriID,
	                       Priority priority,
	                       URILookUpContext lookupCxt,
	                       DataImporter importer,
	                       URILookUpStatus previousStatus )
	{
		super( priority );

		assert lookupCxt != null;
		assert importer != null;
		assert previousStatus != null;

		this.uriID = uriID;
		this.lookupCxt = lookupCxt;
		this.importer = importer;
		this.previousStatus = previousStatus;

		analyzer = ( lookupCxt.seeAlso ) ? new DataAnalyzerImpl() : null;
	}


	// accessor methods

	/**
	 * Returns true if the given {@link DataImporter} is the same
	 * as the {@link DataImporter} registered with this task.
	 */
	public boolean isRegisteredDataImporter ( DataImporter importer )
	{
		return this.importer == importer;
	}


	// implementation of the TaskBase<URILookUpResult> abstract methods

	public URILookUpResult createFailureResult ( Exception e )
	{
		return new Failed( uriID, getTimestamp(), getExecutionStartTimestamp(), e );
	}


	// implementation of the Callable interface

	synchronized public URILookUpResult call ()
	{
		synchronized ( pendingURISearch )
		{
			synchronized ( stepCountersOfPendingDerefs ) {
				dereferenceRecursively( uriID, 0 );
			}

			if ( lookupCxt.searchMgr != null )
			{
				URISearchResult r = lookupCxt.searchMgr.requestSearch( uriID, getPriority(), lookupCxt.searchAgain, searchListener );
				if ( r != null ) {
					if ( ! r.isFailure() ) {
						handleURISearchResult( r );
					}
				}
				else {
					pendingURISearch.set( true );
				}
			}

			if ( lookupCxt.seeAlso )
			{
				for ( int seeAlsoPredicateID : lookupCxt.getSeeAlsoPredicateIDs() ) {
					Iterator<Triple> it = lookupCxt.getSeeAlsoQueriedDataset().find( uriID, seeAlsoPredicateID, Triple.UNKNOWN_IDENTIFIER );
					while ( it.hasNext() ) {
						Triple seeAlsoTriple = it.next();
						log.debug( "Found a seeAlso triple for URI {} that refers to URI {}.", uriID, seeAlsoTriple.o );
						dereferenceRecursively( seeAlsoTriple.o, 0 );
					}
				}
			}

		} // Unlock pendingURISearch not before here to ensure that all
		  // initial tasks get queued before we even try to execute
		  // allSubTasksFinished for the first time.

		try {
			if ( lookupCxt.timeout == 0 ) {
				wait();
			} else {
				wait( lookupCxt.timeout );
			}
			stopped = true;
		}
		catch ( InterruptedException e )
		{
			stopped = true;
			log.warn( "URI look-up task interrupted. Ignoring it. (task type: {}, task: {}, interrupt type: {}, interrupt message: {})", new Object[] {getClass().getName(),toString(),e.getClass().getName(),e.toString()} );

			synchronized ( stepCountersOfPendingDerefs ) {
				resultsOfDereferencedURIs.clear();
			}

			return new Interrupted( uriID, getTimestamp(), getExecutionStartTimestamp() );
		}

		if ( allSubTasksFinished() ) {
			log.debug( "URI look-up task finished completely (successfulDerefTaskCounter: {}, failedDerefTaskCounter: {}, task type: {}, task: {})", new Object[] {successfulDerefTaskCounter,failedDerefTaskCounter,getClass().getName(),toString()} );
			return new Completed( uriID, getTimestamp(), getExecutionStartTimestamp(), maxStepsReachedCounter, resultsOfDereferencedURIs );
		} else {
			log.debug( "URI look-up task timed out (successfulDerefTaskCounter: {}, failedDerefTaskCounter: {}, task type: {}, task: {})", new Object[] {successfulDerefTaskCounter,failedDerefTaskCounter,getClass().getName(),toString()} );
			return new TimedOut( uriID, getTimestamp(), getExecutionStartTimestamp(), maxStepsReachedCounter, resultsOfDereferencedURIs );
		}
	}


	// helper methods

	protected boolean allSubTasksFinished ()
	{
		synchronized ( pendingURISearch ) {
			if ( pendingURISearch.get() == true ) {
				return false;
			}

			synchronized ( stepCountersOfPendingDerefs ) {
				return stepCountersOfPendingDerefs.isEmpty();
			}
		}
	}

	protected void handleURISearchResult ( URISearchResult result )
	{
		if ( stopped ) {
			return;
		}

		if ( result.getURIID() != uriID ) {
			log.warn( "Unexpected URI search result reported. Ignoring it. (task type: {}, task: {}, result type: {})", new Object[] {getClass().getName(),toString(),result.getClass().getName()} );
			return;
		}

		if ( ! result.isFailure() ) {
			log.debug( "Handling URI search result ...  (task type: {}, task: {}, result type: {})", new Object[] {getClass().getName(),toString(),result.getClass().getName()} );

			for ( Integer discoveredDocumentURI : result.getDiscoveredDocumentURIs() ) {
				log.debug( "Potentially relevant URI {} discovered by searching for URI {}.", discoveredDocumentURI, uriID );
				dereferenceRecursively( discoveredDocumentURI.intValue(), 0 );
			}
		}
		else {
			log.debug( "Search for URI {} failed with {}. (task type: {}, task: {}, result type: {})", new Object[] {result.getURIID(),result.getException(),getClass().getName(),toString(),result.getClass().getName()} );
		}

		synchronized ( pendingURISearch ) {
			pendingURISearch.set( false );
		}

		if ( allSubTasksFinished() ) {
			log.debug( "All sub-tasks finished. Waking up the main thread that is responsible for this task. (task type: {}, task: {})", getClass().getName(), toString() );
			synchronized( this ) {
				notify();
			}
		}
	}

	protected void handleDereferencingResult ( DereferencingResult result )
	{
		if ( stopped ) {
			return;
		}

		Integer id = Integer.valueOf( result.getURIID() );
		Integer currentStep;

		synchronized ( stepCountersOfPendingDerefs ) {
			currentStep = stepCountersOfPendingDerefs.get( id );

			if ( currentStep == null ) {
				log.warn( "Unexpected dereferencing result reported. Ignoring it. (task type: {}, task: {}, result type: {})", new Object[] {getClass().getName(),toString(),result.getClass().getName()} );
				return;
			}

			resultsOfDereferencedURIs.put( id, result );
		}

		if ( ! result.isFailure() ) {
			log.debug( "Handling dereferencing result ...  (task type: {}, task: {}, result type: {})", new Object[] {getClass().getName(),toString(),result.getClass().getName()} );
			successfulDerefTaskCounter++;
			handleDiscoveredURIs( result, currentStep.intValue() + 1 );
		}
		else {
			log.debug( "Dereferencing for URI {} failed with {}. (task type: {}, task: {}, result type: {})", new Object[] {result.getURIID(),result.getException(),getClass().getName(),toString(),result.getClass().getName()} );
			failedDerefTaskCounter++;
		}

		synchronized ( stepCountersOfPendingDerefs ) {
			stepCountersOfPendingDerefs.remove( id );
		}

		if ( allSubTasksFinished() ) {
			log.debug( "All sub-tasks finished. Waking up the main thread that is responsible for this task. (task type: {}, task: {})", getClass().getName(), toString() );
			synchronized( this ) {
				notify();
			}
		}
	}

	/**
	 * Attempts to initiate the dereferencing of URIs that had been discovered
	 * during a URI dereferencing with the given result.
	 */
	protected void handleDiscoveredURIs ( DereferencingResult result, int currentStep )
	{
		if ( currentStep >= lookupCxt.maxsteps ) {
			return;
		}

		if ( result.hasBeenRedirected() ) {
			int redirectionCode = result.getRedirectionCode();
			int redirectionUriID = result.getRedirectionURI();
			log.debug( "Relevant URI {} discovered by dereferencing the URI {} during the look-up of URI {} (redirection code: {}).", new Object[] {redirectionUriID,result.getURIID(),uriID,redirectionCode} );
if ( redirectionUriID == 0 ) { System.err.println("redirectionUriID = " + redirectionUriID); throw new Error("redirectionUriID = " + redirectionUriID); }
			dereferenceRecursively( redirectionUriID, currentStep );
		}

		if ( result.hasDiscoveredOtherURIs() ) {
			for ( DiscoveredURI d : result.getDiscoveredURIs() ) {
				log.debug( "Relevant URI {} discovered by dereferencing the URI {} during the look-up of URI {} (discovery type: {}).", new Object[] {d.uriID,result.getURIID(),this.uriID,d.discoveryType.toString()} );
				dereferenceRecursively( d.uriID, currentStep );
			}
		}
	}

	protected void dereferenceRecursively ( int uriID, int currentStep )
	{
		if ( stopped ) {
			return;
		}

		// ignore URIs for which the maximum number of
		// recursive dereferencing steps has been reached
		if ( currentStep >= lookupCxt.maxsteps ) {
			maxStepsReachedCounter++;
			return;
		}

		int derefUriID = lookupCxt.derefMgr.getAsDereferenceableURI( uriID );
		if ( derefUriID == Triple.UNKNOWN_IDENTIFIER ) {
			log.debug( "No dereferenceable URI for the URI with identifier {} - ignoring it.", uriID );
			return;
		}

		Integer id = Integer.valueOf( derefUriID );
		Integer cs = Integer.valueOf( currentStep );

		synchronized ( stepCountersOfPendingDerefs ) {
			// ignore URIs that have already been
			// dereferenced during this look-up
			if ( resultsOfDereferencedURIs.containsKey(id) ) {
				return;
			}

			// ignore URIs that are currently being dereferenced
			// (However, adjust their step counter if necessary.)
			Integer csCurrent = stepCountersOfPendingDerefs.get( id );
			if ( csCurrent != null ) {
				if ( csCurrent.compareTo(cs) > 0 ) {
					stepCountersOfPendingDerefs.put( id, cs );
				}
				return;
			}

			// remember the step counter for the URI before
			// requesting the dereferencing for that URI
			stepCountersOfPendingDerefs.put( id, cs );
		}

		DereferencingResult result;
		try {
			result  = lookupCxt.derefMgr.requestDereferencing( derefUriID,
			                                                   getPriority(),
			                                                   lookupCxt.rederef,
			                                                   importer,
			                                                   analyzer,
			                                                   derefListener );
		} catch ( IllegalStateException e ) {
			// This exception will be caught while we are in the processing of
			// shutting down the deref manager. We can safely ignore this "issue"
			// because we are also shutting down the look-up manager at the same
			// time.
			log.debug( "Requesting the dereferencing of a URI caused a {}: {}", e.getClass().getName(),e.getMessage() );

			synchronized ( stepCountersOfPendingDerefs ) {
				stepCountersOfPendingDerefs.remove( id );
			}

			return; 
		}

		if ( result != null ) {
			log.debug( "Handling previous dereferencing result ...  (task type: {}, task: {}, result type: {})", new Object[] {getClass().getName(),toString(),result.getClass().getName()} );
			handleDiscoveredURIs( result, currentStep + 1 );

			synchronized ( stepCountersOfPendingDerefs ) {
				stepCountersOfPendingDerefs.remove( id );
			}
		}
	}


	@Override
	public boolean equals ( Object o )
	{
		return ( o instanceof URILookUpTask ) && ((URILookUpTask) o).uriID == uriID;
	}

	@Override
	public String toString ()
	{
		return "URILookUpTask(uriID=" + uriID + ")";
	}


	static abstract class URILookUpResultBase implements URILookUpResult
	{
		final public int uriID;
		final public long queueTime;
		final public long execTime;

		public URILookUpResultBase ( int uriID, long taskInitTimestamp, long taskStartTimestamp )
		{
			this.uriID = uriID;
			queueTime = taskStartTimestamp - taskInitTimestamp;
			execTime = System.currentTimeMillis() - taskStartTimestamp;
		}

		public int getURIID () { return uriID; }
		public Set<Integer> getIDsOfDereferencedURIs () { throw new UnsupportedOperationException(); }
		public DereferencingResult getDereferencingResult ( Integer idOfDereferencedURI ) { throw new UnsupportedOperationException(); }
		public Exception getException () { throw new UnsupportedOperationException(); }
		public long getQueueTime () { return queueTime; }
		public long getExecutionTime () { return execTime; }
		public int getMaxStepsReachedCounter () { throw new UnsupportedOperationException(); }
	}

	static class Interrupted extends URILookUpResultBase
	{
		public Interrupted ( int uriID, long taskInitTimestamp, long taskStartTimestamp ) { super(uriID,taskInitTimestamp,taskStartTimestamp); }
		public boolean hasTimedOut () { return false; }
		public boolean hasBeenInterrupted () { return true; }
		public boolean hasThrownException () { return false; }
	}

	static class Completed extends URILookUpResultBase
	{
		final public int maxStepsReachedCounter;
		final protected Map<Integer,DereferencingResult> resultsOfDereferencedURIs;
		public Completed ( int uriID, long taskInitTimestamp, long taskStartTimestamp, int maxStepsReachedCounter, Map<Integer,DereferencingResult> resultsOfDereferencedURIs ) { super(uriID,taskInitTimestamp,taskStartTimestamp); this.maxStepsReachedCounter = maxStepsReachedCounter; this.resultsOfDereferencedURIs = resultsOfDereferencedURIs; }
		public boolean hasTimedOut () { return false; }
		public boolean hasBeenInterrupted () { return false; }
		public boolean hasThrownException () { return false; }

		@Override
		public Set<Integer> getIDsOfDereferencedURIs () { return resultsOfDereferencedURIs.keySet(); }

		@Override
		public DereferencingResult getDereferencingResult ( Integer idOfDereferencedURI ) { return resultsOfDereferencedURIs.get(idOfDereferencedURI); }

		@Override
		public int getMaxStepsReachedCounter () { return maxStepsReachedCounter; }
	}

	static class TimedOut extends Completed
	{
		public TimedOut ( int uriID, long taskInitTimestamp, long taskStartTimestamp, int maxStepsReachedCounter, Map<Integer,DereferencingResult> resultsOfDereferencedURIs ) { super(uriID,taskInitTimestamp,taskStartTimestamp,maxStepsReachedCounter,resultsOfDereferencedURIs); }

		@Override
		public boolean hasTimedOut () { return true; }
	}

	static class Failed extends URILookUpResultBase
	{
		final protected Exception e;
		public Failed ( int uriID, long taskInitTimestamp, long taskStartTimestamp, Exception e ) { super(uriID,taskInitTimestamp,taskStartTimestamp); this.e = e; }
		public boolean hasTimedOut () { return false; }
		public boolean hasBeenInterrupted () { return false; }
		public boolean hasThrownException () { return true; }

		@Override
		public Exception getException () { return e; }
	}


	class DataAnalyzerImpl extends DataAnalyzer
	{
		public DataAnalyzingIterator createDataAnalyzingIterator( Iterator<Triple> input ) { return new DataAnalyzingIteratorImpl(input); }

		class DataAnalyzingIteratorImpl extends DataAnalyzer.DataAnalyzingIterator
		{
			public DataAnalyzingIteratorImpl ( Iterator<Triple> input ) { super( input ); }
			protected void analyze ( Triple t )
			{
				if ( t.s == uriID ) {
					for ( int seeAlsoPredicateID : lookupCxt.getSeeAlsoPredicateIDs() ) {
						if ( t.p == seeAlsoPredicateID ) {
							log.debug( "Retrieved a relevant seeAlso triple for URI {} that refers to URI {}.", uriID, t.o );
							dereferenceRecursively( t.o, 0 );
						}
					}
				}
			}
		}
	}


	class DereferencingResultListener implements TaskListener<DereferencingResult>
	{
		public void handleCompletedTask ( DereferencingResult result ) { handleDereferencingResult( result ); }
		public void handleFailedTask ( DereferencingResult result ) { handleDereferencingResult( result ); }
	}

	class URISearchResultListener implements TaskListener<URISearchResult>
	{
		public void handleCompletedTask ( URISearchResult result ) { handleURISearchResult( result ); }
		public void handleFailedTask ( URISearchResult result ) { handleURISearchResult( result ); }
	}

}
