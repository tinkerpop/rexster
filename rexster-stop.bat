:: Windows launcher script for Rexster
@echo off

cd %CD%\target\

set TARGET=

for /f "tokens=*" %%a in ('dir /b /ad') do (
if exist "%%a\bin\rexster-stop.bat" set TARGET=%%a
)

cd %TARGET%\bin\
call rexster-stop.bat -host 127.0.0.1 -port 8184 -cmd s %*
