@echo off

rem [JDK path]
set JAVA=D:\DevTools\jdk1.7.0_45\bin
REM set JAVA=d:\DevTools\jdk1.8.0_51\bin

rem [path to PLAY folder]
set PLAY_HOME=d:\DevTools\play-2.1.3\

set PATH=%JAVA%;%PLAY_HOME%;%PATH%
set GOOGLE_APPLICATION_CREDENTIALS=d:\Projects\PhimCity\gaeserviceaccount.json