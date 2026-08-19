@echo off
setlocal enabledelayedexpansion
if exist "%~dp0ship_editor.jar" (
    cd /d "%~dp0"
) else if exist "%~dp0..\..\ship_editor.jar" (
    cd /d "%~dp0..\.."
) else (
    cd /d "%~dp0"
)

set JVM_OPTS=-Xmx4g -XX:+UseG1GC -XX:+UseStringDeduplication -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=20 -Dsun.java2d.opengl=false -Dsun.java2d.d3d=false -Dsun.java2d.noddraw=true -Dsun.awt.noerasebackground=true -Dorg.lwjgl.opengl.contextAPI=native

set "JAVA_EXE=java"
for %%J in (
    "jre\bin\java.exe"
    "..\jre\bin\java.exe"
    "..\..\jre\bin\java.exe"
    "..\jre_linux\bin\java.exe"
    "..\..\jre_linux\bin\java.exe"
) do (
    if exist %%J (
        set "JAVA_EXE=%%~J"
        echo Found JRE: %%~J
        goto :found_jre
    )
)
echo Local JRE not found. Launching with system Java...
:found_jre

if "%~1"=="--cli" (
    !JAVA_EXE! %JVM_OPTS% -cp ship_editor.jar shipeditor.CliMain %2 %3 %4 %5 %6 %7 %8 %9
    exit /b !errorlevel!
)

!JAVA_EXE! %JVM_OPTS% -jar ship_editor.jar

if %errorlevel% neq 0 (
    echo Application exited with error code %errorlevel%
    pause
)
