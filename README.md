# Facturación mayorista SENATI

Aplicación web local para calcular precios por volumen, descuentos y el IGV incluido en los precios. La lógica y el servidor están escritos en Java; la interfaz usa HTML, CSS y JavaScript servido desde el mismo servidor. Incluye un catálogo de 20 componentes de PC, impresión de factura y descarga directa en PDF.

Los componentes tienen precios referenciales propios en soles. La escala por cantidad aplica un ajuste sobre el precio base y conserva descuentos del 5% hasta 500 unidades y del 15% por encima de esa cantidad.

## Requisitos

- JDK 21 o superior en el `PATH`.
- No requiere Maven ni dependencias externas.

## Ejecutar en Windows

Desde la carpeta del proyecto, abre una terminal y ejecuta:

```bat
run.bat
```

Abre <http://127.0.0.1:8000>. Para detener el servidor, presiona `Ctrl+C`.

## Publicar en Render

El proyecto está preparado para desplegarse como servicio Docker:

1. Sube el proyecto a un repositorio de GitHub, incluyendo `Dockerfile` y `.dockerignore`.
2. En Render, crea un **New Web Service** y conecta ese repositorio.
3. Selecciona **Docker** como entorno y deja la ruta del Dockerfile como `./Dockerfile`.
4. Crea el servicio. Render asignará `PORT`; el servidor Java lo lee automáticamente y escucha en `0.0.0.0`.

No agregues un comando de inicio de Maven ni una base de datos. El Dockerfile compila y ejecuta la aplicación directamente con Java 21.

## Probar los cálculos

En una terminal, compila la lógica y las pruebas:

```bat
if not exist out mkdir out
javac --add-modules jdk.httpserver -encoding UTF-8 -d out src\main\java\pe\senati\facturacion\Factura.java src\main\java\pe\senati\facturacion\CalculadoraFactura.java src\main\java\pe\senati\facturacion\PdfFactura.java src\test\java\pe\senati\facturacion\PruebasFactura.java
java --add-modules jdk.httpserver -cp out pe.senati.facturacion.PruebasFactura
```

La API recibe `POST /api/factura` con `Content-Type: application/json`, con los campos `nombre_cliente`, `producto` y `cantidad`. Para descargar el documento, envía el mismo JSON a `POST /api/factura/pdf`; la respuesta es un PDF.
