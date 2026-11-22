@echo off
setlocal
if not exist out mkdir out
javac --add-modules jdk.httpserver -encoding UTF-8 -d out src\main\java\pe\senati\facturacion\Factura.java src\main\java\pe\senati\facturacion\CalculadoraFactura.java src\main\java\pe\senati\facturacion\PdfFactura.java src\main\java\pe\senati\facturacion\ServidorFactura.java
if errorlevel 1 exit /b 1
java --add-modules jdk.httpserver -cp out pe.senati.facturacion.ServidorFactura
