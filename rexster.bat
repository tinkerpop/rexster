:: Windows launcher script for Rexster
@echo off

cd %CD%\target\

set TARGET=

for /f "tokens=*" %%a in ('dir /b /ad') do (
if exist "%%a\rexster-server\bin\rexster.bat" set TARGET=%%a
)

cd %TARGET%\bin\
call rexster.bat %*

cd ..\..\..
