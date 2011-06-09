/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.lookup.urisearch;

import org.squin.common.TaskStatus;


/**
 * This interface represents the status of a URI search.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
public interface URISearchStatus extends TaskStatus
{
	/**
	 * Returns a finished URI search status as
	 * a {@link FinishedURISearch} object.
	 *
	 * @throws IllegalStateException if the corresponding URI
	 *                       search is not finished (i.e. if
	 *                       {@link TaskStatus#isFinished}
	 *                       returns false).
	 */
	FinishedURISearch asFinishedURISearch () throws IllegalStateException;

}
