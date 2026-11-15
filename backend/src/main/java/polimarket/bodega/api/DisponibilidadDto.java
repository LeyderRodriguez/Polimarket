package polimarket.bodega.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;

/** Disponible = stock físico menos las unidades reservadas por ventas pendientes. */
public record DisponibilidadDto(
        Long id,
        String codigo,
        String nombre,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal precio,
        int stockFisico,
        int cantidadReservada,
        int cantidadDisponible) {
}
