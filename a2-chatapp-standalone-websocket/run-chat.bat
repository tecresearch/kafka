@echo off
REM -------------------------------
REM WebSocket Chat Server - Compile & Run
REM -------------------------------

REM Set project directory
set PROJECT_DIR=%~dp0

REM Set classpath including all required JARs
set CLASSPATH=.;%PROJECT_DIR%libs\tyrus-server-1.18.jar;%PROJECT_DIR%libs\tyrus-core-1.18.jar;%PROJECT_DIR%libs\jakarta.websocket-api-1.1.2.jar

echo Compiling WebSocketChat.java ...
javac -cp "%CLASSPATH%" WebSocketChat.java
if %ERRORLEVEL% neq 0 (
    echo Compilation failed!
    pause
    exit /b
)

echo.
echo Running WebSocket Chat Server ...
echo Server will start at ws://localhost:8080/chat
echo Press Ctrl+C to stop the server.

java -cp "%CLASSPATH%" WebSocketChat