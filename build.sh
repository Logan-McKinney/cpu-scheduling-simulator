#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
JAVA=java
JAVAC=javac
if [ -n "${JAVA_HOME:-}" ]; then
  JAVA="$JAVA_HOME/bin/java"
  JAVAC="$JAVA_HOME/bin/javac"
fi
task=${1:-test}
if [ "$#" -gt 0 ]; then shift; fi
case "$task" in
  baseline)
    mkdir -p build/baseline
    "$JAVAC" --release 17 -d build/baseline baseline/*.java tests/RegressionTests.java
    "$JAVA" -cp build/baseline RegressionTests ;;
  test|run|report)
    mkdir -p build/classes
    "$JAVAC" --release 17 -d build/classes src/*.java tests/*.java tools/*.java
    if [ "$task" = test ]; then
      "$JAVA" -cp build/classes RegressionTests
      "$JAVA" -cp build/classes EngineTests
    elif [ "$task" = report ]; then
      "$JAVA" -cp build/classes GenerateReport
    else
      "$JAVA" -cp build/classes scheduling_algorithms "$@"
    fi ;;
  *) echo 'Usage: sh build.sh [test|baseline|report|run [workload [quanta...]]]' >&2; exit 2 ;;
esac
