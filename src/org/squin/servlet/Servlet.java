/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.sparql.util.Utils;

import org.squin.Config;
import org.squin.dataset.QueriedDataset;
import org.squin.dataset.hashimpl.combined.QueriedDatasetImpl;
import org.squin.dataset.jenacommon.JenaIOBasedQueriedDataset;
import org.squin.ldcache.jenaimpl.JenaIOBasedLinkedDataCache;


/**
 * The base class for all servlets that process requests to the SQUIN service.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
abstract public class Servlet extends HttpServlet
{
	// members

	static private Log log = LogFactory.getLog( Servlet.class );

	static private Context namingContext = null;

	// helper methods

	final protected Config getConfig ()
	{
		Config config = (Config) getServletContext().getAttribute( "org.squin.servlet.Servlet.config" );
		if ( config == null )
		{
			config = new Config();

				Properties p = new Properties();
				// NOTE: configuration file location is set in web.xml
				
				String cf = getConfigFileLocation();
				try {
					File fi = new File(cf);
					p.load(new FileInputStream(fi));
					log.info("Using config file " + fi.getAbsolutePath());
				} catch (FileNotFoundException e) {
					log.warn("Failed to find config file '"+ cf + "' - using defaults");
				} catch (IOException e) {
					log.error("Failed to read config file '"+ cf + "': " + e.getMessage());
				}
				config.init(p);
			getServletContext().setAttribute( "org.squin.servlet.Servlet.config", config );
		}

		return config;
	}

	final protected JenaIOBasedLinkedDataCache getLinkedDataCache ()
	{
		JenaIOBasedLinkedDataCache ldcache = (JenaIOBasedLinkedDataCache) getServletContext().getAttribute( "org.squin.servlet.Servlet.ldcache" );
		if ( ldcache == null )
		{
			ldcache = createLinkedDataCache();
			getServletContext().setAttribute( "org.squin.servlet.Servlet.ldcache", ldcache );
		}

		return ldcache;
	}

	private JenaIOBasedLinkedDataCache createLinkedDataCache ()
	{
		log.debug( "Creating new JenaIOBasedLinkedDataCache object for the servlet." );

		QueriedDataset qds = new QueriedDatasetImpl();
		JenaIOBasedLinkedDataCache ldcache = new JenaIOBasedLinkedDataCache( new JenaIOBasedQueriedDataset(qds) );
		return ldcache;
	}

// 	static private void loadInitialGraphs ( SemanticWebClient semweb )
// 	{
// 
// 
// 		FileManager fm = new FileManager();
// 		fm.addLocatorFile( "tests" + FileManager.filePathSeparator + "data" );
// 
// 		try {
// 			dataModel = fm.loadModel( "data1.n3" );
// 		} catch ( Exception e ) {
// 			throw new Exception( "Loading the file with our test data caused a " + Utils.className(e) + ": " + e.getMessage(), e );
// 		}
// 
// 	}
// 

	static protected String getInitialFilesDirectory ()
	{
		return getStringFromContext("InitialFilesDirectory");
	}
	
	static protected String getConfigFileLocation ()
	{
		return getStringFromContext("ConfigFileLocation");
	}
	
	static private String getStringFromContext(String key) {
		Context nCtx = getNamingContext();
		try {
			return (String) nCtx.lookup( key );
		} catch ( NameNotFoundException e ) {
			return null;
		} catch ( NamingException e ) {
			log.error("Looking up the object named 'InitialFilesDirectory' caused a " + Utils.className(e) + ": " + e.getMessage());
			return null;
		}
	}

	static protected Context getNamingContext ()
	{
		if ( namingContext == null )
		{
			try {
				namingContext = (Context) ( new InitialContext() ).lookup( "java:comp/env" );
			} catch ( NamingException e ) {
				throw new Error( "Looking up the application naming context caused a " + Utils.className(e) + ": " + e.getMessage(), e );
			}
		}

		return namingContext;
	}
}