package polimarket.entregas.api;

import java.util.List;
import polimarket.ventas.api.VentaPendienteDto;

/** Contrato de Entregas: qué falta despachar y el despacho. */
public interface IEntregas {

    /** Ventas pendientes de entrega: qué entregar, a quién y dónde (las consulta a Ventas). */
    List<VentaPendienteDto> listarPendientes();

    /**
     * Despacha una venta pendiente una sola vez: la marca despachada, consume su reserva y registra la salida
     * de inventario, todo en una transacción. Un segundo despacho o el de una venta anulada da ESTADO_INVALIDO.
     * No exige que el vendedor conserve el aval: la venta ya fue aceptada.
     */
    EntregaDto despachar(long ventaId, long usuarioId);
}
