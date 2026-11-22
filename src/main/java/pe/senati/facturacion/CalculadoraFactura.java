package pe.senati.facturacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class CalculadoraFactura {
    private static final BigDecimal IGV_FACTOR = new BigDecimal("1.18");
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

        BigDecimal precioUnitario = precioPara(cantidad);
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
                cantidad,
                precioUnitario,
                importeCompra,
                descuento,
                subtotal,
                igv,
                totalPagar);
    }

    private static BigDecimal precioPara(int cantidad) {
        if (cantidad <= 100) {
            return new BigDecimal("27.70");
        }
        if (cantidad <= 250) {
            return new BigDecimal("25.50");
        }
        if (cantidad <= 500) {
            return new BigDecimal("19.50");
        }
        return new BigDecimal("13.60");
    }
}
