package org.squin.example;

import org.squin.dataset.QueriedDataset;
import org.squin.dataset.hashimpl.individual.QueriedDatasetImpl;
import org.squin.dataset.jenacommon.JenaIOBasedQueriedDataset;
import org.squin.engine.LinkTraversalBasedQueryEngine;
import org.squin.engine.LinkedDataCacheWrappingDataset;
import org.squin.ldcache.jenaimpl.JenaIOBasedLinkedDataCache;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;

public class HelloWorld {

	public static void main(String[] args) {
		LinkTraversalBasedQueryEngine.register();
		QueriedDataset qds = new QueriedDatasetImpl();
		JenaIOBasedQueriedDataset qdsWrapper = new JenaIOBasedQueriedDataset( qds );
		JenaIOBasedLinkedDataCache ldcache = new JenaIOBasedLinkedDataCache( qdsWrapper );
		Dataset dsARQ = new LinkedDataCacheWrappingDataset( ldcache );
		
		String queryString = 
			"PREFIX swc: <http://data.semanticweb.org/ns/swc/ontology#>" +  
			"PREFIX swrc: <http://swrc.ontoware.org/ontology#>" + 
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" +
			"PREFIX owl: <http://www.w3.org/2002/07/owl#>" + 
			"PREFIX foaf:   <http://xmlns.com/foaf/0.1/>" +
			"SELECT DISTINCT ?author ?phone" +
			"WHERE {" +
			"    <http://data.semanticweb.org/conference/eswc/2009/proceedings> swc:hasPart ?pub ." + 
			"    ?pub swc:hasTopic ?topic ." + 
			"    ?topic rdfs:label ?topicLabel ." + 
			"    FILTER regex ( str(?topicLabel), \"ontology_engineering\", \"i\" ) ." + 
			"    ?pub swrc:author ?author ." + 
			"    { ?author owl:sameAs ?authAlt } UNION { ?authAlth owl:sameAs ?author }" + 
			"    ?authAlt foaf:phone ?phone ." + 
			"}";
		QueryExecution qe = QueryExecutionFactory.create( queryString, dsARQ );
		ResultSet results = qe.execSelect();
		System.out.println(ResultSetFormatter.asText(results));
		try {
			ldcache.shutdownNow( 4000 ); // 4 sec.
		} catch ( Exception e ) {
			System.err.println( "Shutting down the Linked Data cache failed: " + e.getMessage() );
		}
	}

}
