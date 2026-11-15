package polimarket;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base PostgreSQL desechable para las pruebas, nunca la de demostración. El contenedor es único por ejecución:
 * se arranca una sola vez aunque haya varios contextos de Spring (con y sin perfil dev), en vez de uno por contexto.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ContenedorPostgres {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

    static {
        POSTGRES.start();
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return POSTGRES;
    }
}
