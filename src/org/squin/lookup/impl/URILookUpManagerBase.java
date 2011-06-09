/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.lookup.impl;

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
import org.squin.lookup.DataImporter;
import org.squin.lookup.FinishedURILookUp;
import org.squin.lookup.RelookupDecisionMaker;
import org.squin.lookup.URILookUpManager;
import org.squin.lookup.URILookUpResult;
import org.squin.lookup.URILookUpStatus;
import org.squin.lookup.deref.RederefDecisionMaker;


/**
 * Base class for implementations of a {@link URILookUpManager}.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
public class URILookUpManagerBase implements URILookUpManager, TaskListener<URILookUpResult>
{
	static final private Logger log = LoggerFactory.getLogger( URILookUpManagerBase.class );

	// configuration parameters for the thread pool
	static final public int executorDefaultCorePoolSize = 10;
	static final public int executorDefaultMaximumPoolSize = 20;
	static final public long executorDefaultKeepAliveTime = 600; // 10 min
	static final public TimeUnit executorDefaultTimeUnit = TimeUnit.SECONDS;

	// members

	final protected URILookUpContext lookupCxt;

	/** the thread pool used by this deref. manager */
	final protected ThreadPoolExecutor executor;

	/** the index of look-up statuses used by this look-up manager */
	// Never access this member without synchronization!
	final protected URILookUpStatusIndex statuses = new URILookUpStatusIndex ();

	/** denotes whether this look-up manager is currently shutting down */
	// Never access this member without synchronization!
	protected AtomicBoolean shuttingdown = new AtomicBoolean( false );

	/** denotes whether a previous shut down attempt for this look-up manager failed */
	protected boolean shutdownFailed = false;

	private long requestCount = 0;
	private long finishedTaskCount = 0;
	private long failedTaskCount = 0;
	private long timedOutTaskCount = 0;
	private long overallMaxStepsReachedCount = 0;
	private long overallQueueTime = 0;
	private long overallExecTime = 0;


	// initialization

	public URILookUpManagerBase ( URILookUpContext lookupCxt )
	{
		assert lookupCxt != null;
		this.lookupCxt = lookupCxt;

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
			log.warn( "Finalizing a look-up manager (type: {}) that still seems to be running. Let's try to shut it down.", getClass().getName() );

			try {
				shutdownNow( 3000 );
			}
			catch ( Exception e ) {
				log.error( "Shutting down the look-up manager (type: {}) caused a {}: {}", new Object[] {getClass().getName(),e.getClass().getName(),e.getMessage()} );
			}
		}
	}


	// implementation of the URILookUpManager interface

	public URILookUpStatus getLookUpStatus ( int uriID )
	{
		synchronized ( statuses ) {
			return statuses.getStatus( uriID );
		}
	}

	public boolean requestLookUp ( int uriID,
	                               Priority priority,
	                               RelookupDecisionMaker relookup,
	                               DataImporter importer,
	                               TaskListener<URILookUpResult> listener ) throws IllegalStateException
	{
		assert priority != null;
		assert relookup != null;
		assert importer != null;

		log.debug( "Look-up of URI with identifier {} requested.", uriID );

		synchronized ( shuttingdown )
		{
			// check if a look-up is (still) possible at all
			if ( executor.isShutdown() ) { throw new IllegalStateException( "Accepting look-up requests impossible: This " + this + " has already been shut down." ); }
			if ( shutdownFailed ) { throw new IllegalStateException( "Accepting look-up requests impossible: We already tried to shut down this " + this + "." ); }
			if ( shuttingdown.get() == true ) { throw new IllegalStateException( "Accepting look-up requests impossible: We are already in the process of shutting this " + this + " down." ); }

			requestCount++;

			// get the (previous) look-up status of the given URI
			URILookUpStatus s;
			synchronized ( statuses ) {
				s = statuses.getLockedStatus( uriID );
			}

			// decide on how to handle the look-up request
			// depending on the (previous) look-up status
			boolean result;
			try {
				if ( s.isUnknown() )
				{
					initiateLookUp( uriID, priority, s, importer, listener );
					result = true;
				}
				else if ( s.isFinished() )
				{
					if ( relookup.decideAboutRelookup(uriID,s.asFinishedURILookUp()) )
					{
						initiateLookUp( uriID, priority, s, importer, listener );
						result = true;
					}
					else {
						result = false;
						synchronized ( statuses ) {
							statuses.unlockStatus( uriID );
						}
					}
				}
				else if ( s.isPending() )
				{
					dealWithPendingTask( uriID, (PendingURILookUpStatus) s, importer, priority, listener );
					result = true;
				}
				else {
					String msg = "Unknown look-up status (" + s.toString() + ") for URI with identifier " + uriID + ".";
					log.error( msg );

					synchronized ( statuses ) {
						statuses.unlockStatus( uriID );
					}

					throw new IllegalArgumentException( msg );
				}
			}
			catch ( Exception e ) {
				String msg = "Unexpected " + e.getClass().getName() + " caught: " + e.getMessage();
				log.warn( "{}  -- Trying to unlock the current URI status for URI {} at least, before throwing an exception.", msg, uriID );
				synchronized ( statuses ) {
					statuses.unlockStatus( uriID );
				}
				throw new IllegalStateException( msg, e );
			}

			return result;
		}
	}

	public boolean requestLookUp ( int uriID,
	                               Priority priority,
	                               RelookupDecisionMaker relookup,
	                               DataImporter importer ) throws IllegalStateException
	{
		return requestLookUp( uriID, priority, relookup, importer, null );
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

		int timeoutDivisor = ( lookupCxt.searchMgr != null ) ? 3 : 2;

		ExecutionException derefMgrShutdownException = null;
		try {
			lookupCxt.derefMgr.shutdownNow( timeoutInMilliSeconds/timeoutDivisor );
		}
		catch ( Exception e ) {
			String msg = "Shutting down the deref manager of this look-up manager caused a " + e.getClass().getName() + " with message: " + e.getMessage();
			log.warn( msg + "  -- However, let's try to shut down the thread pool of this look-up manager at least ..." );
			derefMgrShutdownException = new ExecutionException( msg, e );
		}

		ExecutionException searchMgrShutdownException = null;
		if ( lookupCxt.searchMgr != null ) {
			try {
				lookupCxt.searchMgr.shutdownNow( timeoutInMilliSeconds/timeoutDivisor );
			}
			catch ( Exception e ) {
				String msg = "Shutting down the URI search manager of this look-up manager caused a " + e.getClass().getName() + " with message: " + e.getMessage();
				log.warn( msg + "  -- However, let's try to shut down the thread pool of this look-up manager at least ..." );
				searchMgrShutdownException = new ExecutionException( msg, e );
			}
		}

		executor.shutdownNow();

		boolean terminated;
		try {
			terminated = executor.awaitTermination( timeoutInMilliSeconds/timeoutDivisor, TimeUnit.MILLISECONDS );
		}
		catch ( InterruptedException e ) {
			String msg = "Unexpected interruption (class:" + e.getClass().getName() + " message: " + e.getMessage() + ") of executor.awaitTermination during shut down of " + this.toString() + " (getActiveCount: " + executor.getActiveCount() + ", getTaskCount: " + executor.getTaskCount() + ", isShutdown: " + executor.isShutdown() + ", isTerminated: " + executor.isTerminated() + ", isTerminating: " + executor.isTerminating() + ").";
			log.warn( msg );

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

		if ( derefMgrShutdownException != null || searchMgrShutdownException != null ) {
			synchronized ( shuttingdown ) {
				shuttingdown.set( false );
				shutdownFailed = true;
			}

			throw ( derefMgrShutdownException != null ) ? derefMgrShutdownException : searchMgrShutdownException;
		}

		log.debug( "... shut down of {} finished.", this.toString() );

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
		statAttrs.add( "requestCount", requestCount );
		statAttrs.add( "finishedTaskCount", finishedTaskCount );
		statAttrs.add( "timedOutTaskCount", timedOutTaskCount );
		statAttrs.add( "failedTaskCount", failedTaskCount );
		statAttrs.add( "overall queue time", overallQueueTime );
		statAttrs.add( "avg queue time", (finishedTaskCount != 0 ) ? overallQueueTime / finishedTaskCount : 0 );
		statAttrs.add( "overall exec. time", overallExecTime );
		statAttrs.add( "avg exec. time", (finishedTaskCount != 0 ) ? overallExecTime / finishedTaskCount : 0 );
		statAttrs.add( "overall maxStepsReachedCount", overallMaxStepsReachedCount );
		statAttrs.add( "avg maxStepsReachedCount", (finishedTaskCount != 0 ) ? overallMaxStepsReachedCount / (finishedTaskCount-failedTaskCount) : 0 );
		statAttrs.add( "statuses index", statuses.getStatistics() );
		statAttrs.add( "derefMgr", lookupCxt.derefMgr.getStatistics() );
		if ( lookupCxt.searchMgr != null ) { statAttrs.add( "searchMgr", lookupCxt.searchMgr.getStatistics() ); }
		return new StatisticsImpl( statAttrs );
	}


	// worker methods

	/**
	 * Creates a new look-up task, queues this task for asynchronous
	 * execution, and updates the look-up status for the given URI
	 * accordingly.
	 */
	protected void initiateLookUp ( int uriID,
	                                Priority priority,
	                                URILookUpStatus currentStatus,
	                                DataImporter importer,
	                                TaskListener<URILookUpResult> listener )
	{
		log.debug( "Initiate look-up of the URI with identifier {}.", uriID );

		URILookUpTask task = new URILookUpTask( uriID, priority, lookupCxt, importer, currentStatus );
		task.registerListener( this, Priority.HIGH );
		if ( listener != null ) {
			task.registerListener( listener, priority );
		}

		LockableURILookUpStatus newStatus = new PendingURILookUpStatus( task );
		synchronized ( statuses ) {
			statuses.updateStatus( uriID, newStatus );
			executor.execute( task );
		}
	}

	/**
	 * Tries to deal with a look-up request for which a previously initiated
	 * look-up task is currently pending.
	 * Unlocks the current status.
	 */
	protected void dealWithPendingTask ( int uriID,
	                                     PendingURILookUpStatus currentStatus,
	                                     DataImporter importer,
	                                     Priority priority,
	                                     TaskListener<URILookUpResult> listener )
	{
		log.debug( "Trying to deal with pending look-up task for the URI with identifier {}.", uriID );

		URILookUpTask task = currentStatus.task;
		synchronized ( task )
		{
			// check the given data importer
			if ( importer != null && ! task.isRegisteredDataImporter(importer) )
			{
				synchronized ( statuses ) {
					statuses.unlockStatus( uriID );
				}
				throw new UnsupportedOperationException( "We can not attach a second data importer to an already running look-up task (current: " + importer + ", given: " + task.importer + "). Not sure what to do in this case :-(" );
			}

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
					throw new UnsupportedOperationException( "We cannot attach a listener to a look-up task that is already notifying its listeners. Not sure what to do in this case :-(" );
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


	// implementation of the TaskListener<URILookUpResult> interface

	public void handleCompletedTask ( URILookUpResult result )
	{
		URILookUpStatus s;
		synchronized ( statuses ) {
			s = statuses.getLockedStatus( result.getURIID() );

			if ( ! s.isPending() ) {
				log.warn( "Completion of look-up for URI {} reported (reported look-up result: {}) but the current look-up status (current look-up status: {}) for this URI is not 'pending'. Ignoring it.", new Object[] {result.getURIID(),result.toString(),s.toString()} );
				statuses.unlockStatus( result.getURIID() );
				return;
			}
		}

		// update statistics
		finishedTaskCount++;
		if ( result.hasTimedOut() ) { timedOutTaskCount++; }
		if ( ! result.hasBeenInterrupted() && ! result.hasThrownException() ) { overallMaxStepsReachedCount += result.getMaxStepsReachedCounter(); }

		overallQueueTime += result.getQueueTime();
		overallExecTime += result.getExecutionTime();

		LockableURILookUpStatus newStatus = new FinishedURILookUpImpl( result );
		synchronized ( statuses ) {
			statuses.updateStatus( result.getURIID(), newStatus );
		}
	}

	public void handleFailedTask ( URILookUpResult result )
	{
		failedTaskCount++;
		handleCompletedTask( result );
	}


	// helpers

	static class URILookUpStatusIndex extends TaskStatusIndexBase<URILookUpStatus,LockableURILookUpStatus>
	{
		final static protected UnknownURILookUpStatus unknown = new UnknownURILookUpStatus ();
		protected URILookUpStatus getUnknownSingleton () { return unknown; }
		protected LockableURILookUpStatus getNewUnknownStatus () { return new UnknownURILookUpStatus(); }
	}

	static abstract class LockableURILookUpStatus extends LockableTaskStatusBase
	                                              implements URILookUpStatus, LockableTaskStatus
	{}

	static class UnknownURILookUpStatus extends LockableURILookUpStatus
	{
		public boolean isUnknown () { return true; }
		public boolean isPending () { return false; }
		public boolean isFinished () { return false; }
		public FinishedURILookUp asFinishedURILookUp () { throw new UnsupportedOperationException(); }
	}

	static class PendingURILookUpStatus extends LockableURILookUpStatus
	{
		final public URILookUpTask task;
		public PendingURILookUpStatus ( URILookUpTask task ) { this.task = task; }
		public boolean isUnknown () { return false; }
		public boolean isPending () { return true; }
		public boolean isFinished () { return false; }
		public FinishedURILookUp asFinishedURILookUp () { throw new UnsupportedOperationException(); }
	}

	static class FinishedURILookUpImpl extends LockableURILookUpStatus implements FinishedURILookUp
	{
		final public URILookUpResult result;
		final public long finishTimeMillis = System.currentTimeMillis();
		public FinishedURILookUpImpl ( URILookUpResult result ) { this.result = result; }
		public boolean isUnknown () { return false; }
		public boolean isPending () { return false; }
		public boolean isFinished () { return true; }
		public FinishedURILookUp asFinishedURILookUp () { return this; }

		public long getFinishTimeMillis () { return finishTimeMillis; }
		public URILookUpResult getResult () { return result; }
	}

}
