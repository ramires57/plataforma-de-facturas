package pe.senati.facturacion;

import java.math.BigDecimal;
import java.util.List;

public record FacturaMultiple(
        String nombreCliente,
        List<LineaFactura> lineas,
        int cantidadTotal,
        BigDecimal importeCompra,
        BigDecimal descuento,
        BigDecimal subtotal,
        BigDecimal igv,
        BigDecimal totalPagar) {
}
