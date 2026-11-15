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

La propuesta completa está en [docs/propuesta-polimarket.html](docs/propuesta-polimarket.html).
