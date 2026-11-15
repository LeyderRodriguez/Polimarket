package polimarket.ventas.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import polimarket.bodega.api.EstadoReserva;

public record VentaDto(
        Long ventaId,
        EstadoVenta estado,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal total,
        EstadoReserva reservaEstado) {
}
