/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.lookup.impl;

import org.squin.dataset.QueriedDataset;
import org.squin.lookup.deref.RederefDecisionMaker;
import org.squin.lookup.deref.URIDerefManager;
import org.squin.lookup.urisearch.SearchAgainDecisionMaker;
import org.squin.lookup.urisearch.URISearchManager;


/**
 * Represents the context for URI look-ups.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
public class URILookUpContext
{
	final public URIDerefManager derefMgr;
	final public RederefDecisionMaker rederef;
	final public URISearchManager searchMgr;
	final public SearchAgainDecisionMaker searchAgain;
	final public int maxsteps = 5;
	final public int timeout = 30000;

	final public boolean seeAlso;
	final protected int [] seeAlsoPredicateIDs;
	final protected QueriedDataset seeAlsoQueriedDataset;


	public URILookUpContext ( URIDerefManager derefMgr, RederefDecisionMaker rederef )
	{
		this( derefMgr, rederef, null, null, null, null );
	}

	public URILookUpContext ( URIDerefManager derefMgr, RederefDecisionMaker rederef, URISearchManager searchMgr, SearchAgainDecisionMaker searchAgain )
	{
		this( derefMgr, rederef, searchMgr, searchAgain, null, null );
	}

	public URILookUpContext ( URIDerefManager derefMgr,
	                          RederefDecisionMaker rederef,
	                          URISearchManager searchMgr,
	                          SearchAgainDecisionMaker searchAgain,
	                          QueriedDataset seeAlsoQueriedDataset,
	                          int [] seeAlsoPredicateIDs )
	{
		assert derefMgr != null;
		assert rederef != null;
		assert searchMgr == null || searchAgain != null; // if a URISearchManager is given we also need a SearchAgainDecisionMaker

		this.derefMgr = derefMgr;
		this.rederef = rederef;
		this.searchMgr = searchMgr;
		this.searchAgain = searchAgain;

		seeAlso = ( seeAlsoQueriedDataset != null && seeAlsoPredicateIDs != null );
		this.seeAlsoQueriedDataset = seeAlsoQueriedDataset;
		this.seeAlsoPredicateIDs = seeAlsoPredicateIDs;
	}

	public int [] getSeeAlsoPredicateIDs ()
	{
		return seeAlsoPredicateIDs;
	}

	public QueriedDataset getSeeAlsoQueriedDataset ()
	{
		return seeAlsoQueriedDataset;
	}

}
