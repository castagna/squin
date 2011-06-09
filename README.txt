This package contains the source code of the SQUIN, a query interface for the
Web of Linked Data that implements the link traversal based query execution
paradigm. For more information about SQUIN and about link traversal based
query execution visit:

                       http://squin.org

SQUIN is distributed under the Apache License, version 2.0. See the files

         COPYRIGHT.txt   and   LICENSE-APACHE-2.0.txt

in this package.


USING SQUIN
===========
SQUIN may be used in three ways:

1. Command Line Interface
-------------------------
The command line interface of SQUIN allows you to execute single SPARQL queries
using the link traversal based query execution paradigm as implemented in the
query engine that comes with SQUIN. To use the SQUIN command line interface you
require a recent version of ARQ [1]. Adjust the shell script

                       ./bin/squin.sh

to set the shell variable ARQ_LIBS to the 'lib' directory that you find when
you unzip the ARQ package.


If you want RDFa support you also need the java-rdfa parser [2]. Download the
latest JAR file and adjust the shell script

                       ./bin/squin.sh

so that the shell variable JAVA_RDFa_JAR refers to that JAR file. Furthermore,
you may download the latest version of the Validator.nu HTML Parser [3] and
refer to the corresponding JAR file using the HTMLPARSER_JAR shell variable in
the aforementioned script. Using the HTML parser is optional, it enables the
RDFa parser to deal with messy HTML documents.


Once you have everything in place, you can use the SQUIN command line interface
by calling

 ./bin/squin.sh [options] --query=<queryfile> | <query>

To learn about the possible options of that command, call

 ./bin/squin.sh --help



2. Java Servlet
---------------
You can use SQUIN as a Web service. Learn here

 http://sourceforge.net/apps/mediawiki/squin/index.php?title=Set_up_a_SQUIN_service

how to setup such a service and here

 http://sourceforge.net/apps/mediawiki/squin/index.php?title=Configure_a_SQUIN_service

how to configure it.

There is also a Standalone package of SQUIN that can be set up very easily.



3. Library
----------
To make use of the link traversal based query execution system implemented in
SQUIN within your own Java applications you, first, have to register the query
engine in your code:

   LinkTraversalBasedQueryEngine.register();

Then you have to create a Linked Data cache:

   QueriedDataset qds = new QueriedDatasetImpl();
   JenaIOBasedQueriedDataset qdsWrapper = new JenaIOBasedQueriedDataset( qds );
   JenaIOBasedLinkedDataCache ldcache = new JenaIOBasedLinkedDataCache( qdsWrapper );

You wrap that Linked Data cache as an implementation of the Dataset interface
of the ARQ [2] query execution framework for RDF data:

   Dataset dsARQ = new LinkedDataCacheWrappingDataset( ldcache );

and use that to execute your queries as you usually do with ARQ:

   String queryString = "... your SPARQL query here ...";
   QueryExecution qe = QueryExecutionFactory.create( queryString, dsARQ );
   ResultSet results = qe.execSelect();
   while ( results.hasNext() )
   {
      QuerySolution s = results.nextSolution();
      // ... process the query result here ...
   }

Finally, you should not forget to shut down the Linked Data cache:

   try {
      ldcache.shutdownNow( 4000 ); // 4 sec.
   }
   catch ( Exception e ) {
      System.err.println( "Shutting down the Linked Data cache failed: " + e.getMessage() );
   }

The directory

                       apidoc/

provides a complete Javadoc documentation of the SQUIN API.

To compile and run your application you have to have all JAR files from a
recent version of ARQ [1] in your Java CLASSPATH, as well as the SQUIN JAR
file (which you find in the directory ./dist/lib/).
If you want RDFa support then the CLASSPATH to run your application must also
include the latest JAR file of the java-rdfa parser [2] and it may, optionally,
include the latest JAR file of the Validator.nu HTML Parser [3].


[1] http://openjena.org/ARQ/
[2] https://github.com/shellac/java-rdfa
[3] http://about.validator.nu/htmlparser/