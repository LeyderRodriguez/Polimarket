# Cliente de escritorio (C++): Recursos Humanos y Bodega

Estructura base para quien desarrolle este cliente. Solo habla con la API HTTP de `../backend`, nunca con la base de datos.

## Qué debe hacer

- **Recursos Humanos:** iniciar sesión, buscar o registrar un tercero, crear, modificar y desactivar cuentas, listar vendedores y aprobar o rechazar su aval.
- **Bodega:** iniciar sesión, gestionar productos y proveedores y asociarlos, consultar existencias y bajo stock, registrar entradas y ajustes con motivo.

## Contrato con el backend

- URL base en desarrollo: `http://localhost:8080`.
- Sesión: cookie del servidor (`JSESSIONID`). El cliente debe guardarla y reenviarla.
- CSRF: tras el login, leer la cookie `XSRF-TOKEN` y enviarla en el encabezado `X-XSRF-TOKEN` en las peticiones POST, PATCH y DELETE.
- Endpoints, ejemplos de solicitud y respuesta y códigos de error: sección 6 de [docs/propuesta-polimarket.html](../docs/propuesta-polimarket.html).

## Pendiente

Elegir la biblioteca gráfica y crear el proyecto aquí.
