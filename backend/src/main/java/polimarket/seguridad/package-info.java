/**
 * Componente Seguridad (transversal).
 *
 * <p>Provee {@code IAutenticacion}: iniciar y cerrar sesión, crear y desactivar cuentas, rol y permisos.
 * No requiere otros componentes; la cuenta apunta a la persona solo por {@code idTercero}.
 * Esquema de base de datos: {@code seguridad}.
 * <p>Lo público está en el subpaquete {@code api}; {@code dominio} y {@code aplicacion} son internos.
 */
package polimarket.seguridad;
