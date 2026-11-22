package pe.senati.facturacion;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

public final class PruebasFactura {
    private PruebasFactura() {
    }

    public static void main(String[] args) {
        exigir(CalculadoraFactura.PRODUCTOS.size() == 20,
                "El catálogo debe contener 20 componentes.");
        verificar(1, "27.70", "1.39");
        verificar(100, "27.70", "138.50");
        verificar(101, "25.50", "128.78");
        verificar(250, "25.50", "318.75");
        verificar(251, "19.50", "244.73");
        verificar(500, "19.50", "487.50");
        verificar(501, "13.60", "1022.04");

        Factura factura = CalculadoraFactura.calcular(" Ana ", "Procesador (CPU)", 600);
        exigir("Ana".equals(factura.nombreCliente()), "Se debe normalizar el nombre.");
        exigir(new BigDecimal("6936.00").equals(factura.totalPagar()),
                "El total para 600 unidades debe ser S/. 6,936.00.");
        exigir(new BigDecimal("5877.97").equals(factura.subtotal()),
                "El subtotal debe extraer correctamente el IGV incluido.");
        exigir(new BigDecimal("1058.03").equals(factura.igv()),
                "El IGV debe completar el total sin volver a sumarse.");

        String pdf = new String(
                PdfFactura.generar(CalculadoraFactura.calcular(
                        "José (Compras)", "Tarjeta gráfica (GPU)", 600)),
                StandardCharsets.US_ASCII);
        exigir(pdf.startsWith("%PDF-1.4"), "La factura debe ser un PDF.");
        exigir(pdf.contains("Jos\\351 \\(Compras\\)"),
                "El PDF debe conservar caracteres españoles y escapar paréntesis.");
        exigir(pdf.contains("Tarjeta gr\\341fica \\(GPU\\)"),
                "El PDF debe incluir el producto seleccionado.");
        exigir(pdf.contains("S/. 6936.00") && pdf.endsWith("%%EOF\n"),
                "El PDF debe incluir el total y terminar correctamente.");

        for (int cantidadInvalida : new int[]{0, -1}) {
            try {
                CalculadoraFactura.calcular("Ana", "Procesador (CPU)", cantidadInvalida);
                throw new AssertionError("Se debe rechazar una cantidad no positiva.");
            } catch (IllegalArgumentException esperado) {
                exigir(esperado.getMessage().contains("entero positivo"),
                        "La validación debe explicar el error.");
            }
        }

        try {
            CalculadoraFactura.calcular(" ", "Procesador (CPU)", 1);
            throw new AssertionError("Se debe rechazar un nombre vacío.");
        } catch (IllegalArgumentException esperado) {
            exigir(esperado.getMessage().contains("nombre"),
                    "La validación debe explicar el error.");
        }

        try {
            CalculadoraFactura.calcular("Ana", "Producto no listado", 1);
            throw new AssertionError("Se debe rechazar un componente fuera del catálogo.");
        } catch (IllegalArgumentException esperado) {
            exigir(esperado.getMessage().contains("componente"),
                    "La validación debe explicar el error.");
        }

        try {
            CalculadoraFactura.calcular("A".repeat(101), "Procesador (CPU)", 1);
            throw new AssertionError("Se debe rechazar un nombre de más de 100 caracteres.");
        } catch (IllegalArgumentException esperado) {
            exigir(esperado.getMessage().contains("100 caracteres"),
                    "La validación debe explicar el límite del nombre.");
        }

        System.out.println("Pruebas de factura aprobadas.");
    }

    private static void verificar(int cantidad, String precio, String descuento) {
        Factura factura = CalculadoraFactura.calcular("Cliente", "Procesador (CPU)", cantidad);
        exigir(new BigDecimal(precio).equals(factura.precioUnitario()),
                "Precio unitario incorrecto para " + cantidad + " unidades.");
        exigir(new BigDecimal(descuento).equals(factura.descuento()),
                "Descuento incorrecto para " + cantidad + " unidades.");
    }

    private static void exigir(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }
}
