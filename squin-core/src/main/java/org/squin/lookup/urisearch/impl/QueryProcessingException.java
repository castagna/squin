/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.lookup.urisearch.impl;


/**
 * An exception that occured during praparation or execution of a URI search
 * query.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
public class QueryProcessingException extends Exception
{
	// initialization

	/**
	 * @param msg a short description of this exception
	 * @param cause the cause
	 */
	public QueryProcessingException ( String msg, Throwable cause )
	{
		super( msg, cause );
	}

	/**
	 * @param msg a short description of this exception
	 */
	public QueryProcessingException ( String msg )
	{
		super( msg );
	}

}
