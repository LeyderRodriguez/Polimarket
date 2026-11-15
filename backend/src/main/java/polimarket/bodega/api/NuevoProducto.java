package polimarket.bodega.api;

import java.math.BigDecimal;

public record NuevoProducto(
        String codigo,
        String nombre,
        String descripcion,
        String categoria,
        BigDecimal precioVenta,
        int stockMinimo) {
}
