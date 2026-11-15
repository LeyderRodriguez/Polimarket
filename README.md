# PoliMarket

Actividad de Temas avanzados en diseño de software (Unidad 2: reutilización de software y desarrollo basado en componentes).

Backend en Java con Spring Boot organizado por componentes de negocio (Seguridad, Terceros, Recursos Humanos, Ventas, Bodega, Proveedores y Entregas), con una fachada y una API HTTP que consumen dos clientes.

## Estructura

| Carpeta | Contenido |
|---|---|
| `backend/` | Java 25, Spring Boot 4.1.2, Maven. Un paquete por componente. |
| `web-client/` | Cliente Angular (Ventas y Entregas). Estructura base y contrato de la API. |
| `desktop-client/` | Cliente C++ (Bodega y Recursos Humanos). Estructura base y contrato de la API. |
| `docs/` | Propuesta de implementación y diagramas PlantUML. |

## Cómo correr el backend

```bash
docker compose up -d
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Las pruebas usan su propia base PostgreSQL en un contenedor:

```bash
cd backend
./mvnw test
```

## Cuentas de demostración (perfil `dev`)

Con `-Dspring-boot.run.profiles=dev` se cargan datos ficticios, una sola vez. La clave ficticia está en
`backend/src/main/resources/application-dev.yml`; es solo para desarrollo.

| Correo | Rol | Notas |
|---|---|---|
| `rh@polimarket.test` | Recursos Humanos | Crea cuentas y aprueba avales |
| `bodega@polimarket.test` | Bodega | Productos, proveedores, entradas y ajustes |
| `entregas@polimarket.test` | Entregas | Ve ventas pendientes y las despacha |
| `ana@polimarket.test` | Vendedor | Aval aprobado: puede vender |
| `luis@polimarket.test` | Vendedor | Aval pendiente: no puede vender |

## Probar a mano

Con el backend en marcha y la base recién creada (`docker compose down -v && docker compose up -d`):

```bash
bash docs/probar-api.sh          # RSI-1: cuentas y aval
bash docs/probar-flujo-venta.sh  # RSI-2 a RSI-6: vender, reservar, anular y despachar
```

## Documentación

- [Propuesta de implementación](docs/propuesta-polimarket.html): arquitectura, API, reglas y decisiones.
- `docs/diagramas/`: código PlantUML de los diagramas de clases y de componentes.
