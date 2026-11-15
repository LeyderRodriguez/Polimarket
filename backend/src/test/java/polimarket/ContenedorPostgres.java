package polimarket;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base PostgreSQL desechable para las pruebas. Al importarla desde varias pruebas se comparte
 * un solo contenedor, y nunca se toca la base de demostración.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ContenedorPostgres {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:17");
    }
}
