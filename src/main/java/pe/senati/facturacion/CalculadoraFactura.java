package pe.senati.facturacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

public final class CalculadoraFactura {
    private static final BigDecimal IGV_FACTOR = new BigDecimal("1.18");
    private static final Map<String, BigDecimal> PRECIOS_PRODUCTO = Map.ofEntries(
            Map.entry("Procesador (CPU)", new BigDecimal("650.00")),
            Map.entry("Placa madre", new BigDecimal("480.00")),
            Map.entry("Memoria RAM", new BigDecimal("180.00")),
            Map.entry("Tarjeta gráfica (GPU)", new BigDecimal("1800.00")),
            Map.entry("SSD NVMe", new BigDecimal("220.00")),
            Map.entry("SSD SATA", new BigDecimal("150.00")),
            Map.entry("Disco duro (HDD)", new BigDecimal("260.00")),
            Map.entry("Fuente de poder", new BigDecimal("300.00")),
            Map.entry("Gabinete", new BigDecimal("280.00")),
            Map.entry("Disipador para procesador", new BigDecimal("120.00")),
            Map.entry("Refrigeración líquida", new BigDecimal("520.00")),
            Map.entry("Ventilador para gabinete", new BigDecimal("65.00")),
            Map.entry("Tarjeta Wi-Fi", new BigDecimal("90.00")),
            Map.entry("Tarjeta de red Ethernet", new BigDecimal("75.00")),
            Map.entry("Tarjeta de sonido", new BigDecimal("180.00")),
            Map.entry("Capturadora de video", new BigDecimal("350.00")),
            Map.entry("Unidad óptica", new BigDecimal("110.00")),
            Map.entry("Pasta térmica", new BigDecimal("25.00")),
            Map.entry("Tarjeta PCIe USB", new BigDecimal("130.00")),
            Map.entry("Batería CMOS", new BigDecimal("15.00")));
    public static final List<String> PRODUCTOS = List.of(
            "Procesador (CPU)",
            "Placa madre",
            "Memoria RAM",
            "Tarjeta gráfica (GPU)",
            "SSD NVMe",
            "SSD SATA",
            "Disco duro (HDD)",
            "Fuente de poder",
            "Gabinete",
            "Disipador para procesador",
            "Refrigeración líquida",
            "Ventilador para gabinete",
            "Tarjeta Wi-Fi",
            "Tarjeta de red Ethernet",
            "Tarjeta de sonido",
            "Capturadora de video",
            "Unidad óptica",
            "Pasta térmica",
            "Tarjeta PCIe USB",
            "Batería CMOS");

    private CalculadoraFactura() {
    }

    public static Factura calcular(String nombreCliente, String producto, int cantidad) {
        String nombre = nombreCliente == null ? "" : nombreCliente.trim();
        if (nombre.isEmpty()) {
            throw new IllegalArgumentException("Ingresa el nombre del cliente.");
        }
        if (nombre.length() > 100) {
            throw new IllegalArgumentException("El nombre no puede superar 100 caracteres.");
        }
        String productoSeleccionado = producto == null ? "" : producto.trim();
        if (!PRODUCTOS.contains(productoSeleccionado)) {
            throw new IllegalArgumentException("Selecciona un componente válido.");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser un entero positivo.");
        }

        BigDecimal precioUnitario = precioPara(productoSeleccionado, cantidad);
        BigDecimal precioReferencial = PRECIOS_PRODUCTO.get(productoSeleccionado);
        BigDecimal importeCompra = precioUnitario
                .multiply(BigDecimal.valueOf(cantidad))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal tasaDescuento = cantidad > 500
                ? new BigDecimal("0.15")
                : new BigDecimal("0.05");
        BigDecimal descuento = importeCompra
                .multiply(tasaDescuento)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPagar = importeCompra.subtract(descuento);
        BigDecimal subtotal = totalPagar.divide(IGV_FACTOR, 2, RoundingMode.HALF_UP);
        BigDecimal igv = totalPagar.subtract(subtotal);

        return new Factura(
                nombre,
                productoSeleccionado,
                precioReferencial,
                cantidad,
                precioUnitario,
                importeCompra,
                descuento,
                subtotal,
                igv,
                totalPagar);
    }

    private static BigDecimal precioPara(String producto, int cantidad) {
        BigDecimal precioBase = PRECIOS_PRODUCTO.get(producto);
        BigDecimal factor;
        if (cantidad <= 100) {
            factor = BigDecimal.ONE;
        } else if (cantidad <= 250) {
            factor = new BigDecimal("0.92");
        } else if (cantidad <= 500) {
            factor = new BigDecimal("0.704");
        } else {
            factor = new BigDecimal("0.491");
        }
        return precioBase.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }
}
