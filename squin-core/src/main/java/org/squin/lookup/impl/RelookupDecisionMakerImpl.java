/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.lookup.impl;

import org.squin.lookup.FinishedURILookUp;
import org.squin.lookup.RelookupDecisionMaker;


/**
 * Default implementation of a {@link RelookupDecisionMaker}.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
public class RelookupDecisionMakerImpl implements RelookupDecisionMaker
{
// TODO: This implementation must be improved significantly. For instance, the
//       time that must have been elapsed before attempting to repeat look-ups
//       that failed before should be adjusted per URI so that look-up which
//       fail (almost) permanently are not re-tried as often as look-ups that
//       failed only once. Furthermore, the number of failed URI dereferencings
//       during a completed (or timed-out) look-up should be taken into account.

	static public long ELAPSED_TIME_TO_REPEAT_COMPLETED_LOOKUP = 3600000; // 1h
	static public long ELAPSED_TIME_TO_REPEAT_TIMED_OUT_LOOKUP = 120000; // 2min
	static public long ELAPSED_TIME_TO_REPEAT_FAILED_LOOKUP = 30000; // 30sec
	static public long ELAPSED_TIME_TO_REPEAT_INTERRUPTED_LOOKUP = 0; // immediately

	static private RelookupDecisionMaker singleton = null;
	static public RelookupDecisionMaker get ()
	{
		if ( singleton == null ) {
			singleton = new RelookupDecisionMakerImpl();
		}
		return singleton;
	}

	protected RelookupDecisionMakerImpl ()
	{
	}

	public boolean decideAboutRelookup ( int uriID, FinishedURILookUp status )
	{
		long timeDifferenceMillis = System.currentTimeMillis() - status.getFinishTimeMillis();

		if ( status.getResult().hasBeenInterrupted() && timeDifferenceMillis >= ELAPSED_TIME_TO_REPEAT_INTERRUPTED_LOOKUP ) {
			return true;
		}

		if ( status.getResult().hasTimedOut() && timeDifferenceMillis >= ELAPSED_TIME_TO_REPEAT_TIMED_OUT_LOOKUP ) {
			return true;
		}

		if ( status.getResult().hasThrownException() && timeDifferenceMillis >= ELAPSED_TIME_TO_REPEAT_FAILED_LOOKUP ) {
			return true;
		}

		if (    ! status.getResult().hasBeenInterrupted()
		     && ! status.getResult().hasTimedOut()
		     && ! status.getResult().hasThrownException()
		     && timeDifferenceMillis >= ELAPSED_TIME_TO_REPEAT_COMPLETED_LOOKUP ) {
			return true;
		}

		return false;
	}

}
