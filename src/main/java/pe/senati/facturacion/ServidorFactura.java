package pe.senati.facturacion;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;

public final class ServidorFactura {
    private static final int PUERTO_LOCAL = 8000;
    private static final int LIMITE_SOLICITUD = 16_384;
    private static final Path WEB = Path.of("web");

    private ServidorFactura() {
    }

    public static void main(String[] args) throws IOException {
        String puertoRender = System.getenv("PORT");
        int puerto = obtenerPuerto(puertoRender);
        String host = puertoRender == null || puertoRender.isBlank()
                ? "127.0.0.1"
                : "0.0.0.0";
        HttpServer servidor = HttpServer.create(new InetSocketAddress(host, puerto), 0);
        servidor.createContext("/", ServidorFactura::atender);
        servidor.setExecutor(Executors.newFixedThreadPool(8));
        servidor.start();
        System.out.printf("Facturación SENATI disponible en http://%s:%d%n", host, puerto);
    }

    private static int obtenerPuerto(String configuracion) {
        if (configuracion == null || configuracion.isBlank()) {
            return PUERTO_LOCAL;
        }
        try {
            int puerto = Integer.parseInt(configuracion);
            if (puerto < 1 || puerto > 65_535) {
                throw new IllegalArgumentException("La variable PORT debe estar entre 1 y 65535.");
            }
            return puerto;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("La variable PORT debe ser un puerto válido.");
        }
    }

    private static void atender(HttpExchange intercambio) throws IOException {
        intercambio.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        intercambio.getResponseHeaders().set("Referrer-Policy", "strict-origin-when-cross-origin");
        intercambio.getResponseHeaders().set(
                "Content-Security-Policy",
                "default-src 'self'; style-src 'self'; script-src 'self'; "
                        + "img-src 'self' data:; connect-src 'self'; "
                        + "base-uri 'self'; form-action 'self'; frame-ancestors 'none'");

        String ruta = intercambio.getRequestURI().getPath();
        boolean esPdf = "/api/factura/pdf".equals(ruta);
        if ("/api/factura".equals(ruta) || esPdf) {
            if (!"POST".equalsIgnoreCase(intercambio.getRequestMethod())) {
                intercambio.getResponseHeaders().set("Allow", "POST");
                responder(intercambio, 405, "application/json; charset=utf-8",
                        "{\"error\":\"Método no permitido.\"}");
                return;
            }
            atenderFactura(intercambio, esPdf);
            return;
        }

        if (!"GET".equalsIgnoreCase(intercambio.getRequestMethod())) {
            intercambio.getResponseHeaders().set("Allow", "GET, POST");
            responder(intercambio, 405, "text/plain; charset=utf-8", "Método no permitido.");
            return;
        }

        servirArchivoWeb(intercambio, ruta);
    }

    private static void atenderFactura(HttpExchange intercambio, boolean comoPdf) throws IOException {
        byte[] cuerpo = intercambio.getRequestBody().readNBytes(LIMITE_SOLICITUD + 1);
        if (cuerpo.length > LIMITE_SOLICITUD) {
            responder(intercambio, 413, "application/json; charset=utf-8",
                    "{\"error\":\"La solicitud supera el tamaño permitido.\"}");
            return;
        }
        String tipoContenido = intercambio.getRequestHeaders().getFirst("Content-Type");
        if (tipoContenido == null
                || !tipoContenido.toLowerCase().startsWith("application/json")) {
            responder(intercambio, 415, "application/json; charset=utf-8",
                    "{\"error\":\"El servicio requiere una solicitud JSON.\"}");
            return;
        }

        try {
            Solicitud solicitud = leerSolicitud(new String(cuerpo, StandardCharsets.UTF_8));
            Factura factura = CalculadoraFactura.calcular(
                    solicitud.nombreCliente(), solicitud.producto(), solicitud.cantidad());
            if (comoPdf) {
                intercambio.getResponseHeaders().set(
                        "Content-Disposition", "attachment; filename=\"factura-senati.pdf\"");
                responder(intercambio, 200, "application/pdf", PdfFactura.generar(factura));
            } else {
                responder(intercambio, 200, "application/json; charset=utf-8", comoJson(factura));
            }
        } catch (IllegalArgumentException error) {
            responder(intercambio, 400, "application/json; charset=utf-8",
                    "{\"error\":\"" + escaparJson(error.getMessage()) + "\"}");
        }
    }

    private static Solicitud leerSolicitud(String json) {
        CursorJson cursor = new CursorJson(json);
        cursor.esperar('{');
        String nombreCliente = null;
        String producto = null;
        Integer cantidad = null;
        boolean nombreEncontrado = false;
        boolean productoEncontrado = false;
        boolean cantidadEncontrada = false;

        if (cursor.ver() != '}') {
            while (true) {
                String clave = cursor.leerCadena();
                cursor.esperar(':');
                switch (clave) {
                    case "nombre_cliente" -> {
                        if (nombreEncontrado) {
                            throw new IllegalArgumentException("El nombre está duplicado.");
                        }
                        nombreCliente = cursor.leerCadena();
                        nombreEncontrado = true;
                    }
                    case "producto" -> {
                        if (productoEncontrado) {
                            throw new IllegalArgumentException("El producto está duplicado.");
                        }
                        producto = cursor.leerCadena();
                        productoEncontrado = true;
                    }
                    case "cantidad" -> {
                        if (cantidadEncontrada) {
                            throw new IllegalArgumentException("La cantidad está duplicada.");
                        }
                        cantidad = cursor.leerEntero();
                        cantidadEncontrada = true;
                    }
                    default -> throw new IllegalArgumentException(
                            "Campo no admitido: " + clave + ".");
                }

                if (cursor.ver() == '}') {
                    break;
                }
                cursor.esperar(',');
            }
        }
        cursor.esperar('}');
        cursor.verificarFin();

        if (!nombreEncontrado || !productoEncontrado || !cantidadEncontrada) {
            throw new IllegalArgumentException(
                    "Se requieren nombre_cliente, producto y cantidad.");
        }
        return new Solicitud(nombreCliente, producto, cantidad);
    }

    private static void servirArchivoWeb(HttpExchange intercambio, String ruta) throws IOException {
        String archivo = switch (ruta) {
            case "/", "/index.html" -> "index.html";
            case "/styles.css" -> "styles.css";
            case "/app.js" -> "app.js";
            default -> null;
        };
        if (archivo == null) {
            responder(intercambio, 404, "text/plain; charset=utf-8", "No encontrado.");
            return;
        }

        Path rutaArchivo = WEB.resolve(archivo);
        if (!Files.isRegularFile(rutaArchivo)) {
            responder(intercambio, 500, "text/plain; charset=utf-8",
                    "No se encontró un recurso necesario de la interfaz.");
            return;
        }

        String tipo = archivo.endsWith(".css")
                ? "text/css; charset=utf-8"
                : archivo.endsWith(".js")
                        ? "text/javascript; charset=utf-8"
                        : "text/html; charset=utf-8";
        byte[] contenido = Files.readAllBytes(rutaArchivo);
        intercambio.getResponseHeaders().set("Cache-Control", "no-cache");
        intercambio.getResponseHeaders().set("Content-Type", tipo);
        intercambio.sendResponseHeaders(200, contenido.length);
        try (var salida = intercambio.getResponseBody()) {
            salida.write(contenido);
        }
    }

    private static String comoJson(Factura factura) {
        return "{"
                + "\"nombre_cliente\":\"" + escaparJson(factura.nombreCliente()) + "\","
                + "\"producto\":\"" + escaparJson(factura.producto()) + "\","
                + "\"precio_referencial\":\"" + factura.precioReferencial().toPlainString() + "\","
                + "\"cantidad\":" + factura.cantidad() + ","
                + "\"precio_unitario\":\"" + factura.precioUnitario().toPlainString() + "\","
                + "\"importe_compra\":\"" + factura.importeCompra().toPlainString() + "\","
                + "\"descuento\":\"" + factura.descuento().toPlainString() + "\","
                + "\"subtotal\":\"" + factura.subtotal().toPlainString() + "\","
                + "\"igv\":\"" + factura.igv().toPlainString() + "\","
                + "\"total_pagar\":\"" + factura.totalPagar().toPlainString() + "\""
                + "}";
    }

    private static String escaparJson(String valor) {
        StringBuilder resultado = new StringBuilder();
        for (char caracter : valor.toCharArray()) {
            switch (caracter) {
                case '"' -> resultado.append("\\\"");
                case '\\' -> resultado.append("\\\\");
                case '\b' -> resultado.append("\\b");
                case '\f' -> resultado.append("\\f");
                case '\n' -> resultado.append("\\n");
                case '\r' -> resultado.append("\\r");
                case '\t' -> resultado.append("\\t");
                default -> {
                    if (caracter < 0x20) {
                        resultado.append(String.format("\\u%04x", (int) caracter));
                    } else {
                        resultado.append(caracter);
                    }
                }
            }
        }
        return resultado.toString();
    }

    private static void responder(
            HttpExchange intercambio, int estado, String tipo, String mensaje) throws IOException {
        responder(intercambio, estado, tipo, mensaje.getBytes(StandardCharsets.UTF_8));
    }

    private static void responder(
            HttpExchange intercambio, int estado, String tipo, byte[] contenido) throws IOException {
        intercambio.getResponseHeaders().set("Content-Type", tipo);
        intercambio.sendResponseHeaders(estado, contenido.length);
        try (var salida = intercambio.getResponseBody()) {
            salida.write(contenido);
        }
    }

    private record Solicitud(String nombreCliente, String producto, int cantidad) {
    }

    private static final class CursorJson {
        private final String texto;
        private int posicion;

        private CursorJson(String texto) {
            this.texto = texto;
        }

        private String leerCadena() {
            esperar('"');
            StringBuilder resultado = new StringBuilder();
            while (posicion < texto.length()) {
                char caracter = texto.charAt(posicion++);
                if (caracter == '"') {
                    return resultado.toString();
                }
                if (caracter < 0x20) {
                    throw new IllegalArgumentException("La solicitud JSON no es válida.");
                }
                if (caracter != '\\') {
                    resultado.append(caracter);
                    continue;
                }

                if (posicion >= texto.length()) {
                    throw new IllegalArgumentException("La solicitud JSON no es válida.");
                }
                char escape = texto.charAt(posicion++);
                switch (escape) {
                    case '"', '\\', '/' -> resultado.append(escape);
                    case 'b' -> resultado.append('\b');
                    case 'f' -> resultado.append('\f');
                    case 'n' -> resultado.append('\n');
                    case 'r' -> resultado.append('\r');
                    case 't' -> resultado.append('\t');
                    case 'u' -> resultado.append(leerUnicode());
                    default -> throw new IllegalArgumentException(
                            "La solicitud JSON contiene un escape inválido.");
                }
            }
            throw new IllegalArgumentException("La solicitud JSON no es válida.");
        }

        private char leerUnicode() {
            if (posicion + 4 > texto.length()) {
                throw new IllegalArgumentException("La solicitud JSON no es válida.");
            }
            String codigo = texto.substring(posicion, posicion + 4);
            try {
                char resultado = (char) Integer.parseInt(codigo, 16);
                posicion += 4;
                return resultado;
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException("La solicitud JSON no es válida.");
            }
        }

        private int leerEntero() {
            espacios();
            int inicio = posicion;
            if (ver() == '-') {
                posicion++;
            }
            if (ver() == '0') {
                posicion++;
                if (esDigito(ver())) {
                    throw new IllegalArgumentException("La cantidad debe ser un entero positivo.");
                }
            } else {
                if (ver() < '1' || ver() > '9') {
                    throw new IllegalArgumentException("La cantidad debe ser un entero positivo.");
                }
                while (esDigito(ver())) {
                    posicion++;
                }
            }
            try {
                return Integer.parseInt(texto.substring(inicio, posicion));
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException("La cantidad debe ser un entero positivo.");
            }
        }

        private char ver() {
            espacios();
            return posicion < texto.length() ? texto.charAt(posicion) : '\0';
        }

        private void esperar(char esperado) {
            espacios();
            if (posicion >= texto.length() || texto.charAt(posicion) != esperado) {
                throw new IllegalArgumentException("La solicitud JSON no es válida.");
            }
            posicion++;
        }

        private void verificarFin() {
            espacios();
            if (posicion != texto.length()) {
                throw new IllegalArgumentException("La solicitud JSON no es válida.");
            }
        }

        private void espacios() {
            while (posicion < texto.length() && Character.isWhitespace(texto.charAt(posicion))) {
                posicion++;
            }
        }

        private static boolean esDigito(char caracter) {
            return caracter >= '0' && caracter <= '9';
        }
    }
}
