/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.lookup.urisearch.jenaimpl;

import org.squin.dataset.jenacommon.NodeDictionary;
import org.squin.lookup.urisearch.impl.QueryProcessor;
import org.squin.lookup.urisearch.impl.URISearchManagerBase;


/**
 * A {@link org.squin.lookup.urisearch.URISearchManager} that makes use of the
 * Jena framework to parse RDF data retrieved from the Web.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
public class JenaIOBasedURISearchManager extends URISearchManagerBase
{
	// members

	final public NodeDictionary nodeDict;


	// initialization

	public JenaIOBasedURISearchManager ( NodeDictionary nodeDict )
	{
		this.nodeDict = nodeDict;
	}


	// implementation of the URISearchManagerBase abstract worker methods

	protected QueryProcessor createQueryProcessor ()
	{
		return new QueryProcessorSindice( nodeDict );
	}

}
