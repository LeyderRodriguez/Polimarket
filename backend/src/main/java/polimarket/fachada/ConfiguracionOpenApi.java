package polimarket.fachada;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Datos generales de la documentación interactiva de la API. */
@Configuration
class ConfiguracionOpenApi {

    @Bean
    OpenAPI documentacionDeLaApi(
            @Value("${polimarket.demo.cargar-datos:false}") boolean hayDatosDeDemostracion,
            @Value("${polimarket.demo.clave:}") String claveDeDemostracion) {
        // La cuenta de ejemplo se muestra solo si se cargaron los datos de demostración, y con la clave configurada.
        String cuentaDeEjemplo = hayDatosDeDemostracion
                ? "con una cuenta de demostración, por ejemplo `ana@polimarket.test` y la clave `" + claveDeDemostracion + "`"
                : "con una cuenta existente";
        return new OpenAPI().info(new Info()
                .title("PoliMarket API")
                .version("1.0")
                .description("""
                        API REST del backend de PoliMarket. Es lo único que consumen los clientes (Angular y C++).

                        **Cómo probarla desde esta página**
                        1. **Inicie sesión primero:** abra *Sesión → POST /api/auth/login*, pulse *Try it out* y entre %s.
                           El navegador guarda la cookie de sesión. Use siempre `localhost`, no `127.0.0.1`.
                        2. Ya puede probar el resto. Las escrituras (POST, PATCH) llevan solas el encabezado `X-XSRF-TOKEN`.
                        3. Cada operación indica qué rol puede usarla. Si el rol no corresponde responde 403; sin sesión, 401.

                        **Errores:** siempre `{ "codigo": "...", "mensaje": "..." }`. Los montos viajan como texto (`"5000.00"`).
                        """.formatted(cuentaDeEjemplo)));
    }
}
