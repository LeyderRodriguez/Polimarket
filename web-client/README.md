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
- **Empieza por [docs/guides/clients.md](../docs/guides/clients.md)**: cómo levantar el backend, el proxy de Angular, el token CSRF y qué pantalla llama a qué.
- Todos los endpoints con ejemplos y códigos de error: [docs/reference/api-rest.md](../docs/reference/api-rest.md), o Swagger UI en `http://localhost:8080/swagger-ui.html`.

## Cómo ejecutarlo

Requisitos: **Node.js 24 LTS** (también sirven 22.12+ y 20.19+; Angular 21 no soporta oficialmente la 26) con npm, y el backend en marcha (`docker compose up --build` en la raíz).

```bash
cd web-client
npm ci
npm start          # ng serve en http://localhost:4200 (las llamadas a /api pasan por proxy al backend)
```

| Comando | Para qué |
|---|---|
| `npm start` | Servidor de desarrollo con proxy a `http://localhost:8080` |
| `npm test` | Pruebas con Vitest |
| `npm run build` | Compilación de producción |
| `npm run api:types` | Regenera `src/app/core/api/schema.d.ts` desde el contrato del backend (con el backend en marcha) |

La pantalla de inicio de sesión trae **botones de cuentas de demostración** (Ana con aval, Luis sin aval, Entregas, RH y
Bodega): un clic llena correo y contraseña y solo queda pulsar Entrar. La clave está en
[docs/guides/clients.md](../docs/guides/clients.md). Las cuentas de RH y Bodega se rechazan: usan el cliente de escritorio.

## Estructura

```
src/app/
  core/       api/ (tipos generados y llamadas HTTP) · auth/ (sesión y guardias) · http/ (interceptor) · layout/ (menú lateral)
  shared/     piezas que usan varias pantallas (aviso de error, diálogo de registro de cliente)
  features/   una carpeta por pantalla: login, disponibilidad, ventas (nueva-venta, mis-ventas), clientes, entregas
```

Nombres en `kebab-case`; un componente son cuatro archivos (`.ts`, `.html`, `.css`, `.spec.ts`); el resto lleva sufijo
(`.api.ts`, `.servicio.ts`, `.guard.ts`, `.interceptor.ts`). Una pantalla puede usar `core` y `shared`, nunca otra pantalla.

## Estado

Fases 0 a 3 hechas: inicio y cierre de sesión, sesión que sobrevive a recargar la página, guardias por rol, menú lateral,
manejo central de errores, **Disponibilidad** (RSI-2), **Clientes** (registro en tres pasos) y **Nueva venta** (RSI-3, con
carrito). **Mis ventas** y **Entregas** son marcadores que se llenan en la fase 4; el plan está en
[docs/architecture/web-client-plan.md](../docs/architecture/web-client-plan.md).
