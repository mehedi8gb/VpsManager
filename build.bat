@echo off
setlocal

echo === VPS Manager Build ===
echo.

:: ─── Paths ────────────────────────────────────────────────────────────────────
set FLATLAF=lib\flatlaf-3.5.4.jar
set SRC=src\com\vpsmanager
set BIN=bin

:: ─── Compile ─────────────────────────────────────────────────────────────────
echo [1/3] Compiling...
if not exist %BIN% mkdir %BIN%
javac -encoding UTF-8 -cp %FLATLAF% -d %BIN% %SRC%\*.java
if %errorlevel% neq 0 (
    echo.
    echo BUILD FAILED during compilation.
    pause
    exit /b 1
)
echo       OK

:: ─── Build fat JAR (embed FlatLaf classes so no -cp needed at runtime) ────────
echo [2/3] Packaging fat JAR...
cd %BIN%
:: Extract FlatLaf into bin\ so jar picks up its classes
jar xf ..\%FLATLAF%
:: Package everything (app classes + FlatLaf classes) into VpsManager.jar
jar cfe ..\VpsManager.jar com.vpsmanager.App .
cd ..
if %errorlevel% neq 0 (
    echo.
    echo BUILD FAILED during packaging.
    pause
    exit /b 1
)
echo       OK

:: ─── Done ─────────────────────────────────────────────────────────────────────
echo [3/3] Build complete!
echo.
echo   Run with:   run.bat          (no console window)
echo   Or run:     javaw -jar VpsManager.jar
echo.
echo   NOTE: Double-clicking VpsManager.jar may show a brief console.
echo         Always use run.bat for a clean GUI launch.
echo.
pause
