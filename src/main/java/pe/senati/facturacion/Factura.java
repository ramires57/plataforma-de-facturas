package pe.senati.facturacion;

import java.math.BigDecimal;

public record Factura(
        String nombreCliente,
        String producto,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal importeCompra,
        BigDecimal descuento,
        BigDecimal subtotal,
        BigDecimal igv,
        BigDecimal totalPagar) {
}
