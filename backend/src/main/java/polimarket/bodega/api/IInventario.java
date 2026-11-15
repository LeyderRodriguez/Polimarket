package polimarket.bodega.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Contrato del componente Bodega. Disponible = stock físico menos reservas activas. */
public interface IInventario {

    /** Crea el producto con existencia inicial cero. Lanza CODIGO_DUPLICADO si el código ya existe. */
    ProductoDto crearProducto(NuevoProducto producto);

    ProductoDto actualizarProducto(long productoId, CambioProducto cambio);

    Optional<ProductoDto> consultarProducto(long productoId);

    /** Productos con stock físico, reservado y disponible. */
    List<DisponibilidadDto> listarDisponibilidad();

    DisponibilidadDto consultarDisponibilidad(long productoId);

    /** Productos con disponible por debajo del mínimo, con sus proveedores. */
    List<BajoStockDto> listarBajoStock();

    /** Varios productos en una sola consulta (para evitar una consulta por producto). */
    List<ProductoDto> consultarProductos(Collection<Long> productoIds);

    /**
     * Reserva todas las unidades pedidas o ninguna: si un producto no alcanza, no queda ninguna reserva
     * (STOCK_INSUFICIENTE). Dos ventas del mismo producto se atienden una tras otra.
     */
    ReservaDto reservar(long ventaId, List<ItemReserva> items);

    /** Libera la reserva activa de una venta anulada. El stock físico no cambia. */
    ReservaDto liberarReserva(long ventaId);

    /** Consume la reserva al despachar: descuenta el stock físico y registra las salidas. */
    ReservaDto consumirReserva(long ventaId, long usuarioId);

    /** Registra el ingreso de mercancía de un proveedor y deja el movimiento trazable. */
    MovimientoDto registrarEntrada(NuevaEntrada entrada, long usuarioId);

    /**
     * Corrige el stock físico con motivo. No puede dejarlo negativo ni por debajo de las reservas
     * activas (STOCK_INSUFICIENTE).
     */
    MovimientoDto registrarAjuste(NuevoAjuste ajuste, long usuarioId);
}
