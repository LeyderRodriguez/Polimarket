package polimarket.bodega.api;

import java.math.BigDecimal;

/** Ingreso de mercancía comprada a un proveedor. El costo unitario es opcional. */
public record NuevaEntrada(long productoId, int cantidad, long proveedorId, String referencia,
        BigDecimal costoUnitario) {
}
