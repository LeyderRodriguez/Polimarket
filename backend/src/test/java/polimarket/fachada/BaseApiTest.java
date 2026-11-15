package polimarket.fachada;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.jayway.jsonpath.JsonPath;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import polimarket.ContenedorPostgres;
import polimarket.recursoshumanos.api.IGestionPersonal;
import polimarket.seguridad.api.IAutenticacion;
import polimarket.seguridad.api.NuevoUsuario;
import polimarket.seguridad.api.RolUsuario;
import polimarket.terceros.api.ITerceros;
import polimarket.terceros.api.NuevoTercero;
import polimarket.terceros.api.TerceroDto;
import polimarket.terceros.api.TipoTercero;

/**
 * Base de las pruebas de API: PostgreSQL desechable, una cuenta de Recursos Humanos por prueba y
 * utilidades para iniciar sesión y crear datos. Cada prueba corre en una transacción que se revierte.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ContenedorPostgres.class)
@Transactional
abstract class BaseApiTest {

    protected static final String CLAVE = "ClavePrueba-1";
    private static final AtomicLong SECUENCIA = new AtomicLong(5_000_000);

    @Autowired protected MockMvc mvc;
    @Autowired protected ITerceros terceros;
    @Autowired protected IAutenticacion autenticacion;
    @Autowired protected IGestionPersonal personal;
    @Autowired protected JdbcTemplate jdbc;

    protected String correoRh;

    @BeforeEach
    void crearCuentaDeRh() {
        TerceroDto rh = persona("Rosa", "Herrera");
        personal.registrarEmpleado(rh.id(), "Recursos Humanos");
        correoRh = "rh" + rh.id() + "@prueba.test";
        autenticacion.crearUsuario(new NuevoUsuario(rh.id(), correoRh, CLAVE, RolUsuario.RECURSOS_HUMANOS));
    }

    protected MockHttpSession iniciarSesion(String correo) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"contrasena\":\"" + CLAVE + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) r.getRequest().getSession(false);
    }

    protected TerceroDto persona(String nombres, String apellidos) {
        return terceros.registrar(new NuevoTercero(TipoTercero.NATURAL, "CC",
                String.valueOf(SECUENCIA.incrementAndGet()), nombres, apellidos, null, null, null, null));
    }

    protected TerceroDto empresa(String razonSocial) {
        return terceros.registrar(new NuevoTercero(TipoTercero.JURIDICA, "NIT",
                String.valueOf(SECUENCIA.incrementAndGet()), null, null, razonSocial, null, null, null));
    }

    protected static String cuenta(long terceroId, String correo, String rol) {
        return "{\"terceroId\":" + terceroId + ",\"correo\":\"" + correo + "\",\"contrasenaInicial\":\"" + CLAVE
                + "\",\"rol\":\"" + rol + "\"}";
    }

    /** RH crea por la API una cuenta nueva con ese rol y devuelve la respuesta. */
    protected MvcResult crearCuentaPorApi(MockHttpSession rh, String nombres, String rol) throws Exception {
        TerceroDto t = persona(nombres, "Prueba");
        return mvc.perform(post("/api/usuarios").session(rh).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(cuenta(t.id(), correoDe(nombres, t.id()), rol)))
                .andExpect(status().isCreated()).andReturn();
    }

    protected static String correoDe(String nombres, long terceroId) {
        return nombres.toLowerCase() + terceroId + "@prueba.test";
    }

    protected static long leerId(MvcResult resultado, String ruta) throws Exception {
        return ((Number) JsonPath.read(resultado.getResponse().getContentAsString(), ruta)).longValue();
    }
}
