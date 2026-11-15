# Base de datos y esquemas

Qué necesita el backend para funcionar y cómo está organizada su base de datos.

## Qué necesita para arrancar

| Necesidad | Detalle |
|---|---|
| **PostgreSQL 17** | Una sola base llamada `polimarket`. Con `docker compose` ya viene resuelta. |
| Usuario y clave | `polimarket` / `polimarket` por defecto, solo para desarrollo. |
| Java 25 | Solo si se ejecuta con `./mvnw`. Con Docker no hace falta. |

El backend se conecta con estas variables de entorno (si no existen, usa el valor por defecto):

| Variable | Por defecto |
|---|---|
| `POLIMARKET_DB_URL` | `jdbc:postgresql://localhost:5432/polimarket` |
| `POLIMARKET_DB_USER` | `polimarket` |
| `POLIMARKET_DB_PASSWORD` | `polimarket` |

**No hay que crear tablas a mano.** Al arrancar, Flyway aplica las migraciones pendientes (`backend/src/main/resources/db/migration/`)
y deja registro de cuáles ya corrieron. Hibernate solo **valida** que las clases coincidan con las tablas (`ddl-auto: validate`).
Si una tabla cambia, se agrega una migración nueva; las anteriores no se editan.

## Un esquema por componente

La base es una sola, pero cada componente tiene su propio esquema (una «carpeta» de tablas). Un componente solo toca
sus tablas. Para referirse a algo de otro componente guarda su **identificador**, sin llave foránea entre esquemas:
así los componentes siguen siendo independientes, aunque compartan base.

```
seguridad ───────── usuario                          tercero_id ─────────────┐
terceros ────────── tercero, rol_tercero             ◄───────────────────────┤ (por id)
recursos_humanos ── empleado, vendedor, aval_rh      tercero_id ─────────────┤
proveedores ─────── proveedor, producto_suministrado tercero_id; codigo_producto (no id)
bodega ──────────── bodega, producto, existencia, compra, detalle_compra,
                    movimiento_inventario, reserva_inventario, detalle_reserva
                                                     proveedor_id, venta_id, usuario_id (por id)
ventas ──────────── cliente, venta, detalle_venta    tercero_id, vendedor_id, producto_id (por id)
entregas ────────── entrega                          venta_id (por id)
```

Las llaves foráneas existen **solo dentro de un esquema** (por ejemplo `ventas.detalle_venta → ventas.venta`).

## Tablas

### `terceros`: personas y empresas, una sola vez
| Tabla | Columnas | Reglas |
|---|---|---|
| `tercero` | `id`, `tipo` (NATURAL/JURIDICA), `tipo_documento`, `numero_documento`, `nombres`, `apellidos`, `razon_social`, `telefono`, `correo`, `direccion` | Único por (`tipo_documento`, `numero_documento`) |
| `rol_tercero` | `id`, `tercero_id` → `tercero`, `tipo_rol` (EMPLEADO/VENDEDOR/CLIENTE/PROVEEDOR), `fecha_asignacion`, `activo` | Único por (`tercero_id`, `tipo_rol`): un tercero puede ser cliente y proveedor a la vez |

### `seguridad`: cuentas de acceso
| Tabla | Columnas | Reglas |
|---|---|---|
| `usuario` | `id`, `tercero_id`, `correo`, `contrasena_hash`, `rol` (RECURSOS_HUMANOS/VENDEDOR/BODEGA/ENTREGAS), `activo`, `fecha_creacion` | Un tercero, una cuenta. Correo único sin distinguir mayúsculas. La clave solo se guarda con **hash BCrypt** |

### `recursos_humanos`: personal y aval
| Tabla | Columnas | Reglas |
|---|---|---|
| `empleado` | `id`, `tercero_id`, `cargo`, `fecha_ingreso`, `estado` | Un empleado por tercero |
| `vendedor` | `id`, `empleado_id` → `empleado`, `zona` | El código `VEN-0001` se deriva del id |
| `aval_rh` | `id`, `vendedor_id` → `vendedor`, `estado` (PENDIENTE/APROBADO/RECHAZADO), `fecha_solicitud`, `fecha_respuesta`, `responsable_id` → `empleado`, `observacion` | Un vendedor acumula varios con el tiempo. **El vigente es el último** |

### `proveedores`
| Tabla | Columnas | Reglas |
|---|---|---|
| `proveedor` | `id`, `tercero_id`, `nombre_contacto`, `telefono_contacto` | Un proveedor por tercero |
| `producto_suministrado` | `id`, `proveedor_id` → `proveedor`, `codigo_producto`, `precio_compra` | Único por (`proveedor_id`, `codigo_producto`). Referencia el producto **por código**, no por id, para no depender de Bodega |

### `bodega`: productos, existencias, movimientos y reservas
| Tabla | Columnas | Reglas |
|---|---|---|
| `bodega` | `id`, `nombre`, `ubicacion` | Una sola fila («Bodega principal») |
| `producto` | `id`, `codigo`, `nombre`, `descripcion`, `categoria`, `precio_venta`, `activo` | Código único; precio ≥ 0 |
| `existencia` | `id`, `bodega_id`, `producto_id`, `cantidad` (**stock físico**), `stock_minimo` | Único por (bodega, producto); `cantidad` ≥ 0 |
| `compra` / `detalle_compra` | Compra a un proveedor que origina una entrada | `proveedor_id` por id |
| `movimiento_inventario` | `id`, `producto_id`, `fecha`, `tipo` (ENTRADA/SALIDA/AJUSTE), `cantidad`, `cantidad_anterior`, `cantidad_nueva`, `motivo`, `referencia`, `usuario_id`, `compra_id` | **Trazabilidad**: quién, cuándo y de qué valor a cuál |
| `reserva_inventario` | `id`, `venta_id`, `fecha`, `estado` (ACTIVA/CONSUMIDA/LIBERADA) | Única por venta |
| `detalle_reserva` | `id`, `reserva_id` → `reserva_inventario`, `producto_id`, `cantidad` | Cantidad > 0 |

**Disponible = `existencia.cantidad` − suma de `detalle_reserva.cantidad` de reservas ACTIVAS.** No se guarda: se calcula.

### `ventas`
| Tabla | Columnas | Reglas |
|---|---|---|
| `cliente` | `id`, `tercero_id`, `fecha_registro`, `preferencias` | Un cliente por tercero |
| `venta` | `id`, `cliente_id` → `cliente`, `vendedor_id`, `fecha`, `direccion_entrega`, `total`, `estado` (PENDIENTE_ENTREGA/DESPACHADA/ANULADA) | El total lo calcula el servidor |
| `detalle_venta` | `id`, `venta_id` → `venta`, `producto_id`, `cantidad`, `precio_unitario` | El precio queda congelado al vender |

### `entregas`
| Tabla | Columnas | Reglas |
|---|---|---|
| `entrega` | `id`, `venta_id`, `fecha_despacho`, `direccion`, `receptor`, `estado`, `responsable_id` | **`venta_id` único**: una venta se despacha una sola vez, aunque lleguen dos solicitudes a la vez |

## Migraciones

| Versión | Archivo | Qué hace |
|---|---|---|
| V1 | `esquemas_por_componente` | Crea los siete esquemas |
| V2 | `terceros` | Tablas de terceros y sus roles |
| V3 | `seguridad` | Tabla de cuentas |
| V4 | `recursos_humanos` | Empleados, vendedores y avales |
| V5 | `proveedores` | Proveedores y los productos que suministran |
| V6 | `bodega` | Productos, existencias, compras, movimientos y reservas; inserta la bodega única |
| V7 | `ventas` | Clientes, ventas y detalle |
| V8 | `entregas` | Entregas (con `venta_id` único) |
| V9 | `reserva_unica_por_venta` | Índice único: una reserva por venta |

## Cómo se evita vender dos veces las mismas unidades

Es la parte delicada del sistema. Se resuelve con **transacciones** y **bloqueos de fila** de PostgreSQL
(`SELECT … FOR UPDATE`): dos operaciones sobre la misma fila se atienden una tras otra, nunca a la vez.

| Operación | Qué bloquea | Por qué |
|---|---|---|
| Reservar (vender) | Las filas de `existencia` de los productos, **siempre en orden de id** | Dos ventas del mismo producto no pueden leer el mismo disponible. El orden fijo evita que dos ventas se bloqueen entre sí |
| Anular / despachar | Primero la `venta`, luego su `reserva_inventario`, luego las `existencia` | Si se anula y se despacha a la vez, solo una gana |
| Resolver un aval | La fila del `vendedor` | Dos RH no pueden pisarse |

Una venta se registra **completa o no se registra**: si un producto no alcanza, la transacción entera se revierte y no
queda ni la venta ni reservas parciales. Esto está probado con hilos reales (`ConcurrenciaTest`): 20 ventas simultáneas
de una unidad sobre 10 en bodega dejan exactamente 10 reservas, y 5 despachos simultáneos de la misma venta dejan uno solo.

## Datos de demostración

Al arrancar por primera vez, `CargadorDatosDemo` carga una cuenta por rol, dos vendedores (uno con aval aprobado y otro
pendiente), un proveedor, tres productos con existencias (el azúcar queda bajo su mínimo) y dos clientes. Si la cuenta
inicial de RH ya existe, no hace nada: reiniciar no duplica ni pisa datos. Para arrancar con la base vacía:
`POLIMARKET_DEMO_CARGAR_DATOS=false`.

Para volver al estado inicial: `docker compose down -v` (borra el volumen de la base) y levantar de nuevo.
