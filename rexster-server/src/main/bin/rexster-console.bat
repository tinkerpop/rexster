:: Windows launcher script for Rexster Console
@echo off

cd ..\lib
set LIBDIR=%CD%

cd ..\..\..\

set JAVA_OPTIONS=-Xms32M -Xmx512M

:: Launch the application
java %JAVA_OPTIONS% %JAVA_ARGS% -cp %LIBDIR%/*;%EXTDIR%/*  com.tinkerpop.rexster.protocol.RexsterConsole %*