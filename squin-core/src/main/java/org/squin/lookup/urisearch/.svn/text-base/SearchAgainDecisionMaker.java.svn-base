/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.lookup.urisearch;


/**
 * This interface represents a component that decides about searching for
 * URIs again.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
public interface SearchAgainDecisionMaker
{
	/**
	 * Returns true if the given URI with the given search status has to be
	 * searched for again.
	 *
	 * @param uriID identifier of the URI in question
	 * @param status the current search status of the given URI
	 */
	boolean searchAgain ( int uriID, FinishedURISearch status );
}
