/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.cache.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.sparql.resultset.ResultSetMem;

import org.squin.Constants;
import org.squin.cache.QueryResultCache;


/**
 * A simple file-based implementation of a cache for results of SPARQL queries.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
public class QueryResultCacheImpl implements QueryResultCache
{
	// members

	static private Log log = LogFactory.getLog( QueryResultCacheImpl.class );

	final protected MessageDigest mdFct;
	final protected File pathOfQueryResultCache;
	final protected long maxQueryResultCacheEntryDuration; // milliseconds


	// initialization

	/**
	 *
	 * @param maxQueryResultCacheEntryDuration given in minutes
	 */
	public QueryResultCacheImpl ( String pathOfQueryResultCache, long maxQueryResultCacheEntryDuration )
	{
		this.maxQueryResultCacheEntryDuration = maxQueryResultCacheEntryDuration * 60 * 100;
		this.pathOfQueryResultCache = new File( pathOfQueryResultCache );

		assert ! this.pathOfQueryResultCache.exists() || this.pathOfQueryResultCache.isDirectory();

		if ( ! this.pathOfQueryResultCache.exists() )
		{
			try {
				this.pathOfQueryResultCache.mkdirs();
			}
			catch ( SecurityException e )
			{
				log.error( "Creating the query result cache directory caused a " + e.getClass().getName() + ": " + e.getMessage() );
				throw new IllegalArgumentException( "Creating the query result cache directory failed: " + e.getMessage(), e );
			}
		}

		try {
			mdFct = MessageDigest.getInstance( "SHA" );
		}
		catch ( NoSuchAlgorithmException e )
		{
			log.error( "Creating the SHA function caused a " + e.getClass().getName() + ": " + e.getMessage() );
			throw new Error( "Creating the SHA function failed: " + e.getMessage(), e );
		}
	}


	// implementation of the QueryResultCache interface

	public boolean hasResults ( String query, String responseContentType )
	{
		String digest = getDigest( query );
		File queryFile = new File( pathOfQueryResultCache, digest + ".rq" );

		if ( queryFile.lastModified() + maxQueryResultCacheEntryDuration < System.currentTimeMillis() ) {
			return false;
		}

		String cachedQuery = readQueryFile( queryFile );
		if ( ! cachedQuery.equals(query) ) {
			return false;
		}

		File resultsFile = new File( pathOfQueryResultCache, digest + getResultsFileSuffix(responseContentType) );
		return resultsFile.exists() && resultsFile.canRead();
	}

	public InputStream getResults ( String query, String responseContentType )
	{
		String digest = getDigest( query );
		File resultsFile = new File( pathOfQueryResultCache, digest + getResultsFileSuffix(responseContentType) );

		FileInputStream out;
		try {
			out = new FileInputStream( resultsFile );
		}
		catch ( Exception e )
		{
			log.error( "Creating an output stream from the reults file '" + resultsFile.getAbsolutePath() + "' caused a " + e.getClass().getName() + ": " + e.getMessage() );
			return null;
		}

		return out;
	}

	public boolean cacheResults ( String query, ResultSetMem results )
	{
		String digest = getDigest( query );
		File queryFile = new File( pathOfQueryResultCache, digest + ".rq" );
		if ( ! writeQueryFile(queryFile,query) ) {
			return false;
		}

		File resultsFileJSON = new File( pathOfQueryResultCache, digest + getResultsFileSuffix(Constants.MIME_TYPE_RESULT_JSON) );
		if ( ! writeResultsFile(resultsFileJSON,results,Constants.MIME_TYPE_RESULT_JSON) ) {
			return false;
		}

		File resultsFileXML = new File( pathOfQueryResultCache, digest + getResultsFileSuffix(Constants.MIME_TYPE_RESULT_XML) );
		if ( ! writeResultsFile(resultsFileXML,results,Constants.MIME_TYPE_RESULT_XML) ) {
			return false;
		}

		log.info( "Query results successfully cached." );

		return true;
	}


	// helpers

	public String getDigest ( String query )
	{
		byte[] queryAsBytes;
		try {
			queryAsBytes = query.getBytes( "UTF-8" );
		} catch( UnsupportedEncodingException e ) {
			log.error( "Creating a byte array from the query string caused a " + e.getClass().getName() + ": " + e.getMessage() );
			return "";
		}

		mdFct.reset();
		mdFct.update( queryAsBytes );
		byte[] digestAsBytes = mdFct.digest();

		String digestAsString = "";
		for ( int i = 0; i < digestAsBytes.length; ++i ) {
			digestAsString += String.valueOf( digestAsBytes[i] );
		}

		return digestAsString;
	}

	final protected boolean writeQueryFile ( File file, String query )
	{
		if ( ! createNewFile(file) ) {
			return false;
		}

		FileOutputStream fOut;
		try {
			fOut = new FileOutputStream( file );
		}
		catch ( Exception e )
		{
			log.error( "Creating an output stream from the query file '" + file.getAbsolutePath() + "' caused a " + e.getClass().getName() + ": " + e.getMessage() );
			return false;
		}

		ObjectOutputStream objOut;
		try {
			objOut = new ObjectOutputStream( fOut );
		}
		catch ( Exception e )
		{
			log.error( "Creating an output stream from the query file '" + file.getAbsolutePath() + "' caused a " + e.getClass().getName() + ": " + e.getMessage() );

			try {
				fOut.close();
			} catch ( IOException e2 ) {
				log.debug( "Closing the output stream for the query file '" + file.getAbsolutePath() + "' caused a " + e2.getClass().getName() + ": " + e2.getMessage() );
			}

			return false;
		}

		try {
			objOut.writeUTF( query );
		}
		catch ( IOException e )
		{
			log.error( "Writing to the query file '" + file.getAbsolutePath() + "' caused a " + e.getClass().getName() + ": " + e.getMessage() );

			try {
				objOut.close();
			} catch ( IOException e2 ) {
				log.debug( "Closing the output stream for the query file '" + file.getAbsolutePath() + "' caused a " + e2.getClass().getName() + ": " + e2.getMessage() );
			}

			return false;
		}

		try
		{
			objOut.flush();
			objOut.close();
		}
		catch ( IOException e )
		{
			log.debug( "Closing the output stream for the query file '" + file.getAbsolutePath() + "' caused a " + e.getClass().getName() + ": " + e.getMessage() );
			return false;
		}

		return true;
	}

	final protected String readQueryFile ( File file )
	{
		FileInputStream fIn;
		try {
			fIn = new FileInputStream( file );
		}
		catch ( Exception e )
		{
			log.error( "Creating an input stream from the query file '" + file.getAbsolutePath() + "' caused a " + e.getClass().getName() + ": " + e.getMessage() );
			return null;
		}

		ObjectInputStream objIn;
		try {
			objIn = new ObjectInputStream( fIn );
		}
		catch ( Exception e )
		{
			log.error( "Creating an input stream from the query file '" + file.getAbsolutePath() + "' caused a " + e.getClass().getName() + ": " + e.getMessage() );

			try {
				fIn.close();
			} catch ( IOException e2 ) {
				log.debug( "Closing the input stream for the query file '" + file.getAbsolutePath() + "' caused a " + e2.getClass().getName() + ": " + e2.getMessage() );
			}

			return null;
		}

		String query = null;
		try {
// 			int length = objIn.readInt();
			query = objIn.readUTF();
		}
		catch ( IOException e )
		{
			log.error( "Reading from the query file '" + file.getAbsolutePath() + "' caused a " + e.getClass().getName() + ": " + e.getMessage() );

			try {
				objIn.close();
			} catch ( IOException e2 ) {
				log.debug( "Closing the input stream for the query file '" + file.getAbsolutePath() + "' caused a " + e2.getClass().getName() + ": " + e2.getMessage() );
			}

			return null;
		}

		try {
			objIn.close();
		}
		catch ( IOException e ) {
			log.debug( "Closing the input stream for the query file '" + file.getAbsolutePath() + "' caused a " + e.getClass().getName() + ": " + e.getMessage() );
		}

		return query;
	}

	final protected boolean writeResultsFile ( File file, ResultSetMem results, String mimetype )
	{
		if ( ! createNewFile(file) ) {
			return false;
		}

		FileOutputStream out;
		try {
			out = new FileOutputStream( file );
		}
		catch ( Exception e )
		{
			log.error( "Creating an output stream from the results file '" + file.getAbsolutePath() + "' caused a " + e.getClass().getName() + ": " + e.getMessage() );
			return false;
		}

		results.reset();
		try {
			if ( mimetype == Constants.MIME_TYPE_RESULT_JSON ) {
				ResultSetFormatter.outputAsJSON( out, results );
			} else {
				ResultSetFormatter.outputAsXML( out, results );
			}
		}
		catch ( Exception e )
		{
			log.error( "Writing to the results file '" + file.getAbsolutePath() + "' caused a " + e.getClass().getName() + ": " + e.getMessage() );

			try {
				out.close();
			} catch ( IOException e2 ) {
				log.debug( "Closing the output stream for the results file '" + file.getAbsolutePath() + "' caused a " + e2.getClass().getName() + ": " + e2.getMessage() );
			}

			return false;
		}

		try
		{
			out.flush();
			out.close();
		}
		catch ( IOException e )
		{
			log.debug( "Closing the output stream for the results file '" + file.getAbsolutePath() + "' caused a " + e.getClass().getName() + ": " + e.getMessage() );
			return false;
		}

		return true;
	}

	final protected boolean createNewFile ( File f )
	{
		if ( f.exists() )
		{
			try {
				f.delete();
			}
			catch ( SecurityException e ) {
				log.error( "Deleting the file '" + f.getAbsolutePath() + "' caused a " + e.getClass().getName() + ": " + e.getMessage() );
			}
		}

		try {
			f.createNewFile();
		}
		catch ( Exception e )
		{
			log.error( "Creating the file '" + f.getAbsolutePath() + "' caused a " + e.getClass().getName() + ": " + e.getMessage() );
			return false;
		}

		return true;
	}

	static public String getResultsFileSuffix ( String mimetype )
	{
		if ( mimetype == Constants.MIME_TYPE_RESULT_JSON ) {
			return ".json";
		}
		else if ( mimetype == Constants.MIME_TYPE_RESULT_XML ) {
			return ".xml";
		}
		else {
			return null;
		}
	}

}
