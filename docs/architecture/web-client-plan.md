# Plan del cliente web (Angular)

Qué hace el cliente web, cómo se organiza y en qué orden se construye. Es un cliente más del backend: no tiene reglas
de negocio propias, solo muestra lo que responde la API y envía lo que el usuario pide.

## Qué cubre

Dos roles, cuatro requisitos de la actividad:

| Rol | Pantalla | Requisito | Endpoints |
|---|---|---|---|
| Ambos | Inicio de sesión | RSI-1 (acceso solo con aval) | `POST /api/auth/login`, `GET /api/auth/me`, `POST /api/auth/logout` |
| Vendedor | Disponibilidad | **RSI-2** | `GET /api/productos` |
| Vendedor | Nueva venta | **RSI-3** | `POST /api/ventas`, `GET /api/clientes`, `GET /api/terceros`, `POST /api/terceros`, `POST /api/clientes` |
| Vendedor | Mis ventas | **RSI-6** | `GET /api/ventas/mis-ventas`, `POST /api/ventas/{id}/cancelacion` |
| Vendedor | Clientes | Apoyo de RSI-3 | `GET/POST/PATCH /api/clientes` |
| Entregas | Entregas pendientes | **RSI-5** | `GET /api/ventas/pendientes-entrega`, `POST /api/ventas/{id}/despacho` |

Cada rol consume más de dos funcionalidades, así que el cliente web cumple «dos clientes con al menos dos
funcionalidades» junto con el de escritorio (RSI-1 y RSI-4).

## Reglas del cliente

1. **El backend es la fuente de verdad.** El cliente no calcula totales ni decide permisos ni valida stock. Muestra el
   `mensaje` del error `{ codigo, mensaje }` que devuelve la API.
2. **Los tipos salen del backend, no se escriben a mano.** `npm run api:types` (con el backend en marcha) lee
   `/v3/api-docs` con `openapi-typescript` y regenera `core/api/schema.d.ts`, que se sube al repositorio. Si el backend
   cambia un campo, el proyecto deja de compilar en la pantalla afectada. Los servicios sí se escriben a mano.
3. **Rutas relativas (`/api/...`) y proxy de `ng serve`.** Así la cookie de sesión y el token CSRF funcionan sin código
   extra (Angular reenvía `XSRF-TOKEN` como `X-XSRF-TOKEN` solo en rutas relativas).
4. **Ocultar un botón es comodidad, no seguridad.** Los permisos los comprueba el backend.

## Estructura

Se conservan los nombres de las pantallas de Manuel (`ventas`, `disponibilidad`, `entregas`), ahora agrupadas por
funcionalidad en `features/`, y se añade lo transversal:

```
web-client/src/app/
  core/                       ← existe una sola vez en toda la app
    api/schema.d.ts             tipos generados desde el backend (no editar a mano)
    api/tipos.ts                alias legibles y ErrorApi
    api/*.api.ts                un servicio fino por recurso: auth, y luego productos, ventas, clientes…
    auth/                       sesión actual (signal), guardias de sesión y de rol
    http/                       interceptor: errores de la API y sesión caducada
    layout/shell/               menú lateral según el rol
  shared/                     ← piezas reutilizables sin lógica de negocio (aviso-error, …)
  features/                   ← una carpeta por pantalla, con carga diferida
    login/  disponibilidad/  ventas/{nueva-venta,mis-ventas}/  clientes/  entregas/
  app.routes.ts               ← rutas con guardia por rol
```

Reglas: una pantalla usa `core` y `shared`, nunca otra pantalla; `core` no importa `features`. Nombres en
`kebab-case`; los servicios llevan sufijo (`.api.ts`, `.servicio.ts`, `.guard.ts`, `.interceptor.ts`).

Componentes independientes (standalone), `signal` para el estado y `HttpClient` con interceptor funcional. Sin
librerías de interfaz: CSS propio con variables, para no añadir dependencias.

## Fases

Cada fase termina con algo que funciona contra el backend real y con una revisión de código antes de seguir.

| Fase | Qué se entrega | Revisión |
|---|---|---|
| **0. Entorno** *(hecha)* | Node 24 LTS, `npm ci`, proxy a `:8080`, tipos generados desde OpenAPI, pruebas en verde | — |
| **1. Base** *(hecha)* | Cliente HTTP con interceptor, servicio de autenticación, guardia por rol, menú según rol, aviso de errores, login funcionando | Sí, al cerrarla |
| **2. Disponibilidad (RSI-2)** | Lista de productos con físico, reservado y disponible, búsqueda por nombre | |
| **3. Nueva venta (RSI-3)** | Buscar o registrar tercero y cliente, armar el carrito, confirmar; errores `STOCK_INSUFICIENTE` y `VENDEDOR_SIN_AVAL` | Sí, al cerrar las fases 2 y 3 |
| **4. Mis ventas y Entregas (RSI-6 y RSI-5)** | Anular ventas pendientes; listar pendientes y despachar | |
| **5. Cierre** | Pruebas con Vitest de servicios y guardia, README, capturas, recorrido completo contra Docker | Sí, revisión final |

## Cómo se prueba

- **Pruebas de unidad (Vitest, ya configurado):** servicios con `HttpTestingController`, guardia y mapeo de errores.
- **Recorrido manual contra el backend en Docker** con las cuentas de demostración (`ana@`, `entregas@`) y la secuencia
  de `scripts/probar-flujo-venta.sh` como guion.
