@ECHO OFF
SET APP_HOME=%~dp0
SET CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar
IF EXIST "%CLASSPATH%" GOTO execute
ECHO Gradle wrapper JAR is missing. Open this project in Android Studio and use its Gradle wrapper support, or run with a local Gradle installation.
EXIT /B 1
:execute
java -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
