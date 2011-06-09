/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.lookup.deref.jenaimpl;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;

import org.squin.lookup.deref.impl.DerefTaskBase;


/**
 * 
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
public class RDFaReader
{
	final static private Logger log = LoggerFactory.getLogger( RDFaReader.class );

	// singleton access

	final static private RDFaReader singleton = new RDFaReader ();
	static { get(); }
	static public RDFaReader get () { return singleton; }

	// members

	final public boolean initSuccess;
	private Class xhtmlRDFaReaderClass = null;
	private Method xhtmlRDFaReadMethod = null;
	private Class htmlRDFaReaderClass = null;
	private Method htmlRDFaReadMethod = null;


	// initialization

	protected RDFaReader ()
	{
		try {
			xhtmlRDFaReaderClass = Class.forName( "net.rootdev.javardfa.jena.RDFaReader$XHTMLRDFaReader" );
			xhtmlRDFaReadMethod = xhtmlRDFaReaderClass.getMethod( "read", Model.class, InputStream.class, String.class );
		}
		catch ( Throwable e )
		{
			log.warn( "Initializing the XHTML RDFa reader failed ({}: {}).", e.getClass().getName(), e.getMessage() );
			initSuccess = false;
			return;
		}

		try {
			// The HTML RDFa parser has the nu.validator as a runtime dependency!
			Class.forName( "nu.validator.htmlparser.sax.HtmlParser" );
			htmlRDFaReaderClass = Class.forName( "net.rootdev.javardfa.jena.RDFaReader$HTMLRDFaReader" );
			htmlRDFaReadMethod = htmlRDFaReaderClass.getMethod( "read", Model.class, InputStream.class, String.class );
		}
		catch ( Throwable e )
		{
			log.warn( "Initializing the HTML RDFa reader failed ({}: {}). We use the XHTML RDFa reader as a fallback.", e.getClass().getName(), e.getMessage() );
			htmlRDFaReaderClass = xhtmlRDFaReaderClass;
			htmlRDFaReadMethod = xhtmlRDFaReadMethod;
		}

		log.debug( "Successfully initialized the RDFa reader." );
		initSuccess = true;
	}


	// operations

	public void readRDFaFromXHTML ( Model model, InputStream inStream, String urlString ) throws DerefTaskBase.DereferencingException
	{
		if ( ! initSuccess ) {
			throw new DerefTaskBase.DereferencingException( "RDFa reader unusable!" );
		}

		readRDFa( xhtmlRDFaReaderClass, xhtmlRDFaReadMethod, model, inStream, urlString );
	}

	public void readRDFaFromHTML ( Model model, InputStream inStream, String urlString ) throws DerefTaskBase.DereferencingException
	{
		if ( ! initSuccess ) {
			throw new DerefTaskBase.DereferencingException( "RDFa reader unusable!" );
		}

		readRDFa( htmlRDFaReaderClass, htmlRDFaReadMethod, model, inStream, urlString );
	}

	static protected void readRDFa ( Class readerClass, Method readMethod, Model model, InputStream inStream, String urlString ) throws DerefTaskBase.DereferencingException
	{
		try {
			readMethod.invoke( readerClass.newInstance(), model, inStream, urlString );
		}
		catch ( ExceptionInInitializerError e ) {
			throw new DerefTaskBase.DereferencingException( "Exception (type: " + e.getClass().getName() + ") caught: " + e.getMessage(), e );
		}
		catch ( IllegalAccessException e ) {
			throw new DerefTaskBase.DereferencingException( "Exception (type: " + e.getClass().getName() + ") caught: " + e.getMessage(), e );
		}
		catch ( IllegalArgumentException e ) {
			throw new DerefTaskBase.DereferencingException( "Exception (type: " + e.getClass().getName() + ") caught: " + e.getMessage(), e );
		}
		catch ( InstantiationException e ) {
			throw new DerefTaskBase.DereferencingException( "Exception (type: " + e.getClass().getName() + ") caught: " + e.getMessage(), e );
		}
		catch ( InvocationTargetException e ) {
			throw new DerefTaskBase.DereferencingException( "Exception (type: " + e.getClass().getName() + ") caught - caused by " + e.getTargetException().getClass().getName() + ": " + e.getTargetException().getMessage(), e );
		}
		catch ( NullPointerException e ) {
			throw new DerefTaskBase.DereferencingException( "Exception (type: " + e.getClass().getName() + ") caught: " + e.getMessage(), e );
		}
		catch ( SecurityException e ) {
			throw new DerefTaskBase.DereferencingException( "Exception (type: " + e.getClass().getName() + ") caught: " + e.getMessage(), e );
		}
		catch ( Exception e ) {
			throw new DerefTaskBase.DereferencingException( "Unexpected exception (type: " + e.getClass().getName() + ") caught: " + e.getMessage(), e );
		}
	}

}
