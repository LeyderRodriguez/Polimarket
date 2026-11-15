/**
 * Componente Bodega.
 *
 * <p>Provee {@code IInventario}: productos, existencias, entradas, ajustes, reservas (reservar,
 * liberar, consumir) y bajo stock. Disponible = existencia física menos reservas activas.
 * Requiere {@code IProveedores}. Esquema de base de datos: {@code bodega}.
 */
package polimarket.bodega;
