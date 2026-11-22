package pe.senati.facturacion;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PdfFactura {
    private PdfFactura() {
    }

    public static byte[] generar(Factura factura) {
        String contenido = crearContenido(factura);
        byte[] stream = contenido.getBytes(StandardCharsets.US_ASCII);
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        int[] offsets = new int[7];

        escribir(pdf, "%PDF-1.4\n");
        objeto(pdf, offsets, 1, "<< /Type /Catalog /Pages 2 0 R >>");
        objeto(pdf, offsets, 2, "<< /Type /Pages /Kids [3 0 R] /Count 1 >>");
        objeto(pdf, offsets, 3,
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                        + "/Resources << /Font << /F1 4 0 R /F2 5 0 R >> >> "
                        + "/Contents 6 0 R >>");
        objeto(pdf, offsets, 4,
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica "
                        + "/Encoding /WinAnsiEncoding >>");
        objeto(pdf, offsets, 5,
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold "
                        + "/Encoding /WinAnsiEncoding >>");
        offsets[6] = pdf.size();
        escribir(pdf, "6 0 obj\n<< /Length " + stream.length + " >>\nstream\n");
        pdf.writeBytes(stream);
        escribir(pdf, "\nendstream\nendobj\n");

        int inicioXref = pdf.size();
        escribir(pdf, "xref\n0 7\n0000000000 65535 f \n");
        for (int i = 1; i < offsets.length; i++) {
            escribir(pdf, String.format(Locale.ROOT, "%010d 00000 n \n", offsets[i]));
        }
        escribir(pdf, "trailer\n<< /Size 7 /Root 1 0 R >>\nstartxref\n"
                + inicioXref + "\n%%EOF\n");
        return pdf.toByteArray();
    }

    private static String crearContenido(Factura factura) {
        StringBuilder contenido = new StringBuilder();
        texto(contenido, 54, 790, 23, true, "SENATI");
        texto(contenido, 54, 768, 10, false, "FACTURA MAYORISTA");
        texto(contenido, 430, 788, 8, false, "DOCUMENTO / PEN");
        texto(contenido, 430, 770, 10, true, LocalDate.now().toString());
        contenido.append("0.00 0.55 0.80 RG 1.2 w 54 746 m 541 746 l S\n");

        texto(contenido, 54, 718, 8, false, "CLIENTE");
        List<String> nombre = envolver(factura.nombreCliente(), 62);
        int y = 697;
        for (String linea : nombre) {
            texto(contenido, 54, y, 13, true, linea);
            y -= 17;
        }

        texto(contenido, 54, 646, 8, false, "COMPONENTE");
        List<String> producto = envolver(factura.producto(), 62);
        y = 627;
        for (String linea : producto) {
            texto(contenido, 54, y, 11, true, linea);
            y -= 15;
        }
        texto(contenido, 54, 590, 8, false, "UNIDADES ADQUIRIDAS");
        texto(contenido, 54, 570, 12, true, Integer.toString(factura.cantidad()));
        texto(contenido, 340, 590, 8, false, "PRECIO UNITARIO");
        texto(contenido, 340, 570, 12, true, soles(factura.precioUnitario()));
        contenido.append("0.72 0.77 0.81 RG 0.6 w 54 548 m 541 548 l S\n");

        fila(contenido, 54, 523, "Importe de compra", soles(factura.importeCompra()));
        fila(contenido, 54, 496, "Descuento aplicado", "- " + soles(factura.descuento()));
        fila(contenido, 54, 469, "Subtotal", soles(factura.subtotal()));
        fila(contenido, 54, 442, "IGV (18% incluido)", soles(factura.igv()));
        contenido.append("0.72 0.77 0.81 RG 0.6 w 54 420 m 541 420 l S\n");
        texto(contenido, 54, 392, 9, true, "TOTAL A PAGAR");
        texto(contenido, 385, 388, 18, true, soles(factura.totalPagar()));
        contenido.append("0.00 0.55 0.80 RG 1.2 w 54 368 m 541 368 l S\n");

        texto(contenido, 54, 337, 8, false,
                "Precios con IGV incluido. El impuesto se encuentra desglosado del total.");
        texto(contenido, 54, 48, 7, false, "SENATI  /  RESUMEN DE OPERACION");
        texto(contenido, 440, 48, 7, false, "GENERADO LOCALMENTE");
        return contenido.toString();
    }

    private static void fila(StringBuilder contenido, int x, int y, String etiqueta, String valor) {
        texto(contenido, x, y, 10, false, etiqueta);
        texto(contenido, 405, y, 10, true, valor);
    }

    private static void texto(
            StringBuilder contenido, int x, int y, int tamano, boolean negrita, String valor) {
        contenido.append("0.10 0.15 0.20 rg\nBT /")
                .append(negrita ? "F2" : "F1")
                .append(' ').append(tamano).append(" Tf ")
                .append(x).append(' ').append(y).append(" Td ")
                .append(literal(valor)).append(" Tj ET\n");
    }

    private static String literal(String valor) {
        StringBuilder literal = new StringBuilder("(");
        byte[] bytes = valor.getBytes(java.nio.charset.Charset.forName("windows-1252"));
        for (byte byteValor : bytes) {
            int caracter = Byte.toUnsignedInt(byteValor);
            if (caracter == '(' || caracter == ')' || caracter == '\\') {
                literal.append('\\').append((char) caracter);
            } else if (caracter < 32 || caracter > 126) {
                literal.append('\\').append(String.format(Locale.ROOT, "%03o", caracter));
            } else {
                literal.append((char) caracter);
            }
        }
        return literal.append(')').toString();
    }

    private static List<String> envolver(String texto, int maximo) {
        List<String> lineas = new ArrayList<>();
        String restante = texto;
        while (restante.length() > maximo) {
            int corte = restante.lastIndexOf(' ', maximo);
            if (corte <= 0) {
                corte = maximo;
            }
            lineas.add(restante.substring(0, corte));
            restante = restante.substring(corte).stripLeading();
        }
        lineas.add(restante);
        return lineas;
    }

    private static String soles(java.math.BigDecimal monto) {
        return "S/. " + monto.toPlainString();
    }

    private static void objeto(
            ByteArrayOutputStream pdf, int[] offsets, int numero, String contenido) {
        offsets[numero] = pdf.size();
        escribir(pdf, numero + " 0 obj\n" + contenido + "\nendobj\n");
    }

    private static void escribir(ByteArrayOutputStream destino, String texto) {
        destino.writeBytes(texto.getBytes(StandardCharsets.US_ASCII));
    }
}
