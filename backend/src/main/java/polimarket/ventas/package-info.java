/**
 * Componente Ventas.
 *
 * <p>Provee {@code IClientes} e {@code IVentas}: clientes, registrar, consultar y anular ventas
 * pendientes. Requiere {@code IAutorizacion} (aval del vendedor), {@code IInventario} (reservar y
 * liberar) e {@code ITerceros}. Esquema de base de datos: {@code ventas}.
 * <p>Lo público está en el subpaquete {@code api}; {@code dominio} y {@code aplicacion} son internos.
 */
package polimarket.ventas;
