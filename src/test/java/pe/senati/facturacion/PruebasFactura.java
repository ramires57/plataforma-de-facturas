package pe.senati.facturacion;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public final class PruebasFactura {
    private PruebasFactura() {
    }

    public static void main(String[] args) {
        exigir(CalculadoraFactura.PRODUCTOS.size() == 20,
                "El catálogo debe contener 20 componentes.");
        Set<BigDecimal> precios = new HashSet<>();
        for (String producto : CalculadoraFactura.PRODUCTOS) {
            exigir(precios.add(CalculadoraFactura.precioReferencial(producto)),
                    "Hay dos componentes con el mismo precio referencial.");
        }
        Map<String, Integer> varios = new LinkedHashMap<>();
        varios.put("Procesador (CPU)", 300);
        varios.put("Memoria RAM", 200);
        FacturaMultiple multiproducto =
                CalculadoraFactura.calcularMultiple("Compras SENATI", varios);
        exigir(multiproducto.lineas().size() == 2,
                "La factura debe admitir varias líneas.");
        exigir(multiproducto.cantidadTotal() == 500,
                "La cantidad total debe incluir todos los componentes.");
        exigir(new BigDecimal("8131.20").equals(multiproducto.descuento()),
                "La factura multiproducto debe aplicar el 5% con 500 unidades totales.");
        Factura procesador = CalculadoraFactura.calcular(
                "Cliente", "Procesador (CPU)", 100);
        Factura grafica = CalculadoraFactura.calcular(
                "Cliente", "Tarjeta gráfica (GPU)", 100);
        exigir(!procesador.precioUnitario().equals(grafica.precioUnitario()),
                "Los componentes deben tener precios unitarios distintos.");
        exigir(new BigDecimal("650.00").equals(procesador.precioReferencial()),
                "El procesador debe usar su precio referencial.");
        exigir(new BigDecimal("1800.00").equals(grafica.precioReferencial()),
                "La tarjeta gráfica debe usar su precio referencial.");
        exigir(new BigDecimal("650.00").equals(procesador.precioUnitario()),
                "Una compra de hasta 100 unidades debe usar el precio base.");
        exigir(new BigDecimal("598.00").equals(CalculadoraFactura.calcular(
                "Cliente", "Procesador (CPU)", 101).precioUnitario()),
                "Una compra de 101 a 250 unidades debe aplicar el factor comercial.");

        Factura factura = CalculadoraFactura.calcular(" Ana ", "Procesador (CPU)", 600);
        exigir("Ana".equals(factura.nombreCliente()), "Se debe normalizar el nombre.");
        exigir(new BigDecimal("319.15").equals(factura.precioUnitario()),
                "Se debe aplicar la escala comercial de volumen al producto.");
        exigir(new BigDecimal("162766.50").equals(factura.totalPagar()),
                "El total debe combinar el precio propio del producto y el descuento.");
        exigir(new BigDecimal("137937.71").equals(factura.subtotal()),
                "El subtotal debe extraer correctamente el IGV incluido.");
        exigir(new BigDecimal("24828.79").equals(factura.igv()),
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
        exigir(pdf.contains("S/. 450738.00") && pdf.endsWith("%%EOF\n"),
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

    private static void exigir(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }
}
