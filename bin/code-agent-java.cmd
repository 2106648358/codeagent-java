@echo off
setlocal

REM Force Java 25 to avoid stale JAVA_HOME from environment
set JAVA_HOME=C:\Users\EDY\.jdks\openjdk-25.0.1

if defined JAVA_HOME (
    set JAVA=%JAVA_HOME%\bin\java
) else (
    set JAVA=java
)

set SCRIPT_DIR=%~dp0
set JAR=%SCRIPT_DIR%..\code-agent-java-cli\target\code-agent-java-cli-0.0.1-SNAPSHOT.jar

%JAVA% -jar "%JAR%" %*
