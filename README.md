# PoliMarket

Actividad de Temas avanzados en diseño de software (Unidad 2: reutilización de software y desarrollo basado en componentes).

Backend en Java con Spring Boot organizado por componentes de negocio (Seguridad, Terceros, Recursos Humanos, Ventas, Bodega, Proveedores y Entregas), con una fachada y una API HTTP que consumen dos clientes.

## Estructura

| Carpeta | Contenido |
|---|---|
| `backend/` | Java 25, Spring Boot 4.1.2, Maven. Un paquete por componente. |
| `web-client/` | Cliente Angular (Ventas y Entregas). |
| `desktop-client/` | Cliente C++ / Qt 6 (Bodega y Recursos Humanos). |
| `docs/` | Guía para los clientes, API REST, alcance, esquemas de la base y diagramas PlantUML. |
| `scripts/` | Recorridos manuales con `curl` para probar la API en marcha. |

## Requisitos previos

| Herramienta | Versión | Para qué |
|---|---|---|
| Git | reciente | Clonar el repositorio |
| Docker Desktop | encendido | Ejecutar el sistema completo, la base local y las pruebas automáticas |
| JDK | 25 | Solo para **desarrollar** el backend con `./mvnw`; no hace falta para ejecutarlo con Docker |

Maven no hace falta: el proyecto trae su propio wrapper (`mvnw`). Para los scripts de `scripts/` basta Git Bash,
WSL, Linux o macOS con `curl`.

## Ejecutar el sistema (solo Docker)

Sirve para quien solo necesita el backend funcionando, por ejemplo para probar un cliente. No requiere JDK.

```bash
git clone git@github.com:LeyderRodriguez/Polimarket.git
cd Polimarket
git checkout develop            # o la rama que se vaya a probar
docker compose up --build       # construye el backend, levanta PostgreSQL y espera a que esté listo
```

La primera vez tarda unos minutos porque descarga dependencias. Cuando aparece `Started PoliMarketApplication`,
el backend está en `http://localhost:8080` con los datos de demostración. Para comprobarlo:

```bash
curl -i http://localhost:8080/api/auth/me     # debe responder 401: está vivo y pide sesión
bash scripts/probar-flujo-venta.sh            # recorrido completo con los datos de demostración
```

**Docker no reconstruye solo.** Si cambia el código del backend (por ejemplo después de un `git pull`), hay que
pedirle que reconstruya la imagen; si no, sigue ejecutando la versión anterior:

```bash
docker compose up --build -d backend    # reconstruye solo el backend y lo reinicia, sin tocar la base
```

Comandos útiles:

| Quiero | Comando |
|---|---|
| Ver lo que escribe el backend | `docker compose logs -f backend` |
| Detener todo conservando los datos | `docker compose down` |
| Empezar de cero (borra la base) | `docker compose down -v` |
| Backend sin datos de demostración | `POLIMARKET_DEMO_CARGAR_DATOS=false docker compose up --build` |

## Desarrollar el backend (con JDK 25)

Con esto se compila y se reinicia más rápido que con Docker. Se levanta solo la base:

```bash
docker compose up -d postgres   # PostgreSQL en 127.0.0.1:5432
cd backend
./mvnw spring-boot:run
```

En Windows PowerShell el último comando es `.\mvnw.cmd spring-boot:run`.
Detener el backend: `Ctrl+C`. Si el backend de Docker está encendido, hay que apagarlo antes (`docker compose stop backend`)
porque los dos usan el puerto 8080.

Pruebas automáticas (con Docker encendido, unos 25 segundos): `cd backend && ./mvnw test`.

La base es local a cada equipo: las ventas de uno no aparecen en la de otro. Para una demostración conjunta,
todos los clientes deben apuntar a un mismo backend.

**Problemas frecuentes**

| Síntoma | Causa y solución |
|---|---|
| `Connection refused` al arrancar | Docker apagado o la base no ha terminado de iniciar: `docker compose up -d` y esperar. |
| `port 5432 is already allocated` | Hay otro PostgreSQL en el equipo: detenerlo o cambiar el puerto en `docker-compose.yml`. |
| `invalid target release: 25` | El JDK no es 25: instalar JDK 25 y ajustar `JAVA_HOME`. |
| `Permission denied: ./mvnw` | Solo en Linux/macOS si el archivo perdió el permiso: `chmod +x backend/mvnw`. |
| `Port 8080 was already in use` | Hay otro backend encendido (de Docker o de `./mvnw`): detener uno de los dos. |
| Cambié código y no se nota | Docker usa la imagen vieja: `docker compose up --build -d backend`. |

## Cuentas de demostración

Al arrancar la primera vez se cargan datos ficticios (una sola vez). **La clave de todas las cuentas es `Demo-1234`**
(ficticia, solo para desarrollo; sale de `polimarket.demo.clave` en `backend/src/main/resources/application.yml`).

| Correo | Rol | Notas |
|---|---|---|
| `rh@polimarket.test` | Recursos Humanos | Crea cuentas y aprueba avales |
| `bodega@polimarket.test` | Bodega | Productos, proveedores, entradas y ajustes |
| `entregas@polimarket.test` | Entregas | Ve ventas pendientes y las despacha |
| `ana@polimarket.test` | Vendedor | Aval aprobado: puede vender |
| `luis@polimarket.test` | Vendedor | Aval pendiente: no puede vender |

## Probar a mano

Con el backend en marcha y la base recién creada (`docker compose down -v` y volver a levantarlo):

```bash
bash scripts/probar-api.sh          # RSI-1: cuentas y aval
bash scripts/probar-flujo-venta.sh  # RSI-2 a RSI-6: vender, reservar, anular y despachar
```

## Documentación

- **[docs/guides/clients.md](docs/guides/clients.md): para el frontend. Cómo levantar el backend, sesión, CSRF y qué pantalla llama a qué.**
- [docs/reference/api-rest.md](docs/reference/api-rest.md): referencia de todos los endpoints (y Swagger UI en `http://localhost:8080/swagger-ui.html`).
- [docs/architecture/scope.md](docs/architecture/scope.md): qué hace el backend, los componentes, los roles y las reglas de negocio.
- [docs/reference/database.md](docs/reference/database.md): base de datos, esquemas por componente y migraciones.
- `docs/architecture/diagrams/`: código PlantUML de los diagramas de clases y de componentes.
