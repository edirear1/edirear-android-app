@echo off
echo ========================================================
echo          Edirear Android APK Local Builder
echo ========================================================
echo.

where gradlew >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] Gradle wrapper found. Building Debug APK...
    call gradlew.bat assembleDebug
    goto check_output
)

where gradle >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] System Gradle found. Building Debug APK...
    call gradle assembleDebug
    goto check_output
)

echo [!] Gradle not found in system PATH.
echo [i] Tip: Open this folder in Android Studio and click:
echo     Build ^> Build Bundle(s) / APK(s) ^> Build APK(s)
pause
exit /b 1

:check_output
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo.
    echo ========================================================
    echo SUCCESS! APK Generated at:
    echo app\build\outputs\apk\debug\app-debug.apk
    echo ========================================================
) else (
    echo.
    echo Build finished, please check logs above.
)
pause
