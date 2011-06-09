/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.lookup.jenaimpl;

import com.hp.hpl.jena.vocabulary.RDFS;

import org.squin.dataset.jenacommon.JenaIOBasedQueriedDataset;
import org.squin.lookup.impl.URILookUpContext;
import org.squin.lookup.impl.URILookUpManagerBase;
import org.squin.lookup.deref.FinishedDereferencing;
import org.squin.lookup.deref.RederefDecisionMaker;
import org.squin.lookup.deref.jenaimpl.JenaIOBasedDerefManager;
import org.squin.lookup.urisearch.FinishedURISearch;
import org.squin.lookup.urisearch.SearchAgainDecisionMaker;
import org.squin.lookup.urisearch.URISearchManager;
import org.squin.lookup.urisearch.jenaimpl.JenaIOBasedURISearchManager;


/**
 * A {@link org.squin.lookup.URILookUpManager} that makes use of
 * the Jena framework to parse RDF data retrieved from the Web.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
public class JenaIOBasedURILookUpManager extends URILookUpManagerBase
{
	// initialization

	public JenaIOBasedURILookUpManager ( JenaIOBasedQueriedDataset dataset )
	{
		this( dataset,
		      false );  // no URI search by default
	}

	public JenaIOBasedURILookUpManager ( JenaIOBasedQueriedDataset dataset, boolean enableURISearch )
	{
		super( initURILookUpContext(dataset,enableURISearch) );
	}

	static public URILookUpContext initURILookUpContext ( JenaIOBasedQueriedDataset dataset, boolean enableURISearch )
	{
		int [] seeAlsoPredicateIDs = { dataset.nodeDict.createId(RDFS.seeAlso.asNode()) };

		URISearchManager searchMgr = ( enableURISearch ) ? new JenaIOBasedURISearchManager(dataset.nodeDict) : null;
		SearchAgainDecisionMaker searchAgain = ( enableURISearch ) ? new MySearchAgainDecisionMaker() : null;

		URILookUpContext cxt = new URILookUpContext( new JenaIOBasedDerefManager(dataset.nodeDict),
		                                             new MyRederefDecisionMaker(),
		                                             searchMgr,
		                                             searchAgain,
		                                             dataset,
		                                             seeAlsoPredicateIDs );
		return cxt;
	}


	// helpers

	static class MyRederefDecisionMaker implements RederefDecisionMaker
	{
		static final private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( MyRederefDecisionMaker.class );

		public boolean decideAboutRedereferencing ( int uriID, FinishedDereferencing status )
		{
			// TODO: provide a better heuristic, consider additional 
			// for instance, the outcome of the last dereferencing, the outcome of
			// a number of previous dereferencings (e.g. always everything okay,
			// permanent failures), an estimated change frequency, etc.
			long age = System.currentTimeMillis() - status.getFinishTimeMillis();
			boolean decision = age > 86400000; // older than 24 hrs?

			log.debug( "decideAboutRedereferencing for URI with identifier {} - decision: {}", uriID, decision );

			return decision;
		}
	}

	static class MySearchAgainDecisionMaker implements SearchAgainDecisionMaker
	{
		static final private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( MySearchAgainDecisionMaker.class );

		public boolean searchAgain ( int uriID, FinishedURISearch status )
		{
			// TODO: provide a better heuristic
			long age = System.currentTimeMillis() - status.getFinishTimeMillis();
			boolean decision = age > 259200000; // older than 3 days ?

			log.debug( "searchAgain for URI with identifier {} - decision: {}", uriID, decision );

			return decision;
		}
	}
}
