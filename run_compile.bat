@echo off
cd /d C:\Users\Saurav Manandhar\eclipse-workspace\ai-selenium-test-generator
call mvn compile > compile_output.txt 2>&1
type compile_output.txt
