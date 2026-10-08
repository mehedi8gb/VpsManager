@echo off
setlocal EnableExtensions DisableDelayedExpansion

set "ROOT=%~dp0"
if not defined VERSION set "VERSION=1.0.2"
set "SRC=%ROOT%src"
set "BIN=%ROOT%bin"
set "FLATLAF=%ROOT%lib\flatlaf-3.5.4.jar"

where javac >nul 2>nul
if errorlevel 1 (
    echo JDK 17 javac was not found on PATH.
    exit /b 1
)
where javaw >nul 2>nul
if errorlevel 1 (
    echo javaw was not found on PATH.
    exit /b 1
)
if not exist "%FLATLAF%" (
    echo Missing dependency: %FLATLAF%
    exit /b 1
)

if exist "%BIN%" rmdir /s /q "%BIN%"
if exist "%BIN%" (
    echo Could not remove old compiled classes from "%BIN%".
    exit /b 1
)
mkdir "%BIN%"
if errorlevel 1 (
    echo Could not create compiled classes directory "%BIN%".
    exit /b 1
)

pushd "%SRC%"
javac --release 17 -encoding UTF-8 -cp "%FLATLAF%" -d "%BIN%" com\vpsmanager\*.java
set "COMPILE_EXIT=%ERRORLEVEL%"
popd
if not "%COMPILE_EXIT%"=="0" (
    echo Compilation failed. Application not started.
    exit /b %COMPILE_EXIT%
)
> "%BIN%\version.properties" echo version=%VERSION%

pushd "%ROOT%"
if errorlevel 1 (
    echo Could not switch to application directory "%ROOT%".
    exit /b 1
)
start "" javaw -cp "%FLATLAF%;%BIN%" com.vpsmanager.App
popd
