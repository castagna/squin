/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.lookup;


/**
 * This interface represents a component that decides about looking up URIs
 * again.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
public interface RelookupDecisionMaker
{
	/**
	 * Returns true if the given URI with the given look-up status has to be
	 * looked up again.
	 *
	 * @param uriID identifier of the URI in question
	 * @param status the current look-up status of the given URI
	 */
	public boolean decideAboutRelookup ( int uriID, FinishedURILookUp status );
}
