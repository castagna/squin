/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.lookup.deref.jenaimpl;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import org.openjena.atlas.lib.Sink;
import org.openjena.riot.Lang;
import org.openjena.riot.RiotReader;
import org.openjena.riot.WebContent;

import org.squin.common.Priority;
import org.squin.dataset.Triple;
import org.squin.dataset.jenacommon.EncodingTriplesIterator;
import org.squin.dataset.jenacommon.NodeDictionary;
import org.squin.lookup.DataImporter;
import org.squin.lookup.deref.DataAnalyzer;
import org.squin.lookup.deref.DereferencingResult;
import org.squin.lookup.deref.DiscoveredURI;
import org.squin.lookup.deref.TypeOfURIDiscovery;
import org.squin.lookup.deref.impl.DerefTaskBase;


/**
 * A {@link org.squin.lookup.deref.impl.DerefTask} that makes use
 * of the Jena framework to parse RDF data retrieved from the Web.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
public class JenaIOBasedDerefTask extends DerefTaskBase
{
	// members

	final private Logger log = LoggerFactory.getLogger( JenaIOBasedDerefTask.class );


	// initialization

	public JenaIOBasedDerefTask ( JenaIOBasedDerefContext derefCxt,
	                              int uriID,
	                              Priority priority,
	                              DataImporter importer,
	                              DataAnalyzer analyzer )
	{
		super( derefCxt, getURL(uriID,derefCxt.nodeDict), uriID, priority, importer, analyzer );
	}

	static private URL getURL ( int uriID, NodeDictionary nodeDict )
	{
		Node uriNode = nodeDict.getNode( uriID );
		if ( uriNode == null || ! uriNode.isURI() ) {
			throw new IllegalArgumentException( "Dereferencing requested for node ID " + uriID + "(" + uriNode + ") which is unknown or not a URI." );
		}

		String uriString = uriNode.getURI();
		URL url;
		try {
			url = new URL( uriString );
		}
		catch ( MalformedURLException e ) {
			throw new IllegalArgumentException( "Dereferencing requested for node ID " + uriID + "(" + uriString + ") which caused a " + e.getClass().getName() + ": " + e.getMessage(), e );
		}

		if ( ! url.getProtocol().equals("http") ) {
			throw new IllegalArgumentException( "Dereferencing requested for node ID " + uriID + "(" + uriString + ") which does not seem to be an HTTP scheme based URI (getProtocol: " + url.getProtocol() + ")." );
		}

		return url;
	}


	// implementation of abstract methods from the base class

	protected int getUriID ( URI uri )
	{
		Node uriNode = Node.createURI( uri.toString() );
		return ( (JenaIOBasedDerefContext) derefCxt ).nodeDict.createId( uriNode );
	}

	protected Set<DiscoveredURI> handleContent ( InputStream inStream, String contentType, String contentEncoding ) throws DereferencingException
	{
		Iterator<Triple> itTriple;
		if ( contentType == null || contentType.contains("html") ) {

			if ( RDFaReader.get().initSuccess ) {
				Model model = ModelFactory.createDefaultModel();
				try {
					if ( contentType != null && contentType.contains("xhtml") ) {
						RDFaReader.get().readRDFaFromXHTML( model, inStream, url.toString() );
					} else {
						RDFaReader.get().readRDFaFromHTML( model, inStream, url.toString() );
					}
				}
				catch ( DereferencingException e ) {
					throw new DereferencingException( "Reading RDFa from the content for URI <" + url.toString() + "> (ID: " + uriID + ") failed: " + e.getMessage(), e );
				}

				itTriple = new EncodingTriplesIterator( ((JenaIOBasedDerefContext) derefCxt).nodeDict,
				                                        model.getGraph().find(null,null,null) );
			}
			else {
				itTriple = null;
			}
		}
		else {
			Lang lang = guessLang( contentType );
			if ( lang == null ) {
				throw new DereferencingException( "Guessing the language of the content for URI <" + url.toString() + "> (ID: " + uriID + ") failed." );
			}

			TripleMaterializer tm = new TripleMaterializer( ((JenaIOBasedDerefContext) derefCxt).nodeDict );

			try {
				RiotReader.parseTriples( inStream, lang, url.toString(), tm );
			}
			catch ( Exception e ) {
				throw new DereferencingException( "Exception (type: " + e.getClass().getName() + ", first stack trace element: " + e.getStackTrace()[0].toString() + ") caught while parsing the content retrieved for URI <" + url.toString() + "> (ID: " + uriID + ", guessed language: " + (lang==null ? "null" : lang.getName()) + "): " + e.getMessage(), e );
			}

			itTriple = tm.triples.iterator();
		}

		if ( itTriple != null ) {
			try {
				initiateDataImport( url, itTriple );
			}
			catch ( Exception e ) {
				throw new DereferencingException( "Exception (type: " + e.getClass().getName() + ", first stack trace element: " + e.getStackTrace()[0].toString() + ") caught while importing the data retrieved for URI <" + url.toString() + "> (ID: " + uriID + "): " + e.getMessage(), e );
			}
		}

		return null;
	}


	// helper methods

	protected Lang guessLang ( String contentType )
	{
		String contentTypeClean = contentType;
		int idxSemicolon = contentTypeClean.indexOf( ";" );
		if ( idxSemicolon > -1 ) {
			contentTypeClean = contentTypeClean.substring( 0, idxSemicolon );
		}

		Lang lang;

		// In theory N-Triples format should be served with content type
		// "text/plain". In practice, however, several misconfigured Web
		// servers publish RDF/XML documents (or something else) with that
		// content type. Hence, we prefer the URL based language guessing
		// for that content type.
		if (    contentTypeClean.equals("text/plain")
		     && null != (lang = Lang.guess(url.toString()) ) ) {
			return lang;
		}

		// content type based language guessing for all other content types
		if ( null != (lang = mapContentTypeToLang.get(contentTypeClean) ) ) {
			return lang;
		}

		// URL based language guessing as fallback
		return Lang.guess( url.toString() );
	}


	static public class TripleMaterializer implements Sink<com.hp.hpl.jena.graph.Triple>
	{
		final public NodeDictionary nodeDict;
		final public List<Triple> triples = new ArrayList<Triple> ();

		public TripleMaterializer ( NodeDictionary nodeDict ) { this.nodeDict = nodeDict; }
		public void send ( com.hp.hpl.jena.graph.Triple t ) { triples.add( EncodingTriplesIterator.encode(nodeDict,t) ); }
		public void flush() { }
		public void close() {}
	}

	static public Map<String,Lang> mapContentTypeToLang = new HashMap<String,Lang> ();
	static {
		mapContentTypeToLang.put( WebContent.contentTypeRDFXML,         Lang.RDFXML );
		mapContentTypeToLang.put( WebContent.contentTypeXML,            Lang.RDFXML );
		mapContentTypeToLang.put( "text/rdf+xml",                       Lang.RDFXML );
		mapContentTypeToLang.put( "application/rss+xml",                Lang.RDFXML );
		mapContentTypeToLang.put( WebContent.contentTypeTurtle1,        Lang.TURTLE );
		mapContentTypeToLang.put( WebContent.contentTypeTurtle2,        Lang.TURTLE );
		mapContentTypeToLang.put( WebContent.contentTypeTurtle3,        Lang.TURTLE );
		mapContentTypeToLang.put( WebContent.contentTypeN3,             Lang.N3 );
		mapContentTypeToLang.put( WebContent.contentTypeN3Alt1,         Lang.N3 );
		mapContentTypeToLang.put( WebContent.contentTypeN3Alt2,         Lang.N3 );
		mapContentTypeToLang.put( WebContent.contentTypeNTriples,       Lang.NTRIPLES );
		mapContentTypeToLang.put( WebContent.contentTypeNTriplesAlt,    Lang.NTRIPLES );
    }

}
