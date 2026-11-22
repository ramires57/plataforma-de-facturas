@echo off
setlocal
if not exist out mkdir out
dir /b /s src\main\java\*.java > sources.txt
javac --add-modules jdk.httpserver -encoding UTF-8 -d out @sources.txt
del sources.txt
if errorlevel 1 exit /b 1
java --add-modules jdk.httpserver -cp out pe.senati.facturacion.ServidorFactura
