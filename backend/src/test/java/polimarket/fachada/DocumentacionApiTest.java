package polimarket.fachada;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/**
 * El contrato OpenAPI se genera solo a partir de los controllers. Esta prueba evita que una ruta quede sin
 * documentar o que cambie sin que lo notemos: los clientes (Angular y C++) se apoyan en este contrato.
 */
class DocumentacionApiTest extends BaseApiTest {

    @Test
    void elContratoDeLaApiSePublicaSinSesionYListaLosEndpointsDeCadaRequisito() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("PoliMarket API"))
                // RSI-1: aval
                .andExpect(jsonPath("$.paths['/api/vendedores/{id}/aval/aprobar'].post.summary").exists())
                // RSI-2 y RSI-4: disponibilidad y bajo stock
                .andExpect(jsonPath("$.paths['/api/productos'].get.summary").exists())
                .andExpect(jsonPath("$.paths['/api/inventario/bajo-stock'].get.summary").exists())
                // RSI-3, RSI-6 y RSI-5: vender, anular y despachar
                .andExpect(jsonPath("$.paths['/api/ventas'].post.summary").exists())
                .andExpect(jsonPath("$.paths['/api/ventas/{id}/cancelacion'].post.summary").exists())
                .andExpect(jsonPath("$.paths['/api/ventas/{id}/despacho'].post.description")
                        .value("Roles: ENTREGAS."));
    }

    @Test
    void todaOperacionTieneResumenParaQueElFrontNoTengaQueAdivinar() throws Exception {
        String contrato = mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        com.jayway.jsonpath.DocumentContext json = com.jayway.jsonpath.JsonPath.parse(contrato);
        java.util.Map<String, java.util.Map<String, Object>> rutas = json.read("$.paths");

        rutas.forEach((ruta, operaciones) -> operaciones.forEach((metodo, op) -> {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> operacion = (java.util.Map<String, Object>) op;
            org.assertj.core.api.Assertions.assertThat(operacion.get("summary"))
                    .as("%s %s sin resumen", metodo.toUpperCase(), ruta).isNotNull();
        }));
        org.assertj.core.api.Assertions.assertThat(rutas.size()).isGreaterThanOrEqualTo(25);
    }
}
