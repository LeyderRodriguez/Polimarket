# Cliente web (Angular): Ventas y Entregas

Estructura base para quien desarrolle este cliente. El backend es el que está en `../backend`; este cliente solo habla con su API HTTP y nunca con la base de datos.

## Qué debe hacer

- **Vendedor:** iniciar sesión, ver productos con disponibilidad, buscar o registrar clientes, registrar ventas, ver y cancelar sus ventas pendientes.
- **Entregas:** iniciar sesión, ver ventas pendientes de entrega y despacharlas.

Cada pantalla debe consumir la API de verdad y mostrar los errores del servidor.

## Contrato con el backend

- URL base en desarrollo: `http://localhost:8080`.
- Sesión: cookie del servidor. Hay que enviar las peticiones con credenciales (`withCredentials`).
- CSRF: Spring publica la cookie `XSRF-TOKEN` y espera su valor en el encabezado `X-XSRF-TOKEN` en los `POST` y `PATCH`. Angular lo hace solo únicamente con rutas relativas (usa el proxy de `ng serve`); con otro origen hay que ponerlo con un interceptor. La guía lo explica.
- **Empieza por [docs/guia-clientes.md](../docs/guia-clientes.md)**: cómo levantar el backend, el proxy de Angular, el token CSRF y qué pantalla llama a qué.
- Todos los endpoints con ejemplos y códigos de error: [docs/api-rest.md](../docs/api-rest.md), o Swagger UI en `http://localhost:8080/swagger-ui.html`.

## Pendiente

Crear el proyecto Angular aquí (versión por acordar).
