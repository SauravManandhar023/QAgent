@echo off
cd /d C:\Users\Saurav Manandhar\eclipse-workspace\ai-selenium-test-generator
set /p CP=<target\classpath.txt
java -cp "target/classes;%CP%" com.saurav.agentic.runners.Main
