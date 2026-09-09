@echo off
echo ========================================================
echo  Face Details Tracker - APK Build Assistant
echo ========================================================
echo.

where javac >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [!] Java Development Kit (JDK 17) was not detected in PATH.
    echo.
    echo To build the APK file, please either:
    echo   1. Open this folder in Android Studio and click 'Build > Build APK'.
    echo   2. Or push this repository to GitHub (we have included a ready-to-run
    echo      GitHub Actions workflow in .github/workflows/build-apk.yml that
    echo      compiles the APK in the cloud and gives you the direct download link).
    echo.
    pause
    exit /b 1
)

echo [*] Building APK with Gradle...
call gradlew assembleDebug

if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo.
    echo ========================================================
    echo  SUCCESS! APK generated at:
    echo  %~dp0app\build\outputs\apk\debug\app-debug.apk
    echo ========================================================
) else (
    echo [!] Build did not complete. Please check the logs above.
)
pause
