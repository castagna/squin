/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.lookup.urisearch.impl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.squin.common.Priority;
import org.squin.common.Statistics;
import org.squin.common.TaskListener;
import org.squin.common.impl.LockableTaskStatus;
import org.squin.common.impl.LockableTaskStatusBase;
import org.squin.common.impl.StatisticsImpl;
import org.squin.common.impl.TaskStatusIndexBase;
import org.squin.lookup.urisearch.URISearchResult;
import org.squin.lookup.urisearch.URISearchStatus;
import org.squin.lookup.urisearch.FinishedURISearch;
import org.squin.lookup.urisearch.SearchAgainDecisionMaker;
import org.squin.lookup.urisearch.URISearchManager;


/**
 * Base class for implementations of a {@link URISearchManager}.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
abstract public class URISearchManagerBase implements URISearchManager, TaskListener<URISearchResult>
{
	static final private Logger log = LoggerFactory.getLogger( URISearchManagerBase.class );

	// configuration parameters for the thread pool
	static final private int executorDefaultCorePoolSize = 10;
	static final private int executorDefaultMaximumPoolSize = 20;
	static final private long executorDefaultKeepAliveTime = 600; // 10 min
	static final private TimeUnit executorDefaultTimeUnit = TimeUnit.SECONDS;

	// members

	/** the thread pool used by this URI search manager */
	final private ThreadPoolExecutor executor;

	/** the index of URI search statuses used by this URI search manager */
	// Never access this member without synchronization!
	final private URISearchStatusIndex statuses = new URISearchStatusIndex ();

	/** denotes whether this URI search manager is currently shutting down */
	// Never access this member without synchronization!
	protected AtomicBoolean shuttingdown = new AtomicBoolean( false );

	/** denotes whether a previous shut down attempt for this URI search manager failed */
	protected boolean shutdownFailed = false;

	private long finishedTaskCount = 0;
	private long failedTaskCount = 0;
	private long overallQueueTime = 0;
	private long overallExecTime = 0;


	// initialization

	public URISearchManagerBase ()
	{
		executor = new ThreadPoolExecutor( executorDefaultCorePoolSize,
		                                   executorDefaultMaximumPoolSize,
		                                   executorDefaultKeepAliveTime,
		                                   executorDefaultTimeUnit,
		                                   new PriorityBlockingQueue<Runnable> () );
	}

	public void finalize ()
	{
		if ( ! executor.isTerminated() )
		{
			log.warn( "Finalizing a URI search manager (type: {}) that still seems to be running. Let's try to shut it down.", getClass().getName() );

			try {
				shutdownNow( 3000 );
			}
			catch ( Exception e ) {
				log.error( "Shutting down the URISearchManagerBase manager (type: {}) caused a {}: {}", new Object[] {getClass().getName(),e.getClass().getName(),e.getMessage()} );
			}
		}
	}


	// implementation of the URISearchManager interface

	public URISearchStatus getSearchStatus ( int uriID )
	{
		synchronized ( statuses ) {
			return statuses.getStatus( uriID );
		}
	}

	public URISearchResult requestSearch ( int uriID,
	                                       Priority priority,
	                                       SearchAgainDecisionMaker searchagain,
	                                       TaskListener<URISearchResult> listener ) throws IllegalStateException
	{
		log.debug( "Search for URI with identifier {} requested.", uriID );

		synchronized ( shuttingdown )
		{
			// check if dereferencing is (still) possible at all
			if ( executor.isShutdown() ) { throw new IllegalStateException( "Accepting URI search requests impossible: This " + this + " has already been shut down." ); }
			if ( shutdownFailed ) { throw new IllegalStateException( "Accepting URI search requests impossible: We already tried to shut down this " + this + "." ); }
			if ( shuttingdown.get() == true ) { throw new IllegalStateException( "Accepting URI search requests impossible: We are already in the process of shutting this " + this + " down." ); }

			// get the (previous) URI search status of the given URI
			URISearchStatus s;
			synchronized ( statuses ) {
				s = statuses.getLockedStatus( uriID );
			}

			// decide on how to handle the URI search request
			// depending on the (previous) URI search status
			URISearchResult result;
			try {
				if ( s.isUnknown() )
				{
					initiateURISearch( uriID, priority, listener );
					result = null;
				}
				else if ( s.isFinished() )
				{
					if ( searchagain.searchAgain(uriID,s.asFinishedURISearch()) )
					{
						initiateURISearch( uriID, priority, listener );
						result = null;
					}
					else {
						result = s.asFinishedURISearch().getResult();
						synchronized ( statuses ) {
							statuses.unlockStatus( uriID );
						}
					}
				}
				else if ( s.isPending() )
				{
					dealWithPendingTask( uriID, (PendingURISearch) s, priority, listener );
					result = null;
				}
				else
				{
					String msg = "Unknown URI search status (" + s.toString() + ") for URI with identifier " + uriID + ".";
					log.error( msg );

					synchronized ( statuses ) {
						statuses.unlockStatus( uriID );
					}

					throw new IllegalArgumentException( msg );
				}
			}
			catch ( Exception e ) {
				String msg = "Unexpected " + e.getClass().getName() + " caught: " + e.getMessage();
				log.warn( "{}  -- Trying to unlock the current URI search status for URI {} at least, before throwing an exception.", msg, uriID );
				synchronized ( statuses ) {
					statuses.unlockStatus( uriID );
				}
				throw new IllegalStateException( msg, e );
			}

			return result;
		}
	}

	public void shutdownNow ( long timeoutInMilliSeconds ) throws ExecutionException, TimeoutException
	{
		// check whether shut down already completed
		synchronized ( shuttingdown ) {
			if (    executor.isTerminated()
			     || ( ! shutdownFailed && shuttingdown.get() == true ) ) {
				return;
			}
		}

		log.debug( "Shutting down {} ...", this.toString() );

		// initiate shut down of the thread pool and wait for its termination
		executor.shutdownNow();
		boolean terminated;
		try {
			terminated = executor.awaitTermination( timeoutInMilliSeconds, TimeUnit.MILLISECONDS );
		}
		catch ( InterruptedException e ) {
			String msg = "Unexpected interruption (class:" + e.getClass().getName() + " message: " + e.getMessage() + ") of executor.awaitTermination during shut down of " + this.toString() + " (getActiveCount: " + executor.getActiveCount() + ", getTaskCount: " + executor.getTaskCount() + ", isShutdown: " + executor.isShutdown() + ", isTerminated: " + executor.isTerminated() + ", isTerminating: " + executor.isTerminating() + ").";
			log.error( msg );

			synchronized ( shuttingdown ) {
				shuttingdown.set( false );
				shutdownFailed = true;
			}

			throw new ExecutionException( msg, e );
		}

		if ( ! terminated ) {
			String msg = "Termination of the thread pool timed out during shut down of " + this.toString() + " (getActiveCount: " + executor.getActiveCount() + ", getTaskCount: " + executor.getTaskCount() + ", isShutdown: " + executor.isShutdown() + ", isTerminated: " + executor.isTerminated() + ", isTerminating: " + executor.isTerminating() + ").";
			log.warn( msg );

			synchronized ( shuttingdown ) {
				shuttingdown.set( false );
				shutdownFailed = true;
			}

			throw new TimeoutException( msg );
		}

		log.debug( "... shut down of {} completed successfully.", this.toString() );

		synchronized ( shuttingdown ) {
			shuttingdown.set( false );
		}
	}


	// implementation of the StatisticsProvider interface

	public Statistics getStatistics ()
	{
		StatisticsImpl.AttributeList statAttrs = new StatisticsImpl.AttributeList();
		statAttrs.add( "thread pool - largestPoolSize", executor.getLargestPoolSize() );
		statAttrs.add( "thread pool - completedTaskCount", executor.getCompletedTaskCount() );
		statAttrs.add( "thread pool - taskCount", executor.getTaskCount() );
		statAttrs.add( "finishedTaskCount", finishedTaskCount );
		statAttrs.add( "failedTaskCount", failedTaskCount );
		statAttrs.add( "overall queue time", overallQueueTime );
		statAttrs.add( "avg queue time", (finishedTaskCount != 0 ) ? overallQueueTime / finishedTaskCount : 0 );
		statAttrs.add( "overall exec. time", overallExecTime );
		statAttrs.add( "avg exec. time", (finishedTaskCount != 0 ) ? overallExecTime / finishedTaskCount : 0 );
		statAttrs.add( "statuses index", statuses.getStatistics() );
		return new StatisticsImpl( statAttrs );
	}


	// worker methods

	/**
	 * Creates a new URI search task, queues this task for asynchronous
	 * execution, and updates the URI search status for the given URI
	 * accordingly.
	 */
	protected void initiateURISearch ( int uriID,
	                                   Priority priority,
	                                   TaskListener<URISearchResult> listener )
	{
		log.debug( "Initiate search for URI with identifier {}.", uriID );

		URISearchTask task = new URISearchTask( uriID, priority, createQueryProcessor() );
		task.registerListener( this, Priority.HIGH );
		if ( listener != null ) {
			task.registerListener( listener, priority );
		}

		LockableURISearchStatus newStatus = new PendingURISearch( task );
		synchronized ( statuses ) {
			executor.execute( task );
			statuses.updateStatus( uriID, newStatus );
		}
	}

	/**
	 * Tries to deal with a URI search request for which a previously
	 * initiated URI search task is currently pending.
	 * Unlocks the current status.
	 */
	protected void dealWithPendingTask ( int uriID,
	                                     PendingURISearch currentStatus,
	                                     Priority priority,
	                                     TaskListener<URISearchResult> listener )
	{
		log.debug( "Trying to deal with pending URI search task for the URI with identifier {}.", uriID );

		URISearchTask task = currentStatus.task;
		synchronized ( task )
		{
			// (try to) register the given listener (if any)
			if ( listener != null )
			{
				try {
					task.registerListener( listener, priority );
				}
				catch ( IllegalStateException e ) {
					synchronized ( statuses ) {
						statuses.unlockStatus( uriID );
					}
// TODO:
					throw new UnsupportedOperationException( "We cannot attach a listener to a URI search task that is already notifying its listeners. Not sure what to do in this case :-(" );
				}
			}

			// adjust the priority of the (pending) task if it is still awaiting execution
			if (    ! task.isRunning()
			     && task.getPriority().compareTo(priority) > 0
			     && executor.remove(task) )
			{
				task.upgradePriority( priority );
				executor.execute( task );
			}

			// unlock the current status
			synchronized ( statuses ) {
				statuses.unlockStatus( uriID );
			}
		}
	}


	// implementation of the TaskListener<URISearchResult> interface

	public void handleCompletedTask ( URISearchResult result )
	{
		URISearchStatus s;
		synchronized ( statuses ) {
			s = statuses.getLockedStatus( result.getURIID() );

			if ( ! s.isPending() ) {
				log.warn( "Completion of search for URI {} reported (reported URI search result: {}) but the current URI search status ({}) for this URI is not 'pending'. Ignoring it.", new Object[] {result.getURIID(),result.toString(),s.toString()} );
				statuses.unlockStatus( result.getURIID() );
				return;
			}
		}

		// update statistics
		finishedTaskCount++;
		if ( result.isFailure() ) { failedTaskCount++; }
		overallQueueTime += result.getQueueTime();
		overallExecTime += result.getExecutionTime();

		LockableURISearchStatus newStatus = new FinishedURISearchImpl( result );
		synchronized ( statuses ) {
			statuses.updateStatus( result.getURIID(), newStatus );
		}
	}

	public void handleFailedTask ( URISearchResult result )
	{
		handleCompletedTask( result );
	}


	// abstract worker methods

	/**
	 * An implementation of this method must return a new {@link QueryProcessor}.
	 */
	abstract protected QueryProcessor createQueryProcessor ();


	// helpers

	static class URISearchStatusIndex extends TaskStatusIndexBase<URISearchStatus,LockableURISearchStatus>
	{
		final static protected UnknownURISearchStatus unknown = new UnknownURISearchStatus ();
		protected URISearchStatus getUnknownSingleton () { return unknown; }
		protected LockableURISearchStatus getNewUnknownStatus () { return new UnknownURISearchStatus(); }
	}

	static abstract class LockableURISearchStatus extends LockableTaskStatusBase
	                                              implements URISearchStatus, LockableTaskStatus
	{}

	static class UnknownURISearchStatus extends LockableURISearchStatus
	{
		public boolean isUnknown () { return true; }
		public boolean isPending () { return false; }
		public boolean isFinished () { return false; }
		public FinishedURISearch asFinishedURISearch () { throw new UnsupportedOperationException(); }
	}

	static class PendingURISearch extends LockableURISearchStatus
	{
		final public URISearchTask task;
		public PendingURISearch ( URISearchTask task ) { this.task = task; }
		public boolean isUnknown () { return false; }
		public boolean isPending () { return true; }
		public boolean isFinished () { return false; }
		public FinishedURISearch asFinishedURISearch () { throw new UnsupportedOperationException(); }
	}

	static class FinishedURISearchImpl extends LockableURISearchStatus implements FinishedURISearch
	{
		final public URISearchResult result;
		final public long finishTimeMillis = System.currentTimeMillis();
		public FinishedURISearchImpl ( URISearchResult result ) { this.result = result; }
		public boolean isUnknown () { return false; }
		public boolean isPending () { return false; }
		public boolean isFinished () { return true; }
		public FinishedURISearch asFinishedURISearch () { return this; }

		public long getFinishTimeMillis () { return finishTimeMillis; }
		public URISearchResult getResult () { return result; }
	}

}
