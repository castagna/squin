/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.lookup;


/**
 * This interface represents a finished {@link URILookUpStatus}.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
public interface FinishedURILookUp extends URILookUpStatus
{
	/**
	 * Returns the time at which the corresponding URI look-up was finished.
	 *
	 * @return the difference, measured in milliseconds, between the finish
	 *         time and midnight, January 1, 1970 UTC
	 */
	public long getFinishTimeMillis ();

	/**
	 * Returns the result of the corresponding URI look-up.
	 */
	public URILookUpResult getResult ();
}
