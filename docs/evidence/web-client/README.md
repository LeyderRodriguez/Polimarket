# Evidencia del cliente web

Recorrido del cliente web contra el backend real (Docker), con el tema claro. Cada captura sale de ejecutar la pantalla de
verdad, no de una maqueta. Para repetirlo: [web-client/README.md](../../../web-client/README.md) y la cuenta de cada rol
en la pantalla de inicio de sesión (contraseña de demostración común).

## Requisito → pantalla

| Requisito | Pantalla | Cuenta | Captura |
|---|---|---|---|
| Inicio de sesión por rol | Login | cualquiera | [01](01-login.png) |
| RSI-2 · Disponibilidad (físico − reservado) | Disponibilidad | Ana | [02](02-disponibilidad.png) |
| Registro de clientes en tres pasos | Clientes | Ana | [03](03-clientes.png), [04](04-registrar-cliente.png) |
| RSI-3 · Registrar venta (carrito, total y reserva calculados por el servidor) | Nueva venta | Ana | [05](05-nueva-venta-carrito.png), [06](06-nueva-venta-registrada.png) |
| RSI-6 · Anular una venta pendiente (confirmación en la fila) | Mis ventas | Ana | [07](07-mis-ventas.png), [08](08-mis-ventas-confirmar-anulacion.png) |
| Regla: el vendedor necesita aval vigente | Nueva venta | Luis (sin aval) | [09](09-venta-rechazada-sin-aval.png) |
| RSI-5 · Ventas pendientes de entrega | Entregas | Entregas | [10](10-entregas.png) |
| Rol que no es de este cliente | Login | RH | [11](11-rol-no-soportado.png) |
| El cliente solo consume la API: llamadas reales con lo enviado y lo recibido | Modo demostración | Ana | [12](12-modo-demostracion.png) |

## Qué demuestra cada caso

- **El backend decide.** El rechazo por falta de aval (09) y el de rol (11) los devuelve el servidor con su código
  (`VENDEDOR_SIN_AVAL`, `ROL_NO_SOPORTADO`); la pantalla solo los muestra. En 09 la pantalla ya falla al cargar porque el
  servidor niega la consulta a un vendedor sin aval.
- **El total no lo calcula el cliente.** En 06 el total y el estado de la reserva vienen de la respuesta del servidor.
- **Lo irreversible pide confirmación** en la propia fila (08).

## Lo que no cubre este cliente

RSI-1 (aval) y RSI-4 (bajo stock) son de Recursos Humanos y Bodega: van en el cliente de escritorio. Mientras no exista, se
prueban por Swagger o con los scripts de `scripts/` ([guía](../../guides/clients.md)).

## Pruebas automáticas

94 pruebas con Vitest (servicios, guardias, interceptor y componentes) y la compilación de producción limpia:
`npm test` y `npm run build` dentro de `web-client/`. Las pruebas de extremo a extremo con Playwright se descartaron: este
recorrido se hizo en un navegador real contra el backend y queda aquí como evidencia.
