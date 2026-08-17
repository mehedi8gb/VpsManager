@echo off
setlocal EnableExtensions DisableDelayedExpansion

SET VERSION=1.0.0
SET APP_NAME=VpsManager
SET DISPLAY_NAME=VPS Manager
SET VENDOR=VPS Manager
SET MAIN_CLASS=com.vpsmanager.App
SET ROOT=%~dp0
SET SRC=%ROOT%src
SET LIB=%ROOT%lib
SET BIN=%ROOT%bin
SET DIST=%ROOT%dist
SET STAGE=%ROOT%build-stage
SET ICON=%ROOT%assets\VpsManager.ico
SET FLATLAF=%LIB%\flatlaf-3.5.4.jar
SET APP_IMAGE_NAME=%APP_NAME%-%VERSION%
SET JAR_NAME=%APP_NAME%-%VERSION%.jar
SET SETUP_NAME=%APP_NAME%-%VERSION%-Setup.exe
SET MODULES=java.base,java.desktop,java.logging,java.naming,java.prefs,java.security.jgss,java.xml
SET WIX_URL=https://github.com/wixtoolset/wix3/releases/download/wix3112rtm/wix311-binaries.zip

echo === %DISPLAY_NAME% %VERSION% Release Build ===
echo.

echo [Checking prerequisites]
where javac >nul 2>nul
if errorlevel 1 (set "ERROR_MESSAGE=JDK 17 javac was not found on PATH." & goto :fail)
where jar >nul 2>nul
if errorlevel 1 (set "ERROR_MESSAGE=JDK 17 jar tool was not found on PATH." & goto :fail)
where jpackage >nul 2>nul
if errorlevel 1 (set "ERROR_MESSAGE=JDK 17 jpackage was not found on PATH." & goto :fail)
if not exist "%FLATLAF%" (set "ERROR_MESSAGE=Missing dependency: %FLATLAF%" & goto :fail)
if not exist "%ICON%" (set "ERROR_MESSAGE=Missing application icon: %ICON%" & goto :fail)
javac -version 2>&1 | findstr /b /c:"javac 17." >nul
if errorlevel 1 (set "ERROR_MESSAGE=JDK 17 is required." & goto :fail)
echo       PASS

echo [Cleaning old build artifacts]
if exist "%BIN%" rmdir /s /q "%BIN%"
if exist "%BIN%" (set "ERROR_MESSAGE=Could not remove bin." & goto :fail)
if exist "%DIST%" rmdir /s /q "%DIST%"
if exist "%DIST%" (set "ERROR_MESSAGE=Could not remove dist." & goto :fail)
if exist "%STAGE%" rmdir /s /q "%STAGE%"
if exist "%STAGE%" (set "ERROR_MESSAGE=Could not remove build-stage." & goto :fail)
mkdir "%BIN%" "%DIST%" "%STAGE%\input" "%STAGE%\flatlaf"
if errorlevel 1 (set "ERROR_MESSAGE=Could not create build directories." & goto :fail)
echo       PASS

echo [Preparing WiX installer tooling]
where candle >nul 2>nul
if errorlevel 1 (
    echo       WiX not found on PATH; downloading portable WiX 3.11 binaries...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%WIX_URL%' -OutFile '%STAGE%\wix.zip'; Expand-Archive -LiteralPath '%STAGE%\wix.zip' -DestinationPath '%STAGE%\wix' -Force"
    if errorlevel 1 (set "ERROR_MESSAGE=Could not download or extract WiX tooling." & goto :fail)
    set "PATH=%STAGE%\wix;%PATH%"
)
where candle >nul 2>nul
if errorlevel 1 (set "ERROR_MESSAGE=WiX candle.exe is unavailable after setup." & goto :fail)
where light >nul 2>nul
if errorlevel 1 (set "ERROR_MESSAGE=WiX light.exe is unavailable after setup." & goto :fail)
echo       PASS

echo [Compiling Java 17 sources]
pushd "%SRC%"
javac --release 17 -encoding UTF-8 -cp "%FLATLAF%" -d "%BIN%" com\vpsmanager\*.java
set "JAVAC_EXIT=%ERRORLEVEL%"
popd
if not "%JAVAC_EXIT%"=="0" (set "ERROR_MESSAGE=Compilation failed." & goto :fail)
echo       PASS

echo [Building self-contained fat JAR]
pushd "%STAGE%\flatlaf"
jar xf "%FLATLAF%"
if errorlevel 1 (popd & set "ERROR_MESSAGE=Could not unpack FlatLaf." & goto :fail)
popd
jar --create --file "%DIST%\%JAR_NAME%" --main-class "%MAIN_CLASS%" -C "%BIN%" . -C "%STAGE%\flatlaf" .
if errorlevel 1 (set "ERROR_MESSAGE=Fat JAR packaging failed." & goto :fail)
copy /y "%DIST%\%JAR_NAME%" "%STAGE%\input\%JAR_NAME%" >nul
if errorlevel 1 (set "ERROR_MESSAGE=Could not stage the JAR for jpackage." & goto :fail)
if exist "%ROOT%data" xcopy /e /i /y /q "%ROOT%data" "%STAGE%\input\data" >nul
if errorlevel 1 (set "ERROR_MESSAGE=Could not stage the data directory." & goto :fail)
echo       PASS

echo [Creating portable app image with bundled runtime]
jpackage --type app-image --dest "%DIST%" --name "%APP_IMAGE_NAME%" --app-version "%VERSION%" --vendor "%VENDOR%" --description "%DISPLAY_NAME%" --input "%STAGE%\input" --main-jar "%JAR_NAME%" --main-class "%MAIN_CLASS%" --icon "%ICON%" --add-modules "%MODULES%"
if errorlevel 1 (set "ERROR_MESSAGE=Portable app-image creation failed." & goto :fail)
echo       PASS

echo [Creating Windows installer]
jpackage --type exe --dest "%STAGE%" --name "%APP_NAME%" --app-version "%VERSION%" --vendor "%VENDOR%" --description "%DISPLAY_NAME%" --input "%STAGE%\input" --main-jar "%JAR_NAME%" --main-class "%MAIN_CLASS%" --icon "%ICON%" --add-modules "%MODULES%" --win-dir-chooser --win-menu --win-shortcut
if errorlevel 1 (set "ERROR_MESSAGE=Installer creation failed." & goto :fail)
move /y "%STAGE%\%APP_NAME%-%VERSION%.exe" "%DIST%\%SETUP_NAME%" >nul
if errorlevel 1 (set "ERROR_MESSAGE=Could not rename the installer." & goto :fail)
echo       PASS

echo [Finalizing release artifacts]
rmdir /s /q "%STAGE%"
if exist "%STAGE%" (set "ERROR_MESSAGE=Could not clean temporary staging files." & goto :fail)
echo       PASS

echo.
echo === RELEASE BUILD SUCCEEDED ===
echo Ready for GitHub Release:
dir /b "%DIST%"
exit /b 0

:fail
echo.
echo === RELEASE BUILD FAILED ===
echo %ERROR_MESSAGE%
exit /b 1
