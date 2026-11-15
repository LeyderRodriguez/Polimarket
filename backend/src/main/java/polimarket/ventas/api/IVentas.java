package polimarket.ventas.api;

import java.util.List;

/** Contrato de ventas. */
public interface IVentas {

    /**
     * Registra una venta con todas sus reservas o ninguna. Lanza VENDEDOR_SIN_AVAL si el vendedor no tiene
     * aval vigente y STOCK_INSUFICIENTE si algún producto no alcanza.
     */
    VentaDto registrar(long vendedorId, NuevaVenta venta);

    List<VentaResumenDto> listarVentasDelVendedor(long vendedorId);

    /**
     * Anula una venta propia pendiente y libera su reserva. No exige aval vigente. Lanza SIN_PERMISO si la
     * venta es de otro vendedor y ESTADO_INVALIDO si ya no está pendiente.
     */
    VentaDto anular(long ventaId, long vendedorId);

    List<VentaPendienteDto> listarPendientesEntrega();

    /** La usa Entregas: pasa la venta a DESPACHADA una sola vez (ESTADO_INVALIDO si ya no está pendiente). */
    VentaPendienteDto marcarDespachada(long ventaId);
}
