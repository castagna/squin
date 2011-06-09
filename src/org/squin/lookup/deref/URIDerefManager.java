/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.lookup.deref;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.squin.common.Priority;
import org.squin.common.StatisticsProvider;
import org.squin.common.TaskListener;
import org.squin.lookup.DataImporter;


/**
 * This interface represents a component that manages the dereferencing of
 * HTTP-scheme based URIs.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
public interface URIDerefManager extends StatisticsProvider
{
	/**
	 * Returns the identifier of the dereferenceable URI that corresponds to the
	 * given URI.
	 * The returned ID may be the same as the given ID (if the URI identified by
	 * that ID is dereferenceable) but it may also be another ID (for instance
	 * if the URI identified by the given ID is a hash URI; the returned ID
	 * identifies the URI without tzhe hash part); it may even be
	 * {@link org.squin.dataset.Triple#UNKNOWN_IDENTIFIER} if there is no
	 * dereferenceable URI related to the URI of the given ID.
	 */
	public int getAsDereferenceableURI ( int uriID );

	/**
	 * Returns the dereferencing status of the given URI.
	 */
	public DereferencingStatus getDereferencingStatus ( int uriID );

	/**
	 * Initiates the dereferencing of a URI.
	 * If a dereferencing task for the given URI has been finished before
	 * and a new dereferencing is not necessary (according to the given
	 * {@link RederefDecisionMaker}) then this method just returns the
	 * result of the finished dereferencing task. Otherwise, the method
	 * queues a new dereferencing task for asynchronous execution and
	 * returns null.
	 *
	 * @param uriID identifier of the URI to be dereferenced; this URI must be
	 *              dereferenceable (see {@link #getAsDereferenceableURI})
	 * @param priority the priority of this request (must not be null)
	 * @param rederef the decision maker that decides about dereferencing
	 *                URIs again which have been dereferenced before (must
	 *                not be null)
	 * @param importer the data importer to be used for the data retrieved
	 *                 during the dereferencing
	 * @param analyzer a data analyzer to be used for the data retrieved
	 *                 during the dereferencing (optional parameter; i.e. may
	 *                 be null)
	 * @param listener a listener that has to be notified when the initiated
	 *                 look-up has finished (optional parameter; i.e. may be
	 *                 null)
	 * @return either null (if a new dereferencing task has been queued for
	 *         asynchronous execution) or the result of the (previously)
	 *         finished dereferencing of the given URI
	 * @throws IllegalStateException if it is impossible to accept dereferencing
	 *                               requests; e.g. because this deref. manager
	 *                               has already been shut down or is currently
	 *                               shut down (i.e. if {@link #shutdownNow} has
	 *                               been called)
	 * @throws IllegalArgumentException if the URI identified by the given
	 *                                  ID is not dereferenceable (see
	 *                                  {@link #getAsDereferenceableURI})
	 */
	public DereferencingResult requestDereferencing ( int uriID,
	                                                  Priority priority,
	                                                  RederefDecisionMaker rederef,
	                                                  DataImporter importer,
	                                                  DataAnalyzer analyzer,
	                                                  TaskListener<DereferencingResult> listener ) throws IllegalStateException, IllegalArgumentException;

	/**
	 * Shuts down this dereferencing manager (terminates and forgets
	 * all running and queued dereferencing task, respectively).
	 *
	 * @param timeoutInMilliSeconds timeout (in milliseconds) after which
	 *                              the shut down process is interrupted
	 *                              and a TimeoutException is thrown
	 * @throws TimeoutException when the shut down process takes
	 *                          longer than the given timeout
	 * @throws ExecutionException when completing the shut down
	 *                            fails for another reason
	 */
	public void shutdownNow ( long timeoutInMilliSeconds ) throws ExecutionException, TimeoutException;

}
