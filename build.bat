@echo off
echo Building Personal Assistant APK...
cd /d "C:\path\to\your\android\project"
call gradlew assembleRelease
if %errorlevel% == 0 (
    echo Build successful! APK location: app\build\outputs\apk\release\
) else (
    echo Build failed!
)
pause