/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.lookup.urisearch.jenaimpl;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Model;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;

import org.squin.dataset.jenacommon.NodeDictionary;
import org.squin.lookup.urisearch.impl.QueryProcessor;
import org.squin.lookup.urisearch.impl.QueryProcessingException;


/**
 * A {@link QueryProcessor} for the Sindice search engine.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
public class QueryProcessorSindice implements QueryProcessor
{
	// members

	static final public int defaultMaxResultPortions = 100;
	final protected NodeDictionary nodeDict;
	final protected int maxResultPortions;
	final protected SingleQueryProcessor worker = new SingleQueryProcessor();


	// initialization

	public QueryProcessorSindice ( NodeDictionary nodeDict )
	{
		this( nodeDict, defaultMaxResultPortions );
	}

	public QueryProcessorSindice ( NodeDictionary nodeDict, int maxResultPortions )
	{
		this.nodeDict = nodeDict;
		this.maxResultPortions = maxResultPortions;
	}


	// implementation of the QueryProcessor interface

	public List<Integer> process ( int uriID ) throws QueryProcessingException
	{
		List<Integer> result = new ArrayList<Integer> ();

		int curResultPortionNo = 1;
		List<Integer> curResultPortion;
		do {
			worker.setCurPortionNo( curResultPortionNo );
			curResultPortion = worker.process( uriID );
			result.addAll( curResultPortion );
			curResultPortionNo++;
		} while ( (curResultPortionNo <= maxResultPortions) && ! curResultPortion.isEmpty() );

		return result;
	}


	/**
	 * Helper class for single queries that give only a portion (10 elements) of
	 * the overall result.
	 */
	protected class SingleQueryProcessor extends HTTPbasedQueryProcessor
	{
		// members

		protected int curPortionNo;


		// accessor methods

		public void setCurPortionNo ( int curPortionNo ) { this.curPortionNo = curPortionNo; }


		// implementation of the HTTPbasedQueryProcessor interface

		final protected URL prepareQuery( int uriID ) throws QueryProcessingException
		{
			Node uriNode = nodeDict.getNode( uriID );
			if ( uriNode == null || ! uriNode.isURI() ) {
				throw new IllegalArgumentException( "URI search requested for node ID " + uriID + "(" + uriNode + ") which is unknown or not a URI." );
			}

			try {
				String uriString = uriNode.getURI();
				String encodedURI = URLEncoder.encode( uriString, "UTF-8" );
// 				String query = "http://sindice.com/query/lookup?page=" + String.valueOf(curPortionNo) + "&uri=" + encodedURI;
				String query = "http://api.sindice.com/v2/search?qt=term&page=" + String.valueOf(curPortionNo) + "&q=" + encodedURI;

				return new URL( query );
			}
			catch ( UnsupportedEncodingException e ) {
				throw new QueryProcessingException( "URL encoding failed.", e );
			}
			catch ( MalformedURLException e ) {
				throw new QueryProcessingException( "Creating the query URL failed.", e );
			}
		}

		final protected List<Integer> evaluateResult( Model queryResult ) throws QueryExecutionException
		{
			List<Integer> result = new ArrayList<Integer> ();

			String query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
			               "PREFIX sindice: <http://sindice.com/vocab/search#>" +
			               "PREFIX fields:  <http://sindice.com/vocab/fields#>" +
			               "SELECT DISTINCT ?link WHERE {" +
			               " ?result a sindice:Result ;" +
			               "         fields:format ?format ;" +
			               "         sindice:link ?link ." +
			               " FILTER ( regex(?format,\"RDF\") || regex(?format,\"RDFA\") )" +
			               "}";
			QueryExecution qe = QueryExecutionFactory.create( query, queryResult );
			ResultSet results = qe.execSelect();
			while ( results.hasNext() ) {
				String uriString = results.nextSolution().getResource( "link" ).getURI();
				Integer uriId = new Integer( nodeDict.createId(Node.createURI(uriString)) );
				result.add( uriId );
			}

			return result;
		}
	} // end of SingleQueryProcessor

}
