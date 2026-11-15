# Guía rápida para los clientes (Angular y C++)

Si vas a hacer un cliente de PoliMarket, empieza aquí. En 10 minutos debes tener el backend corriendo y haber
hecho tu primera llamada. El detalle de cada endpoint está en [api-rest.md](../reference/api-rest.md).

## 1. Levantar el backend

Solo necesitas Docker Desktop encendido:

```bash
git clone git@github.com:LeyderRodriguez/Polimarket.git
cd Polimarket
git checkout develop
docker compose up --build
```

Cuando veas `Started PoliMarketApplication`, el backend está en **http://localhost:8080** con datos de prueba.

| Qué | Dónde |
|---|---|
| API | `http://localhost:8080/api/...` |
| **Swagger UI** (probar todo desde el navegador) | `http://localhost:8080/swagger-ui.html` |
| Contrato en JSON (para generar código) | `http://localhost:8080/v3/api-docs` |

Si cambias algo en el backend y quieres verlo: `docker compose up --build -d backend`.
Para empezar con datos limpios: `docker compose down -v` y volver a levantar.

## 2. Cuentas de prueba

**La clave de todas las cuentas es `Demo-1234`** (ficticia, solo para desarrollo; sale de
`polimarket.demo.clave` en `backend/src/main/resources/application.yml`).

| Correo | Rol | Quién lo usa | Qué puede hacer |
|---|---|---|---|
| `rh@polimarket.test` | RECURSOS_HUMANOS | **Desktop (C++)** | Crear cuentas, aprobar avales |
| `bodega@polimarket.test` | BODEGA | **Desktop (C++)** | Productos, proveedores, entradas, ajustes, bajo stock |
| `ana@polimarket.test` | VENDEDOR (aval aprobado) | **Web (Angular)** | Ver productos, clientes, vender, cancelar |
| `luis@polimarket.test` | VENDEDOR (aval pendiente) | Web | Entra, pero no puede vender (403) |
| `entregas@polimarket.test` | ENTREGAS | **Web (Angular)** | Ver ventas pendientes y despacharlas |

## 3. Probar desde Swagger UI

Es la forma más rápida de ver qué hace cada endpoint sin escribir código.

1. Abre **http://localhost:8080/swagger-ui.html**. Usa siempre `localhost`, no `127.0.0.1` (ver más abajo).
2. **Inicia sesión primero:** abre *Sesión → POST /api/auth/login*, pulsa *Try it out* y envía

   ```json
   { "correo": "ana@polimarket.test", "contrasena": "Demo-1234" }
   ```

   Debe responder `200` con tu perfil. El navegador guarda la cookie de sesión.
3. Prueba lo que quieras, por ejemplo `GET /api/productos`. Las escrituras (`POST`, `PATCH`) llevan solas el token CSRF.

Cada operación dice qué roles pueden usarla. Cambia de cuenta (`bodega@`, `rh@`, `entregas@`) haciendo login de nuevo.

### Si algo sale mal

| Qué ves | Causa y solución |
|---|---|
| `401` · `NO_AUTENTICADO` · «Debe iniciar sesión» | No has hecho el login, o la sesión expiró. Haz el login (paso 2) y repite |
| `401` · `CREDENCIALES_INVALIDAS` | Correo o clave mal escritos. La clave es `Demo-1234` |
| El login da `200` pero la siguiente llamada da `401` | Abriste Swagger con `127.0.0.1` y no con `localhost` (o al revés): la cookie es de un solo host. Usa `http://localhost:8080` |
| Funcionaba y de pronto da `401` | Se reinició el backend (las sesiones viven en memoria) o pasaron 30 minutos sin actividad. Haz login de nuevo |
| `403` · `SIN_PERMISO` | Estás logueado con un rol que no puede hacer eso. `/api/clientes` y `/api/ventas` son del **vendedor**; `/api/productos` (escritura) y `/api/inventario` de **Bodega** |
| `403` con el rol correcto, desde `curl` o tu cliente | Falta el encabezado `X-XSRF-TOKEN` en un `POST`/`PATCH` (Swagger lo pone solo) |
| `403` · `VENDEDOR_SIN_AVAL` | Entraste con `luis@polimarket.test`, cuyo aval está pendiente. Usa `ana@polimarket.test` |
| Cambié el backend y no se nota | Docker usa la imagen vieja: `docker compose up --build -d backend` |
| Las cantidades de stock no son las del ejemplo | La base ya tiene ventas de pruebas anteriores. Empieza limpio con `docker compose down -v` y vuelve a levantar |

Swagger marca «Undocumented» junto a los códigos 401 y 403: es solo una etiqueta, porque en el contrato solo se
describe la respuesta exitosa. Los errores están en [api-rest.md](../reference/api-rest.md#errores).

## 4. Las 5 reglas que tu cliente debe cumplir

1. **Sesión con cookie.** Haz `POST /api/auth/login` y guarda la cookie `JSESSIONID` que responde el servidor.
   Envíala en todas las llamadas siguientes. En Angular: `withCredentials: true`.
2. **Token CSRF en cada escritura.** En el mismo login el servidor deja la cookie `XSRF-TOKEN`. En todo
   `POST`, `PATCH` o `DELETE`, copia su valor al encabezado **`X-XSRF-TOKEN`**. Sin él: `403`. Los `GET` no lo necesitan.
3. **Los errores siempre tienen la misma forma:** `{ "codigo": "STOCK_INSUFICIENTE", "mensaje": "..." }`.
   Muestra el `mensaje` al usuario y usa el `codigo` para decidir qué hacer.
4. **Los montos son texto:** `"5000.00"`, no número. Conviértelos para mostrarlos, nunca para calcular dinero con decimales binarios.
5. **Si recibes `401`, vuelve a la pantalla de login.** La sesión dura 30 minutos sin actividad.

El backend **no confía en el cliente**: el vendedor que vende y el responsable que aprueba salen de la sesión, no de lo
que envíes. Los precios los pone el servidor.

## 5. Tu primera llamada

```bash
# 1. Entrar (guarda las cookies en jar.txt)
curl -c jar.txt -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"correo":"ana@polimarket.test","contrasena":"Demo-1234"}'
# → {"id":4,"nombre":"Ana Pérez","rol":"VENDEDOR"}

# 2. Leer (solo la cookie de sesión)
curl -b jar.txt http://localhost:8080/api/productos

# 3. Escribir (cookie + token CSRF en el encabezado)
TOKEN=$(awk '$6=="XSRF-TOKEN"{print $7}' jar.txt)
curl -b jar.txt -X POST http://localhost:8080/api/ventas \
  -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: $TOKEN" \
  -d '{"clienteId":1,"direccionEntrega":"Calle 10 # 20-30","productos":[{"productoId":1,"cantidad":2}]}'
```

El recorrido completo, listo para ejecutar, está en `scripts/probar-flujo-venta.sh`.

## 6. Angular

**Lo más fácil: usa el proxy de desarrollo.** Así el navegador cree que todo viene de `localhost:4200`, no hay CORS
y Angular pone el token CSRF solo.

`proxy.conf.json`:
```json
{ "/api": { "target": "http://localhost:8080", "secure": false } }
```
Se declara en `angular.json` (`serve.options.proxyConfig`) y se arranca con `npm start`. Llama a rutas **relativas**
(`/api/productos`). **En `web-client/` ya está configurado así**: no hace falta tocar nada.

Angular usa por defecto los mismos nombres que Spring (cookie `XSRF-TOKEN`, encabezado `X-XSRF-TOKEN`), así que
`provideHttpClient()` basta; solo hay que configurarlo (`withXsrfConfiguration`) si los nombres fueran otros.

**Si llamas a `http://localhost:8080` directamente** (origen distinto), Angular **no** agrega el token CSRF solo:
tienes que ponerlo tú con un interceptor, y usar `withCredentials: true`. El backend ya acepta el origen
`http://localhost:4200` (se cambia con la propiedad `polimarket.cors.origenes`).

```ts
export const csrfInterceptor: HttpInterceptorFn = (req, next) => {
  const token = document.cookie.split('; ').find(c => c.startsWith('XSRF-TOKEN='))?.split('=')[1];
  const conCredenciales = req.clone({ withCredentials: true,
    setHeaders: token && req.method !== 'GET' ? { 'X-XSRF-TOKEN': token } : {} });
  return next(conCredenciales);
};
```

**Tip:** Swagger publica el contrato en `/v3/api-docs`. `web-client/` genera los tipos desde ahí con
`npm run api:types` (`openapi-typescript`) en vez de escribirlos a mano; los montos viajan como **texto** (`"5000.00"`) y
los tipos generados ya lo reflejan.

## 7. C++

El cliente de escritorio no corre en un navegador, así que no hay CORS. Necesitas hacer tres cosas con la
biblioteca HTTP que elijas (Qt `QNetworkAccessManager`, libcurl, WinHTTP…):

1. **Activar el almacén de cookies** para que guarde y reenvíe `JSESSIONID` y `XSRF-TOKEN` solo.
   Qt lo hace con `QNetworkCookieJar`; libcurl con `CURLOPT_COOKIEFILE` vacío.
2. **Leer la cookie `XSRF-TOKEN`** después del login y enviarla como encabezado `X-XSRF-TOKEN` en `POST`/`PATCH`.
3. **Enviar `Content-Type: application/json`** y leer las respuestas como JSON (Qt: `QJsonDocument`).

```cpp
// Ejemplo con Qt: escritura con token CSRF
QNetworkRequest req(QUrl("http://localhost:8080/api/productos"));
req.setHeader(QNetworkRequest::ContentTypeHeader, "application/json");
for (const QNetworkCookie& c : manager.cookieJar()->cookiesForUrl(req.url()))
    if (c.name() == "XSRF-TOKEN") req.setRawHeader("X-XSRF-TOKEN", c.value());
manager.post(req, QJsonDocument(cuerpo).toJson());
```

## 8. Qué pantalla llama a qué

Cada cliente debe consumir **al menos dos funcionalidades reales** (lo pide la actividad). Sugerencia:

### Web (Angular): Ventas y Entregas
| Pantalla | Llamadas, en orden |
|---|---|
| Login | `POST /api/auth/login` → según `rol` mostrar Ventas o Entregas. `GET /api/auth/me` al recargar. |
| **Vender** (VENDEDOR) | `GET /api/productos` → `GET /api/clientes` → `POST /api/ventas` |
| Mis ventas / cancelar | `GET /api/ventas/mis-ventas` → `POST /api/ventas/{id}/cancelacion` |
| Clientes | `GET /api/terceros?tipoDocumento=..&numeroDocumento=..` → `POST /api/terceros` (si no existe) → `POST /api/clientes` |
| **Despachos** (ENTREGAS) | `GET /api/ventas/pendientes-entrega` → `POST /api/ventas/{id}/despacho` |

### Escritorio (C++): Recursos Humanos y Bodega
| Pantalla | Llamadas, en orden |
|---|---|
| Login | `POST /api/auth/login` → según `rol` mostrar RH o Bodega |
| **Cuentas** (RH) | `GET /api/terceros?...` → `POST /api/terceros` → `POST /api/usuarios` → `GET /api/usuarios` |
| **Avales** (RH) | `GET /api/vendedores` → `POST /api/vendedores/{id}/aval/aprobar` (o `/rechazar`) |
| **Inventario** (Bodega) | `GET /api/productos` · `GET /api/inventario/bajo-stock` |
| Mantenimiento (Bodega) | `POST /api/productos` · `POST /api/proveedores` · `POST /api/proveedores/{id}/productos` · `POST /api/inventario/entradas` · `POST /api/inventario/ajustes` |

Después de vender, cancelar o despachar, **vuelve a pedir la lista**: no asumas que el cambio ocurrió, usa la respuesta.

## 9. Errores frecuentes y qué mostrar

| Código | HTTP | Cuándo | Qué hacer en pantalla |
|---|---|---|---|
| `NO_AUTENTICADO` | 401 | No hay sesión o expiró | Ir al login |
| `CREDENCIALES_INVALIDAS` | 401 | Login incorrecto | «Correo o contraseña incorrectos» |
| `CUENTA_DESACTIVADA` | 401 | RH desactivó la cuenta | Ir al login con aviso |
| `SIN_PERMISO` | 403 | El rol no puede hacer eso, **o falta el token CSRF** | Ocultar la opción; revisar el encabezado |
| `VENDEDOR_SIN_AVAL` | 403 | El vendedor no tiene aval vigente | «Tu aval no está aprobado» |
| `DATOS_INVALIDOS` | 400 | Falta un campo o es demasiado largo | Mostrar el `mensaje` junto al campo |
| `NO_ENCONTRADO` | 404 | El id no existe | «No se encontró» y refrescar |
| `STOCK_INSUFICIENTE` | 409 | No hay unidades disponibles | Mostrar el `mensaje` (dice cuántas hay) |
| `ESTADO_INVALIDO` | 409 | Ya se despachó o canceló | Refrescar la lista |
| `*_DUPLICADO`, `*_EXISTENTE` | 409 | Ya existe ese documento, correo o código | Ofrecer usar el existente |

Lista completa en [api-rest.md](../reference/api-rest.md#errores).
