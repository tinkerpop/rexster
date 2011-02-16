:: Windows launcher script for Rexster
@echo off

cd ..\lib
set LIBDIR=%CD%

set CP=

for %%i in (*.jar) do call :concatsep %%i
cd ..\..\..\

set JAVA_OPTIONS=-Xms32M -Xmx512M

:: Launch the application
java %JAVA_OPTIONS% %JAVA_ARGS% -cp %CP% com.tinkerpop.rexster.WebServer %*
goto :eof

:concatsep
if "%CP%" == "" (
set CP=%LIBDIR%\%1
)else (
set CP=%CP%;%LIBDIR%\%1
)

:skip
