package polimarket.fachada;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Datos generales de la documentación interactiva de la API. */
@Configuration
class ConfiguracionOpenApi {

    @Bean
    OpenAPI documentacionDeLaApi() {
        return new OpenAPI().info(new Info()
                .title("PoliMarket API")
                .version("1.0")
                .description("""
                        API REST del backend de PoliMarket. Es lo único que consumen los clientes (Angular y C++).

                        **Cómo probarla desde esta página**
                        1. Abra *Sesión → POST /api/auth/login*, pulse *Try it out* y entre con una cuenta de demostración
                           (correos y clave en el README del repositorio). El navegador guarda la cookie de sesión.
                        2. Ya puede probar el resto. Las escrituras (POST, PATCH) llevan solas el encabezado `X-XSRF-TOKEN`.
                        3. Cada operación indica qué rol puede usarla. Si el rol no corresponde responde 403.

                        **Errores:** siempre `{ "codigo": "...", "mensaje": "..." }`. Los montos viajan como texto (`"5000.00"`).
                        """));
    }
}
