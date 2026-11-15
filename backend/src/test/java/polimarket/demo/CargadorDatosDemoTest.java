package polimarket.demo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import polimarket.ContenedorPostgresAislado;
import polimarket.bodega.api.IInventario;
import polimarket.recursoshumanos.api.EstadoAval;
import polimarket.recursoshumanos.api.IGestionPersonal;

/** Cuando se piden los datos de demostración se cargan, y volver a cargarlos no duplica nada. */
@SpringBootTest(properties = "polimarket.demo.cargar-datos=true")
@Import(ContenedorPostgresAislado.class)
class CargadorDatosDemoTest {

    @Autowired CargadorDatosDemo cargador;
    @Autowired IGestionPersonal personal;
    @Autowired IInventario inventario;
    @Autowired JdbcTemplate jdbc;

    @Test
    void cargaLasCuentasProductosYClientesDeDemostracion() {
        assertThat(personal.listarVendedores())
                .extracting(v -> v.nombre() + ":" + v.estadoAval())
                .contains("Ana Pérez:" + EstadoAval.APROBADO, "Luis Mora:" + EstadoAval.PENDIENTE);
        assertThat(inventario.listarDisponibilidad()).extracting(d -> d.codigo()).contains("ARR-01", "AZU-01", "ACE-01");
        assertThat(inventario.listarBajoStock()).extracting(b -> b.nombre()).contains("Azúcar");
        assertThat(inventario.listarBajoStock().stream().filter(b -> b.nombre().equals("Azúcar")).findFirst().orElseThrow()
                .proveedores()).extracting(p -> p.nombre()).containsExactly("Distribuidora Ejemplo");
    }

    @Test
    void volverACargarNoDuplicaNiPisaLosDatos() throws Exception {
        int cuentasAntes = contar("seguridad.usuario");
        int productosAntes = contar("bodega.producto");

        cargador.run(null);
        cargador.run(null);

        assertThat(contar("seguridad.usuario")).isEqualTo(cuentasAntes);
        assertThat(contar("bodega.producto")).isEqualTo(productosAntes);
    }

    private int contar(String tabla) {
        return jdbc.queryForObject("select count(*) from " + tabla, Integer.class);
    }
}
