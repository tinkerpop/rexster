:: Windows launcher script for Rexster Console
@echo off

cd %CD%\target\

set TARGET=

for /f "tokens=*" %%a in ('dir /b /ad') do (
if exist "%%a\rexster-server\bin\rexster-console.bat" set TARGET=%%a
)

cd %TARGET%\bin\
call rexster-console.bat localhost 8184 gremlin %*

target/rexster-server/rexster-*-standalone/bin/rexster-console.bat $@