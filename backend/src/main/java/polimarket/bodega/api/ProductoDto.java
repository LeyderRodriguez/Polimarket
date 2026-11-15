package polimarket.bodega.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;

public record ProductoDto(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        String categoria,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal precioVenta,
        int stockMinimo) {
}
