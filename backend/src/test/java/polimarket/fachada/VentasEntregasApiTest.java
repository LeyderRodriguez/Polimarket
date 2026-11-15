package polimarket.fachada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import polimarket.terceros.api.TerceroDto;

/** RSI-3 (vender y reservar), RSI-6 (anular) y RSI-5 (despachar) por la API, con el ejemplo físico 10 / venta de 6. */
class VentasEntregasApiTest extends BaseApiTest {

    MockHttpSession rh;
    MockHttpSession bodega;
    MockHttpSession vendedor;
    MockHttpSession entregas;
    long vendedorId;
    long producto;
    long cliente;

    @BeforeEach
    void escenario() throws Exception {
        rh = iniciarSesion(correoRh);
        bodega = iniciarSesion(correoDe("bruno", leerId(crearCuentaPorApi(rh, "Bruno", "BODEGA"), "$.terceroId")));
        entregas = iniciarSesion(correoDe("edgar", leerId(crearCuentaPorApi(rh, "Edgar", "ENTREGAS"), "$.terceroId")));

        MvcResult cuentaVendedor = crearCuentaPorApi(rh, "Vera", "VENDEDOR");
        vendedorId = leerId(cuentaVendedor, "$.vendedorId");
        aprobar(vendedorId);
        vendedor = iniciarSesion(correoDe("vera", leerId(cuentaVendedor, "$.terceroId")));

        producto = crearProducto("ARR-01", "Arroz");
        registrarEntrada(producto, 10);
        cliente = crearCliente("María");
    }

    // ---- RSI-3: registrar venta con reserva

    @Test
    void unaVentaDeSeisConDiezEnBodegaDejaCuatroDisponibles() throws Exception {
        mvc.perform(post("/api/ventas").session(vendedor).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(venta(cliente, linea(producto, 6))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_ENTREGA"))
                .andExpect(jsonPath("$.total").value("30000.00"))
                .andExpect(jsonPath("$.reservaEstado").value("ACTIVA"));

        verDisponibilidad(producto, 10, 6, 4);
    }

    @Test
    void unaVentaMayorQueLoDisponibleSeRechazaSinCambios() throws Exception {
        vender(linea(producto, 6));

        mvc.perform(post("/api/ventas").session(vendedor).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(venta(cliente, linea(producto, 5))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"));

        verDisponibilidad(producto, 10, 6, 4); // sigue igual: no quedó nada a medias
    }

    @Test
    void unaVentaConVariosProductosNoDejaReservasParcialesSiUnoFalla() throws Exception {
        long escaso = crearProducto("AZU-01", "Azúcar");
        registrarEntrada(escaso, 2);

        mvc.perform(post("/api/ventas").session(vendedor).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(venta(cliente, linea(producto, 1), linea(escaso, 3))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"));

        verDisponibilidad(producto, 10, 0, 10); // el arroz no quedó reservado
        verDisponibilidad(escaso, 2, 0, 2);
        // Que la venta misma tampoco se guarde se comprueba en ConcurrenciaTest: esta prueba corre dentro de una
        // transacción de prueba que no se confirma, así que no puede mostrar el rollback real.
    }

    @Test
    void elVendedorSalePorSesionYElPrecioLoPoneElSistema() throws Exception {
        // El cliente intenta mandar otro vendedor y un precio propio: ambos se ignoran.
        mvc.perform(post("/api/ventas").session(vendedor).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":" + cliente + ",\"vendedorId\":999999,\"direccionEntrega\":\"Calle 10\","
                                + "\"productos\":[{\"productoId\":" + producto + ",\"cantidad\":1,\"precio\":1}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value("5000.00"));

        mvc.perform(get("/api/ventas/mis-ventas").session(vendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cliente").value("María Prueba"))
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE_ENTREGA"));
    }

    @Test
    void unVendedorSinAvalNoPuedeVender() throws Exception {
        MvcResult nuevo = crearCuentaPorApi(rh, "Nuevo", "VENDEDOR"); // aval PENDIENTE
        MockHttpSession sinAval = iniciarSesion(correoDe("nuevo", leerId(nuevo, "$.terceroId")));

        mvc.perform(post("/api/ventas").session(sinAval).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(venta(cliente, linea(producto, 1))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("VENDEDOR_SIN_AVAL"));
    }

    @Test
    void sinAvalVigenteNoSeCreanVentasNuevasPeroSiSeCancelanLasPropiasPendientes() throws Exception {
        long ventaId = vender(linea(producto, 6));
        mvc.perform(post("/api/vendedores/" + vendedorId + "/aval/rechazar").session(rh).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());

        mvc.perform(post("/api/ventas").session(vendedor).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(venta(cliente, linea(producto, 1))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("VENDEDOR_SIN_AVAL"));

        mvc.perform(get("/api/ventas/mis-ventas").session(vendedor)).andExpect(status().isOk());
        mvc.perform(post("/api/ventas/" + ventaId + "/cancelacion").session(vendedor).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADA"));
    }

    // ---- RSI-6: anular

    @Test
    void anularLiberaLaReservaYNoCambiaElFisico() throws Exception {
        long ventaId = vender(linea(producto, 6));

        mvc.perform(post("/api/ventas/" + ventaId + "/cancelacion").session(vendedor).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADA"))
                .andExpect(jsonPath("$.reservaEstado").value("LIBERADA"));

        verDisponibilidad(producto, 10, 0, 10);
    }

    @Test
    void noSeAnulaDosVecesNiLaVentaDeOtroVendedor() throws Exception {
        long ventaId = vender(linea(producto, 6));
        MvcResult otra = crearCuentaPorApi(rh, "Ivan", "VENDEDOR");
        aprobar(leerId(otra, "$.vendedorId"));
        MockHttpSession ivan = iniciarSesion(correoDe("ivan", leerId(otra, "$.terceroId")));

        mvc.perform(post("/api/ventas/" + ventaId + "/cancelacion").session(ivan).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));

        mvc.perform(post("/api/ventas/" + ventaId + "/cancelacion").session(vendedor).with(csrf()))
                .andExpect(status().isOk());
        mvc.perform(post("/api/ventas/" + ventaId + "/cancelacion").session(vendedor).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ESTADO_INVALIDO"));

        verDisponibilidad(producto, 10, 0, 10); // la segunda cancelación no cambió nada
    }

    // ---- RSI-5: despachar

    @Test
    void despacharDescuentaElFisicoConsumeLaReservaYNoSePuedeRepetir() throws Exception {
        long ventaId = vender(linea(producto, 6));

        mvc.perform(post("/api/ventas/" + ventaId + "/despacho").session(entregas).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("DESPACHADA"))
                .andExpect(jsonPath("$.reservaEstado").value("CONSUMIDA"));

        verDisponibilidad(producto, 4, 0, 4); // físico 10 → 4, reserva consumida
        Integer salidas = jdbc.queryForObject(
                "select count(*) from bodega.movimiento_inventario where producto_id = ? and tipo = 'SALIDA'",
                Integer.class, producto);
        assertThat(salidas).isEqualTo(1);

        mvc.perform(post("/api/ventas/" + ventaId + "/despacho").session(entregas).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ESTADO_INVALIDO"));
        verDisponibilidad(producto, 4, 0, 4); // el segundo intento no descontó otra vez

        mvc.perform(post("/api/ventas/" + ventaId + "/cancelacion").session(vendedor).with(csrf()))
                .andExpect(status().isConflict()); // una venta despachada ya no se cancela
    }

    @Test
    void noSeDespachaUnaVentaAnulada() throws Exception {
        long ventaId = vender(linea(producto, 6));
        mvc.perform(post("/api/ventas/" + ventaId + "/cancelacion").session(vendedor).with(csrf()))
                .andExpect(status().isOk());

        mvc.perform(post("/api/ventas/" + ventaId + "/despacho").session(entregas).with(csrf()))
                .andExpect(status().isConflict());

        verDisponibilidad(producto, 10, 0, 10);
    }

    @Test
    void entregasPuedeDespacharUnaVentaAceptadaAunqueElVendedorPerdioElAval() throws Exception {
        long ventaId = vender(linea(producto, 6));
        mvc.perform(post("/api/vendedores/" + vendedorId + "/aval/rechazar").session(rh).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());

        mvc.perform(post("/api/ventas/" + ventaId + "/despacho").session(entregas).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("DESPACHADA"));
    }

    @Test
    void siSeDesactivaLaCuentaDelVendedorSusVentasPendientesSiguenYEntregasLasDespacha() throws Exception {
        long ventaId = vender(linea(producto, 6));
        MvcResult yo = mvc.perform(get("/api/auth/me").session(vendedor)).andReturn();
        mvc.perform(post("/api/usuarios/" + leerId(yo, "$.id") + "/desactivacion").session(rh).with(csrf()))
                .andExpect(status().isOk());

        mvc.perform(post("/api/ventas/" + ventaId + "/cancelacion").session(vendedor).with(csrf()))
                .andExpect(status().isUnauthorized()); // la cuenta inactiva ya no opera
        mvc.perform(get("/api/ventas/pendientes-entrega").session(entregas))
                .andExpect(jsonPath("$[?(@.ventaId == " + ventaId + ")]").isNotEmpty());
        mvc.perform(post("/api/ventas/" + ventaId + "/despacho").session(entregas).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void soloEntregasVeLosPendientesYDespacha() throws Exception {
        long ventaId = vender(linea(producto, 2));

        mvc.perform(get("/api/ventas/pendientes-entrega").session(entregas))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.ventaId == " + ventaId + ")].cliente").value("María Prueba"))
                .andExpect(jsonPath("$[?(@.ventaId == " + ventaId + ")].direccionEntrega").value("Calle 10 # 20-30"))
                .andExpect(jsonPath("$[?(@.ventaId == " + ventaId + ")].productos[0].nombre").value("Arroz"))
                .andExpect(jsonPath("$[?(@.ventaId == " + ventaId + ")].productos[0].cantidad").value(2));

        mvc.perform(post("/api/ventas/" + ventaId + "/despacho").session(vendedor).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/ventas/" + ventaId + "/despacho").session(bodega).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/ventas/pendientes-entrega").session(vendedor)).andExpect(status().isForbidden());
    }

    // ---- Clientes

    @Test
    void noSePuedeRegistrarDosVecesElMismoClienteYSePuedenActualizarSusDatos() throws Exception {
        TerceroDto luis = persona("Luis", "Mora");
        MvcResult creado = mvc.perform(post("/api/clientes").session(vendedor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"terceroId\":" + luis.id() + ",\"preferencias\":[\"sin gluten\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Luis Mora"))
                .andExpect(jsonPath("$.preferencias[0]").value("sin gluten"))
                .andReturn();

        mvc.perform(post("/api/clientes").session(vendedor).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"terceroId\":" + luis.id() + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CLIENTE_EXISTENTE"));

        long id = leerId(creado, "$.id");
        mvc.perform(patch("/api/clientes/" + id).session(vendedor).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferencias\":[\"orgánico\",\"sin azúcar\"],\"telefono\":\"3001112222\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferencias[1]").value("sin azúcar"));

        assertThat(terceros.consultarPorId(luis.id()).orElseThrow().telefono()).isEqualTo("3001112222");
        mvc.perform(get("/api/clientes").session(vendedor))
                .andExpect(jsonPath("$[?(@.id == " + id + ")].nombre").value("Luis Mora"));
    }

    // ---- apoyo

    private static String linea(long productoId, int cantidad) {
        return "{\"productoId\":" + productoId + ",\"cantidad\":" + cantidad + "}";
    }

    private static String venta(long clienteId, String... lineas) {
        return "{\"clienteId\":" + clienteId + ",\"direccionEntrega\":\"Calle 10 # 20-30\",\"productos\":["
                + String.join(",", lineas) + "]}";
    }

    private long vender(String... lineas) throws Exception {
        MvcResult r = mvc.perform(post("/api/ventas").session(vendedor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(venta(cliente, lineas)))
                .andExpect(status().isCreated()).andReturn();
        return leerId(r, "$.ventaId");
    }

    private void aprobar(long idVendedor) throws Exception {
        mvc.perform(post("/api/vendedores/" + idVendedor + "/aval/aprobar").session(rh).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());
    }

    private long crearProducto(String codigo, String nombre) throws Exception {
        MvcResult r = mvc.perform(post("/api/productos").session(bodega).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + codigo + "\",\"nombre\":\"" + nombre
                                + "\",\"precioVenta\":\"5000.00\",\"stockMinimo\":5}"))
                .andExpect(status().isCreated()).andReturn();
        return leerId(r, "$.id");
    }

    private void registrarEntrada(long idProducto, int cantidad) throws Exception {
        TerceroDto empresa = empresa("Distribuidora " + idProducto);
        MvcResult proveedor = mvc.perform(post("/api/proveedores").session(bodega).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"terceroId\":" + empresa.id() + "}"))
                .andExpect(status().isCreated()).andReturn();
        mvc.perform(post("/api/inventario/entradas").session(bodega).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\":" + idProducto + ",\"cantidad\":" + cantidad + ",\"proveedorId\":"
                                + leerId(proveedor, "$.id") + "}"))
                .andExpect(status().isCreated());
    }

    private long crearCliente(String nombres) throws Exception {
        TerceroDto t = persona(nombres, "Prueba");
        MvcResult r = mvc.perform(post("/api/clientes").session(vendedor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"terceroId\":" + t.id() + "}"))
                .andExpect(status().isCreated()).andReturn();
        return leerId(r, "$.id");
    }

    private void verDisponibilidad(long idProducto, int fisico, int reservado, int disponible) throws Exception {
        String filtro = "$[?(@.id == " + idProducto + ")]";
        mvc.perform(get("/api/productos").session(bodega))
                .andExpect(jsonPath(filtro + ".stockFisico").value(fisico))
                .andExpect(jsonPath(filtro + ".cantidadReservada").value(reservado))
                .andExpect(jsonPath(filtro + ".cantidadDisponible").value(disponible));
    }
}
