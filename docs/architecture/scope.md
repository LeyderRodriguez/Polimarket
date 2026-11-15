# Alcance del backend

Qué hace el backend de PoliMarket, qué no hace y cómo se relaciona con lo que pide la actividad.

## Qué es

Una aplicación **Java 25 con Spring Boot** (un solo proyecto Maven) organizada en **siete componentes de negocio**.
Cada componente publica una interfaz y oculta su implementación; se comunican solo por esas interfaces. Una
**fachada** recibe las peticiones HTTP de los clientes y las delega. Los clientes (Angular y C++) solo hablan con la
API REST: nunca con la base de datos.

```
Cliente web (Angular) ──┐
                        ├── HTTP/JSON ──► Controllers ──► FachadaPoliMarket ──► Componentes ──► PostgreSQL
Cliente C++ ────────────┘                                                       (un esquema cada uno)
```

## Los componentes

Cada uno vive en su paquete `polimarket.<componente>`: la subcarpeta `api` es lo público (interfaz y DTO) y `dominio`
y `aplicacion` son internos.

| Componente | Responsabilidad | Usa (por su interfaz) |
|---|---|---|
| **Seguridad** | Cuentas, inicio de sesión, roles, sesiones | — |
| **Terceros** | Datos comunes de personas y empresas, sin duplicados | — |
| **Recursos Humanos** | Empleados, vendedores y el aval (aprobar, rechazar, consultar) | Terceros |
| **Proveedores** | Proveedores y los productos que suministran | Terceros |
| **Bodega** | Productos, existencias, entradas, ajustes, reservas, bajo stock | Proveedores |
| **Ventas** | Clientes y ventas (registrar, anular) | Recursos Humanos, Bodega, Terceros |
| **Entregas** | Ventas pendientes y despacho | Ventas, Bodega |

Seguridad y Terceros no dependen de nadie: se podrían usar en otro sistema. No hay ciclos entre componentes.
La fachada (`polimarket.fachada`) no tiene reglas de negocio de ningún componente: delega y coordina operaciones que
cruzan varios, como crear una cuenta.

## Lo que está implementado

### Los requisitos de la actividad

| Id | Requisito | Qué lo hace posible |
|---|---|---|
| RSI-1 | Aprobar el aval de un vendedor y consultar si está autorizado | Recursos Humanos, Seguridad, Terceros |
| RSI-2 | Consultar la disponibilidad de productos | Bodega, Seguridad, Recursos Humanos |
| RSI-3 | Registrar una venta validando autorización y disponibilidad | Ventas, Recursos Humanos, Bodega, Seguridad |
| RSI-4 | Consultar productos bajo stock y sus proveedores | Bodega, Proveedores, Terceros, Seguridad |
| RSI-5 | Despachar una entrega y registrar la salida de inventario | Entregas, Ventas, Bodega, Seguridad |
| RSI-6 | Cancelar una venta propia pendiente y liberar su reserva (se añadió) | Ventas, Bodega, Seguridad |

Cada requisito usa al menos dos componentes de negocio, de modo que se cumple «mínimo dos componentes por requisito»
tanto si se lee sobre las dos capas (cliente y negocio) como solo sobre el negocio.

### Operaciones que los complementan
- **Cuentas (RH):** crear con rol, listar, cambiar el correo, desactivar con cierre inmediato de sesión.
- **Terceros:** buscar por documento y registrar.
- **Bodega:** crear y actualizar productos, registrar entradas de proveedores, ajustes con motivo.
- **Proveedores:** registrar, actualizar, asociar productos.
- **Clientes:** registrar y actualizar con preferencias.

## Roles

Cada cuenta tiene **un solo rol**: `RECURSOS_HUMANOS`, `VENDEDOR`, `BODEGA` o `ENTREGAS`. La matriz de quién puede
qué está en [api-rest.md](../reference/api-rest.md#quién-puede-qué). Los permisos se comprueban **en el backend**: lo que el cliente
oculte o muestre es solo comodidad.

## Reglas de negocio

- **Disponible = stock físico − unidades reservadas.** El bajo stock se mide sobre lo disponible, no sobre el físico.
- Al registrar una venta se **reservan** los productos; al despachar se **descuentan** del físico y se consume la reserva.
- Una venta con varios productos **no deja reservas parciales** si alguno falla.
- No se despacha ni se cancela dos veces.
- Un vendedor con aval vigente puede vender y gestionar clientes. Si pierde el aval no puede crear ventas nuevas, pero
  **sí anular las suyas pendientes**.
- Entregas despacha una venta ya aceptada aunque el vendedor haya perdido el aval después.
- Si se desactiva la cuenta de un vendedor, sus ventas pendientes siguen pendientes y Entregas puede despacharlas.
- Un ajuste de inventario exige motivo y no puede dejar el físico por debajo de lo reservado.
- Crear la cuenta de un vendedor **no** aprueba su aval: es un paso aparte de RH.
- RH no crea otras cuentas de RH, no cambia roles y no desactiva su propia cuenta.
- La identidad del vendedor y del responsable sale de la sesión, nunca de un dato enviado por el cliente. Los precios
  los calcula el servidor.
- Las contraseñas se guardan solo con hash.

## Lo que no incluye

| Fuera de alcance | Por qué |
|---|---|
| El cliente de escritorio (C++) | Lo desarrolla el resto del grupo; el contrato está en esta carpeta |
| Cambio de contraseña (RF-25) | No está entre los requisitos seleccionados |
| Estados de entrega `ENTREGADA` y `FALLIDA` | Se registra el despacho; confirmar la recepción es una ampliación |
| Varias bodegas | El prototipo usa una sola |
| Borrar datos | Se desactiva o se anula; no se borra |
| Paginación en los listados | El volumen del prototipo no lo requiere |
| HTTPS, límite de intentos de login | Es un sistema de desarrollo local |

El cliente web (Angular) sí está hecho: vive en [`web-client/`](../../web-client/) y consume Disponibilidad, Clientes,
Nueva venta, Mis ventas y Entregas (RSI-2, RSI-3, RSI-5 y RSI-6). Su plan está en [web-client-plan.md](web-client-plan.md).

## Cambios frente al documento del grupo

La implementación ajusta algunos puntos del Word; hay que acordarlos con el grupo y reflejarlos en los diagramas
(`docs/architecture/diagrams/`):

| Tema | Word | Implementación |
|---|---|---|
| Módulos Maven | Uno por componente | Un proyecto con un paquete por componente |
| Base de datos | Una por área | Una base con un esquema por componente |
| Estados de venta | REGISTRADA, PENDIENTE_ENTREGA, ENTREGADA, ANULADA | PENDIENTE_ENTREGA, DESPACHADA, ANULADA |
| Cuenta | `Usuario` sin rol | `Usuario` con `rol` |
| Reservas | No existían | `ReservaInventario` y `DetalleReserva` |
| Movimientos | ENTRADA, SALIDA | ENTRADA, SALIDA, AJUSTE; con `idUsuario` en vez del nombre |
| Clientes de la actividad | Diagrama: web en Java | Web en Angular (Ventas y Entregas), escritorio en C++ (Bodega y RH) |

## Cómo se comprueba que funciona

- **75 pruebas automáticas** en unos 25 segundos (`cd backend && ./mvnw test`): reglas puras, API completa y pruebas con
  hilos reales que comprueban la concurrencia. Cada una corre sobre PostgreSQL real en un contenedor desechable.
- **Recorridos manuales** con `curl` en `scripts/`.
- **Swagger UI** en `http://localhost:8080/swagger-ui.html`.
