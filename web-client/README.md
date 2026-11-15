# Cliente web (Angular): Ventas y Entregas

Estructura base para quien desarrolle este cliente. El backend es el que está en `../backend`; este cliente solo habla con su API HTTP y nunca con la base de datos.

## Qué debe hacer

- **Vendedor:** iniciar sesión, ver productos con disponibilidad, buscar o registrar clientes, registrar ventas, ver y cancelar sus ventas pendientes.
- **Entregas:** iniciar sesión, ver ventas pendientes de entrega y despacharlas.

Cada pantalla debe consumir la API de verdad y mostrar los errores del servidor.

## Contrato con el backend

- URL base en desarrollo: `http://localhost:8080`.
- Sesión: cookie del servidor. Hay que enviar las peticiones con credenciales (`withCredentials`).
- CSRF: Spring publica la cookie `XSRF-TOKEN`; Angular la lee y envía `X-XSRF-TOKEN` en las peticiones que modifican datos.
- Endpoints, ejemplos de solicitud y respuesta y códigos de error: sección 6 de [docs/propuesta-polimarket.html](../docs/propuesta-polimarket.html).

## Pendiente

Crear el proyecto Angular aquí (versión por acordar).
