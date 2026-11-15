package polimarket;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Prueba de humo: la aplicación arranca contra PostgreSQL y Flyway crea un esquema por componente. */
@SpringBootTest
@Testcontainers
class EsquemasPorComponenteTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void flywayCreaUnEsquemaPorComponente() {
        List<String> esquemas = jdbc.queryForList("select schema_name from information_schema.schemata", String.class);

        assertThat(esquemas).contains(
                "seguridad", "terceros", "recursos_humanos", "ventas", "bodega", "proveedores", "entregas");
    }
}
