# API REST de PoliMarket

Referencia de los endpoints del backend. La misma información, con ejemplos para probar, está en **Swagger UI**
(`http://localhost:8080/swagger-ui.html`) y el contrato en JSON en `/v3/api-docs`. Si algo difiere, manda el código.

Para empezar rápido (sesión, CSRF, ejemplos en Angular y C++) lee [guides/clients.md](../guides/clients.md).

## Convenciones

| Tema | Regla |
|---|---|
| Base | `http://localhost:8080`. Todo cuelga de `/api`. |
| Formato | JSON en la solicitud (`Content-Type: application/json`) y en la respuesta. |
| Sesión | Cookie `JSESSIONID` del servidor (30 min sin actividad). Se crea en el login. |
| CSRF | Los `POST`/`PATCH` necesitan el encabezado `X-XSRF-TOKEN` con el valor de la cookie `XSRF-TOKEN`. Sin él, `403`. |
| Montos | Texto con dos decimales: `"5000.00"`. |
| Identidad | El vendedor o responsable sale de la **sesión**. Nunca se envía su id. |
| Cuerpos extra | Los campos que no existen en el contrato se ignoran (por ejemplo un `precio` en una venta). |
| Errores | Siempre `{ "codigo": "...", "mensaje": "..." }`. Ver [Errores](#errores). |
| Borrado | No hay `DELETE`. Las cuentas se desactivan y las ventas se anulan. Un `DELETE` responde `405`. |

## Quién puede qué

| Endpoint | RH | Vendedor con aval | Vendedor sin aval | Bodega | Entregas |
|---|:-:|:-:|:-:|:-:|:-:|
| Sesión (`/api/auth/*`) | ✓ | ✓ | ✓ | ✓ | ✓ |
| Cuentas (`/api/usuarios`) | ✓ | | | | |
| Vendedores y aval (`/api/vendedores`) | ✓ | solo su aval | solo su aval | | |
| Terceros (`/api/terceros`) | ✓ | ✓ (sin datos de contacto) | | | |
| Productos: ver | | ✓ | | ✓ | |
| Productos, proveedores, inventario: escribir | | | | ✓ | |
| Clientes (`/api/clientes`) | | ✓ | | | |
| Registrar venta | | ✓ | | | |
| Mis ventas y anular las propias | | ✓ | ✓ | | |
| Ventas pendientes y despachar | | | | | ✓ |

## Sesión

| Método y ruta | Qué hace | Cuerpo | Respuesta |
|---|---|---|---|
| `POST /api/auth/login` | Inicia sesión | `{ "correo", "contrasena" }` | `200 { "id", "nombre", "rol" }` o `401 CREDENCIALES_INVALIDAS` |
| `GET /api/auth/me` | Quién está autenticado | — | `200 { "id", "nombre", "rol" }` |
| `POST /api/auth/logout` | Cierra la sesión | — | `204` |

`rol` es uno de `RECURSOS_HUMANOS`, `VENDEDOR`, `BODEGA`, `ENTREGAS`.

## Terceros (RH y vendedor con aval)

Un tercero es una persona o empresa con sus datos comunes. Se busca antes de crear, para no duplicar.

| Método y ruta | Qué hace | Respuesta |
|---|---|---|
| `GET /api/terceros?tipoDocumento=CC&numeroDocumento=1001` | Busca por documento | `200` tercero o `404 NO_ENCONTRADO` |
| `POST /api/terceros` | Registra un tercero | `201` tercero, `409 DOCUMENTO_DUPLICADO` |

```json
POST /api/terceros
{ "tipo": "NATURAL", "tipoDocumento": "CC", "numeroDocumento": "1001", "nombres": "Ana", "apellidos": "Pérez",
  "telefono": "3000000000", "correo": "ana@ejemplo.test", "direccion": "Calle 1" }

201 { "id": 21, "tipo": "NATURAL", "tipoDocumento": "CC", "numeroDocumento": "1001", "nombres": "Ana",
      "apellidos": "Pérez", "razonSocial": null, "telefono": "3000000000", "correo": "ana@ejemplo.test", "direccion": "Calle 1" }
```
`tipo` es `NATURAL` (requiere `nombres` y `apellidos`) o `JURIDICA` (requiere `razonSocial`). Un vendedor recibe el
tercero **sin** `telefono`, `correo` ni `direccion`: solo le sirve para saber si ya existe.

## Cuentas (solo RH)

| Método y ruta | Qué hace | Respuesta |
|---|---|---|
| `POST /api/usuarios` | Crea una cuenta para un tercero existente | `201` cuenta, `409 CORREO_DUPLICADO` / `TERCERO_CON_CUENTA` |
| `GET /api/usuarios` | Lista las cuentas (sin contraseña) | `200 [ { "id", "terceroId", "nombre", "correo", "rol", "activo" } ]` |
| `PATCH /api/usuarios/{id}` | Cambia el correo | `200` cuenta. El rol no se puede cambiar |
| `POST /api/usuarios/{id}/desactivacion` | Desactiva la cuenta y cierra sus sesiones | `200` cuenta, `403` si es la propia, `409` si ya estaba desactivada |

```json
POST /api/usuarios
{ "terceroId": 21, "correo": "ana@ejemplo.test", "contrasenaInicial": "mínimo 8 caracteres", "rol": "VENDEDOR" }

201 { "id": 11, "terceroId": 21, "correo": "ana@ejemplo.test", "rol": "VENDEDOR", "activo": true,
      "vendedorId": 1, "estadoAval": "PENDIENTE" }
```
RH solo asigna `VENDEDOR`, `BODEGA` o `ENTREGAS`. Crear la cuenta de un vendedor lo registra con aval `PENDIENTE`;
**no lo aprueba**: es un paso aparte. Para cambiar el rol de alguien se desactiva su cuenta y se crea otra.

## Vendedores y aval

| Método y ruta | Quién | Qué hace |
|---|---|---|
| `GET /api/vendedores` | RH | Lista vendedores: `[ { "id", "terceroId", "nombre", "codigo", "zona", "estadoAval" } ]` |
| `GET /api/vendedores/{id}/autorizacion` | RH; el vendedor solo el suyo | `200 { "vendedorId", "estadoAval", "autorizado" }` |
| `POST /api/vendedores/{id}/aval/aprobar` | RH | Aprueba. Cuerpo opcional `{ "observacion" }` |
| `POST /api/vendedores/{id}/aval/rechazar` | RH | Rechaza o **revoca** un aval aprobado |

`estadoAval` es `PENDIENTE`, `APROBADO` o `RECHAZADO`. Aprobar un aval ya aprobado da `409 ESTADO_INVALIDO`.
Si se revoca, el vendedor no puede crear ventas nuevas, pero sí anular las suyas pendientes.

## Productos y disponibilidad

**Disponible = stock físico − unidades reservadas por ventas pendientes.**

| Método y ruta | Quién | Qué hace |
|---|---|---|
| `GET /api/productos` | Bodega, vendedor con aval | Lista con existencias |
| `POST /api/productos` | Bodega | Crea un producto (`409 CODIGO_DUPLICADO`) |
| `PATCH /api/productos/{id}` | Bodega | Cambia solo los campos enviados |

```json
GET /api/productos
200 [ { "id": 1, "codigo": "ARR-01", "nombre": "Arroz", "precio": "5000.00",
        "stockFisico": 10, "cantidadReservada": 6, "cantidadDisponible": 4 } ]

POST /api/productos
{ "codigo": "ARR-01", "nombre": "Arroz", "descripcion": "Paquete", "categoria": "Granos", "precioVenta": "5000.00", "stockMinimo": 5 }
201 { "id": 1, "codigo": "ARR-01", "nombre": "Arroz", "descripcion": "Paquete", "categoria": "Granos",
      "precioVenta": "5000.00", "stockMinimo": 5 }
```
El producto nace con existencia 0; el stock entra con una entrada de inventario.

## Proveedores (Bodega)

| Método y ruta | Qué hace |
|---|---|
| `GET /api/proveedores` | Lista: `[ { "id", "terceroId", "nombre", "nombreContacto", "telefonoContacto" } ]` |
| `POST /api/proveedores` | Registra un tercero (normalmente una empresa) como proveedor: `{ "terceroId", "nombreContacto", "telefonoContacto" }`. `409 PROVEEDOR_EXISTENTE` |
| `PATCH /api/proveedores/{id}` | Cambia los datos de contacto |
| `POST /api/proveedores/{id}/productos` | Asocia un producto: `{ "productoId" }` → `201 { "proveedorId", "productoId" }`. `409 PRODUCTO_YA_ASOCIADO` |

## Inventario (Bodega)

| Método y ruta | Qué hace |
|---|---|
| `GET /api/inventario/bajo-stock` | Productos cuyo **disponible** está por debajo del mínimo, con sus proveedores |
| `POST /api/inventario/entradas` | Ingreso de mercancía de un proveedor |
| `POST /api/inventario/ajustes` | Corrige el stock físico con motivo |

```json
GET /api/inventario/bajo-stock
200 [ { "productoId": 1, "nombre": "Arroz", "stockFisico": 10, "cantidadReservada": 6, "cantidadDisponible": 4,
        "stockMinimo": 5, "proveedores": [ { "id": 1, "nombre": "Distribuidora Ejemplo" } ] } ]

POST /api/inventario/entradas
{ "productoId": 1, "cantidad": 10, "proveedorId": 1, "referencia": "FAC-001", "costoUnitario": "3000.00" }
201 { "movimientoId": 1, "productoId": 1, "tipo": "ENTRADA", "cantidad": 10, "stockAnterior": 0, "stockFisico": 10 }

POST /api/inventario/ajustes
{ "productoId": 1, "nuevoStockFisico": 9, "motivo": "Corrección por conteo" }
201 { "movimientoId": 2, "productoId": 1, "tipo": "AJUSTE", "cantidad": 1, "stockAnterior": 10, "stockFisico": 9 }
```
Un ajuste no puede dejar el stock físico por debajo de lo reservado (`409 STOCK_INSUFICIENTE`) ni negativo. Todo
movimiento guarda quién lo hizo, cuándo y de qué valor a cuál.

## Clientes (vendedor con aval)

Un cliente es un tercero con rol de cliente. Flujo: buscar el tercero por documento; si no existe, registrarlo; luego
registrarlo como cliente.

| Método y ruta | Qué hace |
|---|---|
| `GET /api/clientes` | Lista: `[ { "id", "terceroId", "nombre", "preferencias": [] } ]` |
| `POST /api/clientes` | `{ "terceroId", "preferencias": ["sin gluten"] }` → `201`. `409 CLIENTE_EXISTENTE` |
| `PATCH /api/clientes/{id}` | `{ "preferencias", "telefono", "correo", "direccion" }`, todos opcionales |

Una preferencia no puede contener `;`.

## Ventas y entregas

| Método y ruta | Quién | Qué hace |
|---|---|---|
| `POST /api/ventas` | Vendedor con aval | Registra la venta y **reserva** el stock, todo o nada |
| `GET /api/ventas/mis-ventas` | Vendedor | Sus ventas |
| `POST /api/ventas/{id}/cancelacion` | Vendedor dueño | Anula una venta pendiente y libera la reserva |
| `GET /api/ventas/pendientes-entrega` | Entregas | Qué entregar, a quién y dónde |
| `POST /api/ventas/{id}/despacho` | Entregas | Despacha una sola vez y descuenta el stock |

```json
POST /api/ventas
{ "clienteId": 1, "direccionEntrega": "Calle 10 # 20-30", "productos": [ { "productoId": 1, "cantidad": 6 } ] }
201 { "ventaId": 1, "estado": "PENDIENTE_ENTREGA", "total": "30000.00", "reservaEstado": "ACTIVA" }

GET /api/ventas/mis-ventas
200 [ { "ventaId": 1, "cliente": "María Pérez", "estado": "PENDIENTE_ENTREGA", "total": "30000.00" } ]

POST /api/ventas/1/cancelacion
200 { "ventaId": 1, "estado": "ANULADA", "total": "30000.00", "reservaEstado": "LIBERADA" }

GET /api/ventas/pendientes-entrega
200 [ { "ventaId": 2, "cliente": "María Pérez", "direccionEntrega": "Calle 10 # 20-30",
        "productos": [ { "productoId": 1, "nombre": "Arroz", "cantidad": 6 } ] } ]

POST /api/ventas/2/despacho
200 { "entregaId": 1, "ventaId": 2, "estado": "DESPACHADA", "reservaEstado": "CONSUMIDA" }
```

Estados de la venta: `PENDIENTE_ENTREGA` → `DESPACHADA`, o `PENDIENTE_ENTREGA` → `ANULADA`. Una venta despachada o
anulada ya no cambia: repetir la acción da `409 ESTADO_INVALIDO` y no toca el stock.

Con 10 unidades en bodega, una venta de 6 deja `físico 10 · reservado 6 · disponible 4`. Si se anula, vuelve a
`10 · 0 · 10`. Si se despacha, queda `4 · 0 · 4`. Una venta con varios productos donde uno no alcanza **no deja
reservas parciales** (`409 STOCK_INSUFICIENTE`).

Entregas puede despachar una venta ya aceptada aunque el vendedor haya perdido el aval después.

## Errores

Todos tienen la forma `{ "codigo": "...", "mensaje": "..." }`.

| HTTP | Código | Cuándo |
|---|---|---|
| 400 | `DATOS_INVALIDOS` | Falta un campo, un texto es demasiado largo, un valor no es válido |
| 401 | `NO_AUTENTICADO` | No hay sesión o expiró |
| 401 | `CREDENCIALES_INVALIDAS` | Correo o contraseña incorrectos |
| 401 | `CUENTA_DESACTIVADA` | La cuenta fue desactivada con la sesión abierta |
| 403 | `SIN_PERMISO` | El rol no puede hacerlo, la venta es de otro vendedor, o falta el token CSRF |
| 403 | `VENDEDOR_SIN_AVAL` | El vendedor no tiene aval vigente |
| 404 | `NO_ENCONTRADO` | El id o la ruta no existen |
| 405 | `METODO_NO_PERMITIDO` | Por ejemplo, un `DELETE` |
| 409 | `STOCK_INSUFICIENTE` | No alcanzan las unidades disponibles (el mensaje dice cuántas hay) |
| 409 | `ESTADO_INVALIDO` | Ya se despachó, anuló, aprobó o desactivó |
| 409 | `DOCUMENTO_DUPLICADO`, `CORREO_DUPLICADO`, `CODIGO_DUPLICADO` | Ya existe ese dato único |
| 409 | `TERCERO_CON_CUENTA`, `CLIENTE_EXISTENTE`, `PROVEEDOR_EXISTENTE`, `VENDEDOR_EXISTENTE`, `PRODUCTO_YA_ASOCIADO`, `RESERVA_EXISTENTE` | Ya estaba registrado |
| 415 | `FORMATO_NO_SOPORTADO` | No se envió JSON |
| 500 | `ERROR_INTERNO` | Fallo inesperado del servidor |
