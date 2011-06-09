/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.lookup;

import org.squin.common.TaskStatus;


/**
 * This interface represents a status of looking up a URI.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
public interface URILookUpStatus extends TaskStatus
{
	/**
	 * Returns a finished URI look-up status as a {@link FinishedURILookUp}
	 * object.
	 *
	 * @throws IllegalStateException if the corresponding look-up is not finished
	 *                    (i.e. if {@link org.squin.common.TaskStatus#isFinished}
	 *                    returns false).
	 */
	public FinishedURILookUp asFinishedURILookUp () throws IllegalStateException;
}
