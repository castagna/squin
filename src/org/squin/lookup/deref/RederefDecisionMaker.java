/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.lookup.deref;


/**
 * This interface represents a component that decides about dereferencing URIs
 * again.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
public interface RederefDecisionMaker
{
	/**
	 * Returns true if the given URI with the given dereferencing status has to
	 * be dereferenced again.
	 *
	 * @param uriID identifier of the URI in question
	 * @param status the current dereferencing status of the given URI
	 */
	public boolean decideAboutRedereferencing ( int uriID, FinishedDereferencing status );
}
