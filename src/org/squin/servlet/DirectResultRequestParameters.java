/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.servlet;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import java.net.URLDecoder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryException;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.sparql.resultset.ResultSetMem;
import com.hp.hpl.jena.sparql.util.Utils;

import org.squin.Constants;
import org.squin.cache.QueryResultCache;
import org.squin.cache.impl.QueryResultCacheImpl;


/**
 * Processes and provides the parameters of direct result requests.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
public class DirectResultRequestParameters
{
	// members

	protected String errorMsgs;

	protected String queryString;
	protected Query query;
	protected String responseContentType;
	protected boolean ignoreQueryCache;

	static final public String dfltResponseContentType = Constants.MIME_TYPE_RESULT_XML;
	static final public boolean dfltIgnoreQueryCache = false;


	// operation

	public boolean process ( HttpServletRequest req )
	{
		errorMsgs = "";

		// - the SPARQL query
		query = null;
		queryString = req.getParameter( "query" );
		if ( queryString == null || queryString.equals("") )
		{
			errorMsgs += "Query not specified. ";
		}
		else
		{
			try
			{
				query = QueryFactory.create( queryString );
				if ( ! query.isSelectType() ) {
					errorMsgs += "Query must be a SELECT query. ";
				}
			}
			catch ( QueryException e ) {
				errorMsgs += e.getMessage() + " ";
			}
		}

		// - the output type
		responseContentType = null;
		String output = req.getParameter( "output" );
		if ( output != null )
		{
			if ( output.equals("json") ) {
				responseContentType = Constants.MIME_TYPE_RESULT_JSON;
			} else if ( output.equals("xml") ) {
				responseContentType = Constants.MIME_TYPE_RESULT_XML;
			} else {
				errorMsgs += "Unsupported output format requested. ";
			}
		}
		else
		{
			String accept = req.getHeader( "ACCEPT" );
			if (    accept != null
			     && (    accept.contains(Constants.MIME_TYPE_RESULT_JSON)
			          || accept.contains(Constants.MIME_TYPE_JSON) ) ) {
				responseContentType = Constants.MIME_TYPE_RESULT_JSON;
			}
			else if (    accept != null
			          && (    accept.contains(Constants.MIME_TYPE_RESULT_XML)
			               || accept.contains(Constants.MIME_TYPE_XML1)
			               || accept.contains(Constants.MIME_TYPE_XML2) ) ) {
				responseContentType = Constants.MIME_TYPE_RESULT_XML;
			}
			else {
				responseContentType = dfltResponseContentType;
			}
		}

		ignoreQueryCache = dfltIgnoreQueryCache;
		String ignoreQueryCacheParam = req.getParameter( "ignoreQueryCache" );
		if ( ignoreQueryCacheParam != null )
		{
			if (    ignoreQueryCacheParam.equalsIgnoreCase("true")
			     || ignoreQueryCacheParam.equalsIgnoreCase("yes") ) {
				ignoreQueryCache = true;
			}
			else if (    ignoreQueryCacheParam.equalsIgnoreCase("false")
			          || ignoreQueryCacheParam.equalsIgnoreCase("no") ) {
				ignoreQueryCache = false;
			}
			else {
				errorMsgs += "Unsupported ignoreQueryCache parameter (" + ignoreQueryCacheParam + ") ";
			}
		}

		return ( errorMsgs.equals("") );
	}


	// accessors

	public String getErrorMsgs () {
		return errorMsgs;
	}

	public String getQueryString () {
		return queryString;
	}

	public Query getQuery () {
		return query;
	}

	public String getResponseContentType () {
		return responseContentType;
	}

	public boolean getIgnoreQueryCache () {
		return ignoreQueryCache;
	}

}