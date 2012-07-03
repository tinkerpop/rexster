:: Windows launcher script for Rexster Console
@echo off

set LIBDIR=lib

set JAVA_OPTIONS=-Xms32m -Xmx512m

:: Launch the application
java %JAVA_OPTIONS% %JAVA_ARGS% -cp %LIBDIR%/*;%EXTDIR%/*  com.tinkerpop.rexster.protocol.RexsterConsole %*