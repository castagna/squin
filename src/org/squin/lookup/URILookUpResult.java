/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.lookup;

import java.util.Set;

import org.squin.lookup.deref.DereferencingResult;


/**
 * This interface represents the result of a finished URI look-up.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
public interface URILookUpResult
{
	/**
	 * Returns the identifier of the URI that was looked up.
	 */
	public int getURIID ();

	/**
	 * Returns true if the corresponding URI look-up has been
	 * terminated due to timeout.
	 * Notice, a URI look-up that timed out may still have dereferenced URIs
	 */
	public boolean hasTimedOut ();

	/**
	 * Returns true if the corresponding URI look-up has been interrupted.
	 * Notice, a URI look-up that has been interrupted does not have
	 * dereferenced URIs
	 */
	public boolean hasBeenInterrupted ();

	/**
	 * Returns true if the execution of the corresponding URI look-up
	 * aborted with an exception.
	 */
	public boolean hasThrownException ();

	/**
	 * Returns a set of identifiers for URIs that have been dereferenced during
	 * the corresponding URI look-up.
	 *
	 * @throws UnsupportedOperationException when {@link #hasThrownException}
	 *                                       is true
	 */
	public Set<Integer> getIDsOfDereferencedURIs () throws UnsupportedOperationException;

	/**
	 * Returns the result of dereferencing the URI with the given identifier
	 * during the corresponding URI look-up (or null if the URI was not
	 * dereferenced during the look-up)
	 *
	 * @throws UnsupportedOperationException when {@link #hasThrownException}
	 *                                       is true
	 */
	public DereferencingResult getDereferencingResult ( Integer idOfDereferencedURI ) throws UnsupportedOperationException;

	/**
	 * Returns the corresponding exception if {@link #hasThrownException} is true.
	 *
	 * @throws UnsupportedOperationException when {@link #hasThrownException}
	 *                                       is false
	 */
	public Exception getException () throws UnsupportedOperationException;


	// statistics about the look-up process that led to this result

	/**
	 * Returns the time (in ms) the corresponding look-up task was spent in the
	 * queue. That is the interval between issuing the look-up task and starting
	 * to actual execute it.
	 */
	public long getQueueTime ();

	/**
	 * Returns the time (in ms) required to execute the corresponding look-up
	 * task.
	 */
	public long getExecutionTime ();

	public int getMaxStepsReachedCounter ();
}
