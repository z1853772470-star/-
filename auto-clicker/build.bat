@echo off
echo ========================================
echo  连点器 - 安卓构建脚本
echo ========================================
echo.
echo 请确保已安装:
echo   - JDK 17 (JAVA_HOME 已配置)
echo   - Android Studio (含 Android SDK)
echo.
if "%JAVA_HOME%"=="" (
    echo [错误] JAVA_HOME 未设置，请安装 JDK 17 并配置环境变量
    pause
    exit /b 1
)
echo [JDK] %JAVA_HOME%
echo.
echo [1/2] 下载 Gradle 依赖...
call gradlew.bat --version
if %errorlevel% neq 0 (
    echo [错误] Gradle 初始化失败
    pause
    exit /b 1
)
echo.
echo [2/2] 编译 APK...
call gradlew.bat assembleDebug
if %errorlevel% neq 0 (
    echo [错误] 编译失败
    pause
    exit /b 1
)
echo.
echo ========================================
echo  编译成功！
echo  APK 位置: app\build\outputs\apk\debug\
echo ========================================
pause
