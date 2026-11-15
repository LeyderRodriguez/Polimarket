package polimarket.fachada;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import polimarket.terceros.api.TerceroDto;

/** Recorrido de RSI-1 por la API: login, crear cuenta de vendedor (aval pendiente), aprobar y consultar. */
class CuentasYAvalApiTest extends BaseApiTest {

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
                        .content(cuenta(ana.id(), correoDe("ana", ana.id()), "VENDEDOR")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("VENDEDOR"))
                .andExpect(jsonPath("$.estadoAval").value("PENDIENTE"))
                .andReturn();
        long vendedorId = leerId(creada, "$.vendedorId");

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
        MvcResult creada = mvc.perform(post("/api/usuarios").session(rh).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuenta(luis.id(), correoDe("luis", luis.id()), "VENDEDOR")))
                .andExpect(status().isCreated()).andReturn();
        long vendedorId = leerId(creada, "$.vendedorId");

        MockHttpSession vendedor = iniciarSesion(correoDe("luis", luis.id()));

        mvc.perform(post("/api/vendedores/" + vendedorId + "/aval/aprobar").session(vendedor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
    }

    @Test
    void unVendedorSinAvalNoPuedeUsarTerceros() throws Exception {
        MockHttpSession rh = iniciarSesion(correoRh);
        TerceroDto eva = persona("Eva", "Rojas");
        mvc.perform(post("/api/usuarios").session(rh).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(cuenta(eva.id(), correoDe("eva", eva.id()), "VENDEDOR"))).andExpect(status().isCreated());

        mvc.perform(get("/api/terceros").session(iniciarSesion(correoDe("eva", eva.id())))
                        .param("tipoDocumento", "CC").param("numeroDocumento", "1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("VENDEDOR_SIN_AVAL"));
    }

    @Test
    void elClienteWebDeAngularPuedeLlamarConCredenciales() throws Exception {
        mvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,x-xsrf-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
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
                        .content(cuenta(otra.id(), correoDe("otra", otra.id()), "RECURSOS_HUMANOS")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
    }

    @Test
    void elCorreoYElDocumentoNoSePuedenRepetir() throws Exception {
        MockHttpSession rh = iniciarSesion(correoRh);
        TerceroDto uno = persona("Uno", "Primero");
        TerceroDto dos = persona("Dos", "Segundo");
        String correo = correoDe("repetido", uno.id());
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
    void rhListaLasCuentasSinContrasenaNiHash() throws Exception {
        MockHttpSession rh = iniciarSesion(correoRh);

        mvc.perform(get("/api/usuarios").session(rh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.correo == '" + correoRh + "')].nombre").value("Rosa Herrera"))
                .andExpect(jsonPath("$[0].contrasenaHash").doesNotExist())
                .andExpect(jsonPath("$[0].contrasena").doesNotExist());
    }

    @Test
    void rhCambiaElCorreoPeroNoElRol() throws Exception {
        MockHttpSession rh = iniciarSesion(correoRh);
        MvcResult creada = crearCuentaPorApi(rh, "Sara", "BODEGA");
        long id = leerId(creada, "$.id");

        mvc.perform(patch("/api/usuarios/" + id).session(rh).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"Sara.Nueva@Prueba.test\",\"rol\":\"RECURSOS_HUMANOS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("sara.nueva@prueba.test"))
                .andExpect(jsonPath("$.rol").value("BODEGA")); // el rol enviado se ignora

        MvcResult otra = crearCuentaPorApi(rh, "Tomas", "BODEGA");
        mvc.perform(patch("/api/usuarios/" + leerId(otra, "$.id")).session(rh).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\"sara.nueva@prueba.test\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CORREO_DUPLICADO"));
    }

    @Test
    void unaCuentaDesactivadaNoEntraYSuSesionAbiertaDejaDeValer() throws Exception {
        MockHttpSession rh = iniciarSesion(correoRh);
        MvcResult creada = crearCuentaPorApi(rh, "Paula", "BODEGA");
        String correo = correoDe("paula", leerId(creada, "$.terceroId"));
        MockHttpSession sesionAbierta = iniciarSesion(correo);
        mvc.perform(get("/api/auth/me").session(sesionAbierta)).andExpect(status().isOk());

        mvc.perform(post("/api/usuarios/" + leerId(creada, "$.id") + "/desactivacion").session(rh).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        mvc.perform(get("/api/auth/me").session(sesionAbierta))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CUENTA_DESACTIVADA"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"contrasena\":\"" + CLAVE + "\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/usuarios/" + leerId(creada, "$.id") + "/desactivacion").session(rh).with(csrf()))
                .andExpect(status().isConflict()); // no se desactiva dos veces
    }

    @Test
    void rhNoPuedeDesactivarSuPropiaCuenta() throws Exception {
        MockHttpSession rh = iniciarSesion(correoRh);
        long miId = ((Number) com.jayway.jsonpath.JsonPath.read(
                mvc.perform(get("/api/auth/me").session(rh)).andReturn().getResponse().getContentAsString(), "$.id"))
                .longValue();

        mvc.perform(post("/api/usuarios/" + miId + "/desactivacion").session(rh).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
    }

    @Test
    void soloRhAdministraCuentas() throws Exception {
        MockHttpSession rh = iniciarSesion(correoRh);
        MvcResult bodega = crearCuentaPorApi(rh, "Hugo", "BODEGA");
        MockHttpSession sesionBodega = iniciarSesion(correoDe("hugo", leerId(bodega, "$.terceroId")));

        mvc.perform(get("/api/usuarios").session(sesionBodega)).andExpect(status().isForbidden());
    }

    @Test
    void rechazarUnAvalAprobadoLoRevocaYNoSePuedeRechazarDosVeces() throws Exception {
        MockHttpSession rh = iniciarSesion(correoRh);
        MvcResult creada = crearCuentaPorApi(rh, "Marta", "VENDEDOR");
        long id = leerId(creada, "$.vendedorId");
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
}
