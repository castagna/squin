/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.lookup.urisearch.jenaimpl;

import java.net.URL;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;

// import org.openjena.riot.WebReader;

import org.squin.lookup.urisearch.impl.QueryProcessor;
import org.squin.lookup.urisearch.impl.QueryProcessingException;


/**
 * A base class for all URI search query processors that use a HTTP based API
 * which returns RDF documents.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
abstract public class HTTPbasedQueryProcessor implements QueryProcessor
{
	final private Logger logger = LoggerFactory.getLogger( HTTPbasedQueryProcessor.class );


	// implementation of the QueryProcessor interface

	public List<Integer> process ( int uriID ) throws QueryProcessingException
	{
		logger.debug( "prepare query URL for URI with ID {}", uriID );
		URL queryURL = prepareQuery( uriID );

		logger.debug( "execute URI search query ({})", queryURL.toString() );
		Model resultModel;
		try {
			resultModel = executeQuery( queryURL );
		}
		catch ( QueryExecutionException e ) {
			String msg = "Execution the URI search query failed: " + e.getMessage();
			logger.debug( msg );
			throw new QueryProcessingException( msg, e );
		}

		logger.debug( "evaluate URI search result (" + queryURL.toString() + ")" );

		if ( resultModel == null )
			return new ArrayList<Integer> ();

		try {
			return evaluateResult( resultModel );
		}
		catch ( QueryExecutionException e ) {
			String msg = "Result evaluation failed: " + e.getMessage();
			logger.debug( msg );
			throw new QueryProcessingException( msg, e );
		}
	}


	// abstract methods to be implemented in sub-classes

	/**
	 * Creates the URL for the specific search service.
	 */
	abstract protected URL prepareQuery( int uriID ) throws QueryProcessingException;

	/**
	 * Evaluates the search result (expressed in the given RDF document) to
	 * extract the requested RDF documents (resp. their URLs).
	 */
	abstract protected List<Integer> evaluateResult( Model queryResult ) throws QueryExecutionException;


	// operations

	protected Model executeQuery ( URL queryURL ) throws QueryExecutionException
	{
// The new RIOT way - doesn't work yet (as of ARQ 2.8.8)
// 		Model m = ModelFactory.createDefaultModel();
// 		try {
// 			WebReader.readGraph( m.getGraph(), queryURL.toString() );
// 
// Hence, we must use the old Jena way:
		Model m;
		try {
			m = FileManager.get().loadModel( queryURL.toString() );
		}
		catch ( Exception e ) {
			throw new QueryExecutionException( "Reading the RDF data failed with a " + e.getClass().getName() + ": " + e.getMessage(), e );
		}

		return m;
	}


	protected class QueryExecutionException extends Exception
	{
		public QueryExecutionException ( String msg, Throwable cause )
		{
			super( msg, cause );
		}

		public QueryExecutionException ( String msg )
		{
			super( msg );
		}
	}

}
