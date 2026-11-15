# Cliente de escritorio (C++): Recursos Humanos y Bodega

Estructura base para quien desarrolle este cliente. Solo habla con la API HTTP de `../backend`, nunca con la base de datos.

## Qué debe hacer

- **Recursos Humanos:** iniciar sesión, buscar o registrar un tercero, crear, modificar y desactivar cuentas, listar vendedores y aprobar o rechazar su aval.
- **Bodega:** iniciar sesión, gestionar productos y proveedores y asociarlos, consultar existencias y bajo stock, registrar entradas y ajustes con motivo.

## Contrato con el backend

- URL base en desarrollo: `http://localhost:8080`.
- Sesión: cookie del servidor (`JSESSIONID`). El cliente debe guardarla y reenviarla.
- CSRF: tras el login, leer la cookie `XSRF-TOKEN` y enviarla en el encabezado `X-XSRF-TOKEN` en las peticiones POST, PATCH y DELETE.
- **Empieza por [docs/guides/clients.md](../docs/guides/clients.md)**: cómo levantar el backend, cómo manejar la sesión y el token CSRF desde C++ y qué pantalla llama a qué.
- Todos los endpoints con ejemplos y códigos de error: [docs/reference/api-rest.md](../docs/reference/api-rest.md), o Swagger UI en `http://localhost:8080/swagger-ui.html`.

## Pendiente

Elegir la biblioteca gráfica y crear el proyecto aquí.
