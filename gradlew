#!/bin/sh

APP_HOME="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)"
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -f "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" ]; then
  exec java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
fi

echo "Gradle wrapper JAR is missing. Open this project in Android Studio and use its Gradle wrapper support, or run with a local Gradle installation." >&2
exit 1
