/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.lookup.urisearch.impl;

import java.util.List;


/**
 * Prepares and executes URI search queries.
 * Implementations of this interface will usually be engine-specific (i.e. each
 * implementation is customized for a specific search engine).
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
public interface QueryProcessor
{
	/**
	 * Processes a query to search for the given URI.
	 * This method can be expected to run in a task-specific thread (i.e. it
	 * does not have to be thread-safe).
	 *
	 * @param uriID identifier of the URI that has to be searched
	 * @return the query result which is a list of identifiers for URIs
	 *         that refer to RDF documents which mention the given URI
	 */
	List<Integer> process ( int uriID ) throws QueryProcessingException;
}
