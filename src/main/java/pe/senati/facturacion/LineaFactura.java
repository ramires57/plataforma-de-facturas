package pe.senati.facturacion;

import java.math.BigDecimal;

public record LineaFactura(
        String producto,
        int cantidad,
        BigDecimal precioReferencial,
        BigDecimal precioUnitario,
        BigDecimal importeCompra) {
}
