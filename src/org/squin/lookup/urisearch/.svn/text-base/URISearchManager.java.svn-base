/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.lookup.urisearch;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.squin.common.Priority;
import org.squin.common.StatisticsProvider;
import org.squin.common.TaskListener;


/**
 * This interface represents a component that manages the searching for RDF
 * documents which mention a URI.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
public interface URISearchManager extends StatisticsProvider
{
	/**
	 * Returns the search status of the given URI.
	 */
	URISearchStatus getSearchStatus ( int uriID );

	/**
	 * Initiates the searching for a URI. If a search task for the given URI has
	 * been finished before and a new search is not necessary (according to the
	 * given {@link SearchAgainDecisionMaker}) then this method just returns the
	 * result of the finished search task. Otherwise, the method queues a new
	 * URI search task for asynchronous execution and returns null.
	 *
	 * @param uriID identifier of the URI to be searched for
	 * @param priority the priority of this request (must not be null)
	 * @param searchagain the decision maker that decides about searching for
	 *                URIs again if they have been searched for before (must
	 *                not be null)
	 * @param listener a listener that has to be notified when the initiated
	 *                 search has finished (optional parameter; i.e. may be
	 *                 null)
	 * @return either null (if a new search task has been queued for asynchronous
	 *         execution) or the result of the (previously) finished search for
	 *         the given URI
	 * @throws IllegalStateException if it is impossible to accept URI search
	 *                         requests; e.g. because this URI search manager
	 *                         has already been shut down or is currently shut
	 *                         down (i.e. if {@link #shutdownNow} has been
	 *                         called)
	 */
	URISearchResult requestSearch ( int uriID,
	                                       Priority priority,
	                                       SearchAgainDecisionMaker searchagain,
	                                       TaskListener<URISearchResult> listener ) throws IllegalStateException;

	/**
	 * Shuts down this URI search manager (terminates and forgets all running
	 * and queued search tasks).
	 *
	 * @param timeoutInMilliSeconds timeout (in milliseconds) after which
	 *                              the shut down process is interrupted
	 *                              and a TimeoutException is thrown
	 * @throws TimeoutException when the shut down process takes
	 *                          longer than the given timeout
	 * @throws ExecutionException when completing the shut down
	 *                            fails for another reason
	 */
	void shutdownNow ( long timeoutInMilliSeconds ) throws ExecutionException, TimeoutException;

}
