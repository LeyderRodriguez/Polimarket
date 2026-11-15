package polimarket;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** Prueba de humo: la aplicación arranca contra PostgreSQL y Flyway crea un esquema por componente. */
@SpringBootTest
@AutoConfigureMockMvc // mismas anotaciones que las pruebas de API: así comparten el mismo contexto de Spring
@Import(ContenedorPostgres.class)
class EsquemasPorComponenteTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void flywayCreaUnEsquemaPorComponente() {
        List<String> esquemas = jdbc.queryForList("select schema_name from information_schema.schemata", String.class);

        assertThat(esquemas).contains(
                "seguridad", "terceros", "recursos_humanos", "ventas", "bodega", "proveedores", "entregas");
    }
}
