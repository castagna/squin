#!/bin/bash

SQUIN_ROOT=${0%/*}/..

SEP=':'
if [ "$(uname)" = "Cygwin" ]; then SEP=';'; fi

exec java -cp "$SQUIN_ROOT/target/*$SEP$SQUIN_ROOT/target/lib/*" org.squin.command.query "$@"
