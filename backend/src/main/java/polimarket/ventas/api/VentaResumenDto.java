package polimarket.ventas.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;

public record VentaResumenDto(
        Long ventaId,
        String cliente,
        EstadoVenta estado,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal total) {
}
