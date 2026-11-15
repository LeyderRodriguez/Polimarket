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
| `scripts/` | Recorridos manuales con `curl` para probar la API en marcha. |

## Requisitos previos

| Herramienta | Versión | Para qué |
|---|---|---|
| Git | reciente | Clonar el repositorio |
| JDK | 25 | Compilar y ejecutar el backend (`java -version` debe mostrar 25) |
| Docker Desktop | encendido | PostgreSQL local y las pruebas automáticas |

Maven no hace falta: el proyecto trae su propio wrapper (`mvnw`). Para los scripts de `scripts/` basta Git Bash,
WSL, Linux o macOS con `curl`.

## Primer arranque en un equipo nuevo

```bash
git clone git@github.com:LeyderRodriguez/Polimarket.git
cd Polimarket
git checkout develop            # o la rama que se vaya a probar
docker compose up -d            # PostgreSQL en 127.0.0.1:5432 (la primera vez descarga la imagen)
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

En Windows PowerShell el último comando es `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"`.

La primera vez, Flyway crea las tablas y el perfil `dev` carga los datos de demostración. Cuando aparece
`Started PoliMarketApplication` el backend está listo en `http://localhost:8080`. Para comprobarlo:

```bash
curl -i http://localhost:8080/api/auth/me     # debe responder 401: está vivo y pide sesión
bash scripts/probar-flujo-venta.sh            # recorrido completo con los datos de demostración
```

- Detener el backend: `Ctrl+C`. Detener la base conservando los datos: `docker compose down`. Borrarla para empezar de cero: `docker compose down -v`.
- Pruebas automáticas (con Docker encendido, unos 25 segundos): `cd backend && ./mvnw test`.
- La base es local a cada equipo: las ventas de uno no aparecen en la de otro. Para una demostración conjunta, todos los clientes deben apuntar a un mismo backend.

**Problemas frecuentes**

| Síntoma | Causa y solución |
|---|---|
| `Connection refused` al arrancar | Docker apagado o la base no ha terminado de iniciar: `docker compose up -d` y esperar. |
| `port 5432 is already allocated` | Hay otro PostgreSQL en el equipo: detenerlo o cambiar el puerto en `docker-compose.yml`. |
| `invalid target release: 25` | El JDK no es 25: instalar JDK 25 y ajustar `JAVA_HOME`. |
| `Permission denied: ./mvnw` | Solo en Linux/macOS si el archivo perdió el permiso: `chmod +x backend/mvnw`. |

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
bash scripts/probar-api.sh          # RSI-1: cuentas y aval
bash scripts/probar-flujo-venta.sh  # RSI-2 a RSI-6: vender, reservar, anular y despachar
```

## Documentación

- [Propuesta de implementación](docs/propuesta-polimarket.html): arquitectura, API, reglas y decisiones.
- `docs/diagramas/`: código PlantUML de los diagramas de clases y de componentes.
