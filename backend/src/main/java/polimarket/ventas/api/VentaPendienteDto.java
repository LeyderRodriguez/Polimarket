package polimarket.ventas.api;

import java.util.List;

/** Venta que espera despacho: qué entregar, a quién y dónde. */
public record VentaPendienteDto(Long ventaId, String cliente, String direccionEntrega, List<Producto> productos) {

    public record Producto(Long productoId, String nombre, int cantidad) {
    }
}
