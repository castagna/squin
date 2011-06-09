#!/bin/bash

SQUIN_ROOT=${0%/*}/..
ARQ_LIBS=/home/olaf/AdditionalSoftware/ARQ/lib
JAVA_RDFa_JAR=/home/olaf/java-rdfa-0.4.1.jar
HTMLPARSER_JAR=/home/olaf/AdditionalSoftware/htmlparser-1.3.1/htmlparser-1.3.1.jar

SEP=':'
if [ "$(uname)" = "Cygwin" ]; then SEP=';'; fi

for jar in "$ARQ_LIBS"/*.jar
do
  if [ ! -e "$jar" ]; then continue; fi
  CP="$CP$SEP$jar"
done

for jar in "$SQUIN_ROOT"/dist/lib/squin*.jar
do
  if [ ! -e "$jar" ]; then continue; fi
  CP="$CP$SEP$jar"
done

if [ -e "$JAVA_RDFa_JAR" ] ; then CP="$CP$SEP$JAVA_RDFa_JAR"; fi
if [ -e "$HTMLPARSER_JAR" ] ; then CP="$CP$SEP$HTMLPARSER_JAR"; fi

exec java -cp "$CP" org.squin.command.query "$@"
