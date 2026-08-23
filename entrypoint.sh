#!/usr/bin/env sh

set -e

if [ "$1" = "java" ]; then
  shift

  DEFAULT_JVM_HEAP_SIZE_PERCENT=39
  JVM_HEAP_SIZE_PERCENT="${JVM_HEAP_SIZE_PERCENT:-$DEFAULT_JVM_HEAP_SIZE_PERCENT}"

  JVM_HEAP_ARGS="-XX:InitialRAMPercentage=${JVM_HEAP_SIZE_PERCENT} -XX:MaxRAMPercentage=${JVM_HEAP_SIZE_PERCENT}"

  # Always-on options
  JVM_OTHER_ARGS="-XX:+AlwaysPreTouch -XX:+UseCompactObjectHeaders -XshowSettings:system -XshowSettings:vm"

  JVM_ALL_ARGS="$JVM_HEAP_ARGS $JVM_OTHER_ARGS ${JAVA_OPTS:-}"

  echo "JVM Options:" $JVM_ALL_ARGS
  exec java $JVM_ALL_ARGS "$@" 2>&1
else
  exec "$@"
fi
