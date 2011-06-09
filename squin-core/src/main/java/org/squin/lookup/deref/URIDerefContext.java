/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.lookup.deref;

import org.squin.Version;


/**
 * Represents the context of a dereferencing task.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
public class URIDerefContext
{
	// members

	static final public int CONNECT_TIMEOUT = 0; // 0 means no timeout (i.e. infinity)
	static final public int READ_TIMEOUT = 0;    // 0 means no timeout (i.e. infinity)
	static final public int MAXFILESIZE_DEFAULT = 100000000;
	static final public boolean ENABLE_RDFa_DEFAULT = true;

	static final public String USERAGENT_HEADER = "SQUIN + (v." + Version.getVersion() + ")";
	static final public String ACCEPT_HEADER =
	                              // official content types for RDF data
	                              "application/rdf+xml;q=1," +
	                              "text/turtle;q=1," +
	                              "text/n3;q=1," +
	                              // other (inofficial) content types for RDF data (sometimes used before the official ones became official)
	                              "text/rdf+xml;q=0.9," +
	                              "text/rdf+n3;q=0.9," +
	                              "application/n3;q=0.9," +
	                              "application/x-turtle;q=0.9," +
	                              // content types for HTML which may contain RDFa markup or relevant link elements
	                              "application/xhtml+xml;q=0.7," +
	                              "text/html;q=0.5," +
	                              // other content types that may yield RDF documents
	                              "text/xml;q=0.3," +
	                              "application/xml;q=0.3," +
	                              "application/rss+xml;q=0.3," +
	                              "application/octet-stream;q=0.2," +
	                              "text/plain;q=0.2";


	static final public boolean RECORD_PROVENANCE = false;
}
