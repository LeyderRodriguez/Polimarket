package polimarket.bodega.api;

import java.util.List;

/** Producto cuya cantidad disponible está por debajo del mínimo, con quién lo suministra. */
public record BajoStockDto(
        Long productoId,
        String nombre,
        int stockFisico,
        int cantidadReservada,
        int cantidadDisponible,
        int stockMinimo,
        List<Proveedor> proveedores) {

    public record Proveedor(Long id, String nombre) {
    }
}
