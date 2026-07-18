@rem Gradle wrapper startup script
@rem This is a placeholder. Run "gradle wrapper" to generate the proper wrapper.

if "%JAVA_HOME%"=="" (
    echo Error: JAVA_HOME is not set.
    exit /b 1
)

if exist "%JAVA_HOME%\bin\java.exe" (
    "%JAVA_HOME%\bin\java.exe" -jar "%~dp0gradle\wrapper\gradle-wrapper.jar" %*
) else (
    echo Error: Cannot find java.exe in JAVA_HOME.
    exit /b 1
)
