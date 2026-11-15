package polimarket.bodega.api;

import java.util.List;
import java.util.Optional;

/**
 * Contrato del componente Bodega. Disponible = stock físico menos reservas activas.
 * Las operaciones de reserva (reservar, liberar y consumir) se agregan con el componente Ventas.
 */
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

    /** Registra el ingreso de mercancía de un proveedor y deja el movimiento trazable. */
    MovimientoDto registrarEntrada(NuevaEntrada entrada, long usuarioId);

    /**
     * Corrige el stock físico con motivo. No puede dejarlo negativo ni por debajo de las reservas
     * activas (STOCK_INSUFICIENTE).
     */
    MovimientoDto registrarAjuste(NuevoAjuste ajuste, long usuarioId);
}
