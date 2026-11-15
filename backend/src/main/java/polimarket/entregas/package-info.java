/**
 * Componente Entregas.
 *
 * <p>Provee {@code IEntregas}: consultar ventas pendientes de entrega y despacharlas una sola vez.
 * Requiere {@code IVentas} (qué entregar y a quién) e {@code IInventario} (consumir la reserva y
 * registrar la salida). Esquema de base de datos: {@code entregas}.
 * <p>Lo público está en el subpaquete {@code api}; {@code dominio} y {@code aplicacion} son internos.
 */
package polimarket.entregas;
