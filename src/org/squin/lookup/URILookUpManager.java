/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.lookup;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.squin.common.Priority;
import org.squin.common.StatisticsProvider;
import org.squin.common.TaskListener;

/**
 * This interface represents a component that manages URI look-ups.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
public interface URILookUpManager extends StatisticsProvider
{
	/**
	 * Returns the look-up status of the given URI.
	 */
	public URILookUpStatus getLookUpStatus ( int uriID );

	/**
	 * Initiates a URI look-up if necessary.
	 *
	 * @param uriID identifier of the URI to be looked up
	 * @param priority the priority of this look-up request (must not be null)
	 * @param relookup the decision maker that decides about looking up URIs
	 *                 again that have been looked up before (must not be null)
	 * @param importer the data importer to be used for the data retrieved
	 *                 during the look-up (must not be null)
	 * @param listener a listener that has to be notified when the initiated
	 *                 look-up has finished (optional parameter; i.e. can be
	 *                 null)
	 * @return true, if an asynchronous URI look-up task has been queued; false,
	 *         if the given URI was already looked up before and a new look-up
	 *         is not necessary
	 * @throws IllegalStateException if it is impossible to accept look-up
	 *                     requests; e.g. because this look-up manager has
	 *                     already been shut down or is currently shut down
	 *                     (i.e. if {@link #shutdownNow} has been called)
	 */
	public boolean requestLookUp ( int uriID,
	                               Priority priority,
	                               RelookupDecisionMaker relookup,
	                               DataImporter importer,
	                               TaskListener<URILookUpResult> listener )  throws IllegalStateException;

	/**
	 * Initiates a URI look-up if necessary.
	 *
	 * @param uriID identifier of the URI to be looked up
	 * @param priority the priority of this look-up request (must not be null)
	 * @param relookup the decision maker that decides about looking up URIs
	 *                 again that have been looked up before (must not be null)
	 * @param importer the data importer to be used for the data retrieved
	 *                 during the look-up (must not be null)
	 * @return true, if an asynchronous URI look-up task has been queued; false,
	 *         if the given URI was already looked up before and a new look-up
	 *         is not necessary
	 * @throws IllegalStateException if it is impossible to accept look-up
	 *                     requests; e.g. because this look-up manager has
	 *                     already been shut down or is currently shut down
	 *                     (i.e. if {@link #shutdownNow} has been called)
	 */
	public boolean requestLookUp ( int uriID,
	                               Priority priority,
	                               RelookupDecisionMaker relookup,
	                               DataImporter importer )  throws IllegalStateException;

	/**
	 * Shuts down this look-up manager.
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
