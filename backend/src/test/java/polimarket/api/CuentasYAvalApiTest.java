package polimarket.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import polimarket.ContenedorPostgres;
import polimarket.recursoshumanos.IGestionPersonal;
import polimarket.seguridad.IAutenticacion;
import polimarket.seguridad.NuevoUsuario;
import polimarket.seguridad.RolUsuario;
import polimarket.terceros.ITerceros;
import polimarket.terceros.NuevoTercero;
import polimarket.terceros.TerceroDto;
import polimarket.terceros.TipoTercero;

/**
 * Recorrido de RSI-1 por la API: login, crear cuenta de vendedor (aval pendiente), aprobar y consultar.
 * Cada prueba corre en una transacción que se revierte, así no deja datos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ContenedorPostgres.class)
@Transactional
class CuentasYAvalApiTest {

    private static final String CLAVE = "ClavePrueba-1";
    private static final AtomicLong SECUENCIA = new AtomicLong(5_000_000);

    @Autowired MockMvc mvc;
    @Autowired ITerceros terceros;
    @Autowired IAutenticacion autenticacion;
    @Autowired IGestionPersonal personal;

    String correoRh;

    @BeforeEach
    void crearCuentaDeRh() {
        TerceroDto rh = persona("Rosa", "Herrera");
        personal.registrarEmpleado(rh.id(), "Recursos Humanos");
        correoRh = "rh" + rh.id() + "@prueba.test";
        autenticacion.crearUsuario(new NuevoUsuario(rh.id(), correoRh, CLAVE, RolUsuario.RECURSOS_HUMANOS));
    }

    @Test
    void sinSesionDevuelve401() throws Exception {
        mvc.perform(get("/api/vendedores"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void credencialesIncorrectasDevuelven401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correoRh + "\",\"contrasena\":\"incorrecta-1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    void loginDevuelveElPerfilYMeLoConfirma() throws Exception {
        MockHttpSession sesion = iniciarSesion(correoRh);

        mvc.perform(get("/api/auth/me").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Rosa Herrera"))
                .andExpect(jsonPath("$.rol").value("RECURSOS_HUMANOS"));
    }

    @Test
    void rhCreaCuentaDeVendedorConAvalPendienteYLuegoLoAprueba() throws Exception {
        MockHttpSession rh = iniciarSesion(correoRh);
        TerceroDto ana = persona("Ana", "Pérez");

        MvcResult creada = mvc.perform(post("/api/usuarios").session(rh).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuenta(ana.id(), "ana" + ana.id() + "@prueba.test", "VENDEDOR")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("VENDEDOR"))
                .andExpect(jsonPath("$.estadoAval").value("PENDIENTE"))
                .andReturn();
        long vendedorId = ((Number) com.jayway.jsonpath.JsonPath.read(creada.getResponse().getContentAsString(),
                "$.vendedorId")).longValue();

        mvc.perform(get("/api/vendedores/" + vendedorId + "/autorizacion").session(rh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autorizado").value(false));

        mvc.perform(post("/api/vendedores/" + vendedorId + "/aval/aprobar").session(rh).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"observacion\":\"Documentación revisada\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoAval").value("APROBADO"))
                .andExpect(jsonPath("$.autorizado").value(true));

        mvc.perform(get("/api/vendedores/" + vendedorId + "/autorizacion").session(rh))
                .andExpect(jsonPath("$.autorizado").value(true));

        mvc.perform(get("/api/vendedores").session(rh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + vendedorId + ")].nombre").value("Ana Pérez"));
    }

    @Test
    void unVendedorNoPuedeAprobarSuPropioAval() throws Exception {
        MockHttpSession rh = iniciarSesion(correoRh);
        TerceroDto luis = persona("Luis", "Mora");
        String correoLuis = "luis" + luis.id() + "@prueba.test";
        MvcResult creada = mvc.perform(post("/api/usuarios").session(rh).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(cuenta(luis.id(), correoLuis, "VENDEDOR")))
                .andExpect(status().isCreated()).andReturn();
        long vendedorId = ((Number) com.jayway.jsonpath.JsonPath.read(creada.getResponse().getContentAsString(),
                "$.vendedorId")).longValue();

        MockHttpSession vendedor = iniciarSesion(correoLuis);

        mvc.perform(post("/api/vendedores/" + vendedorId + "/aval/aprobar").session(vendedor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
    }

    @Test
    void unVendedorSinAvalNoPuedeUsarTerceros() throws Exception {
        MockHttpSession rh = iniciarSesion(correoRh);
        TerceroDto eva = persona("Eva", "Rojas");
        String correoEva = "eva" + eva.id() + "@prueba.test";
        mvc.perform(post("/api/usuarios").session(rh).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(cuenta(eva.id(), correoEva, "VENDEDOR"))).andExpect(status().isCreated());

        mvc.perform(get("/api/terceros").session(iniciarSesion(correoEva))
                        .param("tipoDocumento", "CC").param("numeroDocumento", "1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("VENDEDOR_SIN_AVAL"));
    }

    @Test
    void sinTokenCsrfLasEscriturasSeRechazan() throws Exception {
        MockHttpSession rh = iniciarSesion(correoRh);

        mvc.perform(post("/api/usuarios").session(rh).contentType(MediaType.APPLICATION_JSON)
                        .content(cuenta(1L, "x@prueba.test", "VENDEDOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rhNoPuedeCrearOtraCuentaDeRh() throws Exception {
        MockHttpSession rh = iniciarSesion(correoRh);
        TerceroDto otra = persona("Otra", "Persona");

        mvc.perform(post("/api/usuarios").session(rh).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(cuenta(otra.id(), "otra" + otra.id() + "@prueba.test", "RECURSOS_HUMANOS")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
    }

    @Test
    void elCorreoYElDocumentoNoSePuedenRepetir() throws Exception {
        MockHttpSession rh = iniciarSesion(correoRh);
        TerceroDto uno = persona("Uno", "Primero");
        TerceroDto dos = persona("Dos", "Segundo");
        String correo = "repetido" + uno.id() + "@prueba.test";
        mvc.perform(post("/api/usuarios").session(rh).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(cuenta(uno.id(), correo, "BODEGA"))).andExpect(status().isCreated());

        mvc.perform(post("/api/usuarios").session(rh).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(cuenta(dos.id(), correo.toUpperCase(), "BODEGA")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CORREO_DUPLICADO"));

        mvc.perform(post("/api/terceros").session(rh).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"NATURAL\",\"tipoDocumento\":\"CC\",\"numeroDocumento\":\""
                                + uno.numeroDocumento() + "\",\"nombres\":\"X\",\"apellidos\":\"Y\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DOCUMENTO_DUPLICADO"));
    }

    @Test
    void rechazarUnAvalAprobadoLoRevocaYNoSePuedeRechazarDosVeces() throws Exception {
        MockHttpSession rh = iniciarSesion(correoRh);
        TerceroDto marta = persona("Marta", "Gil");
        MvcResult creada = mvc.perform(post("/api/usuarios").session(rh).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuenta(marta.id(), "marta" + marta.id() + "@prueba.test", "VENDEDOR")))
                .andReturn();
        long id = ((Number) com.jayway.jsonpath.JsonPath.read(creada.getResponse().getContentAsString(),
                "$.vendedorId")).longValue();
        mvc.perform(post("/api/vendedores/" + id + "/aval/aprobar").session(rh).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());

        mvc.perform(post("/api/vendedores/" + id + "/aval/rechazar").session(rh).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"observacion\":\"Revocado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autorizado").value(false));

        mvc.perform(post("/api/vendedores/" + id + "/aval/rechazar").session(rh).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ESTADO_INVALIDO"));
    }

    // ---- apoyo

    private MockHttpSession iniciarSesion(String correo) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"contrasena\":\"" + CLAVE + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) r.getRequest().getSession(false);
    }

    private TerceroDto persona(String nombres, String apellidos) {
        return terceros.registrar(new NuevoTercero(TipoTercero.NATURAL, "CC",
                String.valueOf(SECUENCIA.incrementAndGet()), nombres, apellidos, null, null, null, null));
    }

    private static String cuenta(long terceroId, String correo, String rol) {
        return "{\"terceroId\":" + terceroId + ",\"correo\":\"" + correo + "\",\"contrasenaInicial\":\"" + CLAVE
                + "\",\"rol\":\"" + rol + "\"}";
    }
}
