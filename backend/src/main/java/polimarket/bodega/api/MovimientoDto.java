package polimarket.bodega.api;

/** Movimiento de inventario trazable: qué cambió, de cuánto a cuánto. */
public record MovimientoDto(
        Long movimientoId,
        Long productoId,
        TipoMovimiento tipo,
        int cantidad,
        int stockAnterior,
        int stockFisico) {
}
