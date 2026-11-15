# Documentación de PoliMarket

Organizada según para qué abres el documento (idea tomada de [Diátaxis](https://diataxis.fr/)):

| Carpeta | Para | Documentos |
|---|---|---|
| [`guides/`](guides/) | **Hacer algo** paso a paso | [clients.md](guides/clients.md): levantar el backend, sesión, CSRF y qué pantalla llama a qué |
| [`reference/`](reference/) | **Consultar** un dato exacto | [api-rest.md](reference/api-rest.md): todos los endpoints · [database.md](reference/database.md): esquemas, tablas y migraciones |
| [`evidence/`](evidence/) | **Comprobar** que funciona | [web-client/](evidence/web-client/README.md): recorrido con capturas del cliente web |
| [`architecture/`](architecture/) | **Entender** por qué es así | [scope.md](architecture/scope.md): alcance, componentes, roles y reglas · [web-client-plan.md](architecture/web-client-plan.md): alcance y fases del cliente web · [diagrams/](architecture/diagrams/): PlantUML de clases y componentes |

## Según quién seas

| Si eres… | Empieza por |
|---|---|
| **Del frontend** (Angular o C++) | [guides/clients.md](guides/clients.md) |
| Necesitas el detalle de un endpoint | [reference/api-rest.md](reference/api-rest.md), o Swagger UI en `http://localhost:8080/swagger-ui.html` |
| Quieres saber qué hace el backend y qué no | [architecture/scope.md](architecture/scope.md) |
| Necesitas entender la base de datos | [reference/database.md](reference/database.md) |

Para ejecutar el sistema y probarlo a mano, el [README del repositorio](../README.md) tiene los pasos.
