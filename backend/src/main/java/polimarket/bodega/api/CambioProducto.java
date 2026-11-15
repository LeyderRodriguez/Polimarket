package polimarket.bodega.api;

import java.math.BigDecimal;

/** Cambios parciales: un campo nulo se deja como estaba. El código no se puede cambiar. */
public record CambioProducto(
        String nombre,
        String descripcion,
        String categoria,
        BigDecimal precioVenta,
        Integer stockMinimo) {
}
