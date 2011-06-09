/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.lookup.deref;

import com.hp.hpl.jena.sparql.util.Symbol;


/**
 * This class provides symbols for different ways in which URIs can
 * be discovered by dereferencing another URI.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
public class TypeOfURIDiscovery extends Symbol
{
	/**
	 * The discovered URI was found via a link element with rel attribute "meta"
	 * or "alternate" in an HTML document.
	 */
	final static public TypeOfURIDiscovery AlternateLinkInHTML = new TypeOfURIDiscovery( "AlternateLinkInHTML" );

	/**
	 * It is unknown how the discovered URI was found.
	 */
	final static public TypeOfURIDiscovery Unknown = new TypeOfURIDiscovery( "Unknown" );


	protected TypeOfURIDiscovery ( String name )
	{
		super( TypeOfURIDiscovery.class.getName() + "." + name );
	}
}
