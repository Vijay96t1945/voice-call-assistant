#!/bin/sh
#
# Gradle wrapper start-up script for POSIX systems (sh, dash, ksh, zsh, bash)
#

# Attempt to find java in common locations
find_java() {
    if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        echo "$JAVA_HOME/bin/java"
    elif which java > /dev/null 2>&1; then
        echo "java"
    else
        echo ""
    fi
}

JAVA_EXE=$(find_java)
if [ -z "$JAVA_EXE" ]; then
    echo "ERROR: JAVA_HOME is not set and java is not on the PATH."
    exit 1
fi

APP_HOME=$(dirname "$0")
exec "$JAVA_EXE" \
    -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
    org.gradle.wrapper.GradleWrapperMain "$@"
