package polimarket;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Una base PostgreSQL propia, solo para la prueba de los datos de demostración. Esa prueba deja datos confirmados
 * con códigos y documentos fijos (ARR-01, 1000000001…) que chocarían con los de las otras pruebas si compartieran base.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ContenedorPostgresAislado {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresAislado() {
        return new PostgreSQLContainer("postgres:17");
    }
}
