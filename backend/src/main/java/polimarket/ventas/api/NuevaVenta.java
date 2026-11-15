package polimarket.ventas.api;

import java.util.List;

/** Solicitud de venta. El vendedor no viaja aquí: sale de la sesión. Los precios los calcula el sistema. */
public record NuevaVenta(long clienteId, String direccionEntrega, List<ItemVenta> productos) {
}
