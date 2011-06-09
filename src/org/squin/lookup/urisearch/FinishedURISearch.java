/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.lookup.urisearch;


/**
 * This interface represents a finished search for a URI.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
public interface FinishedURISearch extends URISearchStatus
{
	/**
	 * Returns the time at which the corresponding search was finished.
	 *
	 * @return the difference, measured in milliseconds, between the finish
	 *         time and midnight, January 1, 1970 UTC
	 */
	long getFinishTimeMillis ();

	/**
	 * Returns the result of the corresponding search.
	 */
	URISearchResult getResult ();
}
