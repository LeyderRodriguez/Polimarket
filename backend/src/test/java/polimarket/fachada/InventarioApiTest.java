package polimarket.fachada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

/** RSI-2 (disponibilidad) y RSI-4 (bajo stock y proveedores) por la API. */
class InventarioApiTest extends BaseApiTest {

    MockHttpSession rh;
    MockHttpSession bodega;

    @BeforeEach
    void sesiones() throws Exception {
        rh = iniciarSesion(correoRh);
        MvcResult cuenta = crearCuentaPorApi(rh, "Bruno", "BODEGA");
        bodega = iniciarSesion(correoDe("bruno", leerId(cuenta, "$.terceroId")));
    }

    @Test
    void bodegaCreaProductoYElPrecioViajaComoTexto() throws Exception {
        mvc.perform(post("/api/productos").session(bodega).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"arr-01\",\"nombre\":\"Arroz\",\"descripcion\":\"Paquete\","
                                + "\"precioVenta\":\"5000\",\"stockMinimo\":5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("ARR-01"))
                .andExpect(jsonPath("$.precioVenta").value("5000.00"))
                .andExpect(jsonPath("$.stockMinimo").value(5));
    }

    @Test
    void noSePuedeRepetirElCodigoDeProducto() throws Exception {
        crearProducto("ARR-01", "Arroz", 5);

        mvc.perform(post("/api/productos").session(bodega).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"ARR-01\",\"nombre\":\"Otro\",\"precioVenta\":\"1\",\"stockMinimo\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CODIGO_DUPLICADO"));
    }

    @Test
    void bodegaActualizaUnProductoSoloEnLosCamposQueEnvia() throws Exception {
        long producto = crearProducto("ARR-01", "Arroz", 5);

        mvc.perform(patch("/api/productos/" + producto).session(bodega).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"precioVenta\":\"5500\",\"stockMinimo\":8}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioVenta").value("5500.00"))
                .andExpect(jsonPath("$.stockMinimo").value(8))
                .andExpect(jsonPath("$.nombre").value("Arroz")) // lo que no se envió no cambia
                .andExpect(jsonPath("$.codigo").value("ARR-01"));

        mvc.perform(patch("/api/productos/999999").session(bodega).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"X\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/productos/" + producto).session(bodega).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"precioVenta\":\"-1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bodegaActualizaYListaProveedores() throws Exception {
        long proveedor = crearProveedor("Distribuidora Ejemplo");

        mvc.perform(patch("/api/proveedores/" + proveedor).session(bodega).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"telefonoContacto\":\"3009998888\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefonoContacto").value("3009998888"))
                .andExpect(jsonPath("$.nombreContacto").value("Lucía")); // no se pisó

        mvc.perform(get("/api/proveedores").session(bodega))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + proveedor + ")].nombre").value("Distribuidora Ejemplo"));

        mvc.perform(post("/api/proveedores").session(bodega).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"terceroId\":" + terceros.consultarPorDocumento("NIT",
                                String.valueOf(empresaDe(proveedor))).orElseThrow().id() + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PROVEEDOR_EXISTENTE"));
    }

    @Test
    void unFrontQueIntentaBorrarRecibeUnErrorClaroYNoSeTocaNingunDato() throws Exception {
        long producto = crearProducto("ARR-01", "Arroz", 5);

        mvc.perform(delete("/api/productos/" + producto).session(bodega).with(csrf()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.codigo").value("METODO_NO_PERMITIDO"));

        mvc.perform(get("/api/productos").session(bodega))
                .andExpect(jsonPath("$[?(@.id == " + producto + ")].nombre").value("Arroz")); // sigue ahí
    }

    @Test
    void soloBodegaCreaProductos() throws Exception {
        mvc.perform(post("/api/productos").session(rh).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"X\",\"nombre\":\"X\",\"precioVenta\":\"1\",\"stockMinimo\":0}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void disponibilidadMuestraFisicoReservadoYDisponible() throws Exception {
        long producto = crearProducto("ARR-01", "Arroz", 5);
        long proveedor = crearProveedor("Distribuidora Ejemplo");
        registrarEntrada(producto, 10, proveedor);
        reservar(producto, 6); // lo que haría una venta de 6 unidades

        mvc.perform(get("/api/productos").session(bodega))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + producto + ")].stockFisico").value(10))
                .andExpect(jsonPath("$[?(@.id == " + producto + ")].cantidadReservada").value(6))
                .andExpect(jsonPath("$[?(@.id == " + producto + ")].cantidadDisponible").value(4))
                .andExpect(jsonPath("$[?(@.id == " + producto + ")].precio").value("5000.00"));
    }

    @Test
    void unaReservaLiberadaNoCuentaComoReservada() throws Exception {
        long producto = crearProducto("ARR-01", "Arroz", 5);
        registrarEntrada(producto, 10, crearProveedor("Distribuidora Ejemplo"));
        long reserva = reservar(producto, 6);
        jdbc.update("update bodega.reserva_inventario set estado = 'LIBERADA' where id = ?", reserva);

        mvc.perform(get("/api/productos").session(bodega))
                .andExpect(jsonPath("$[?(@.id == " + producto + ")].cantidadDisponible").value(10));
    }

    @Test
    void unVendedorSinAvalNoVeLaDisponibilidadPeroConAvalSi() throws Exception {
        MvcResult cuenta = crearCuentaPorApi(rh, "Vera", "VENDEDOR");
        MockHttpSession vendedor = iniciarSesion(correoDe("vera", leerId(cuenta, "$.terceroId")));

        mvc.perform(get("/api/productos").session(vendedor))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("VENDEDOR_SIN_AVAL"));

        mvc.perform(post("/api/vendedores/" + leerId(cuenta, "$.vendedorId") + "/aval/aprobar").session(rh)
                .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());

        mvc.perform(get("/api/productos").session(vendedor)).andExpect(status().isOk());
    }

    @Test
    void bajoStockListaLosProductosCuyoDisponibleEstaBajoElMinimoConSusProveedores() throws Exception {
        long escaso = crearProducto("ARR-01", "Arroz", 5);
        long abundante = crearProducto("AZU-01", "Azúcar", 5);
        long proveedor = crearProveedor("Distribuidora Ejemplo");
        asociar(proveedor, escaso);
        registrarEntrada(escaso, 10, proveedor);
        registrarEntrada(abundante, 10, proveedor);
        reservar(escaso, 6); // disponible 4 < mínimo 5

        mvc.perform(get("/api/inventario/bajo-stock").session(bodega))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.productoId == " + escaso + ")].cantidadDisponible").value(4))
                .andExpect(jsonPath("$[?(@.productoId == " + escaso + ")].stockMinimo").value(5))
                .andExpect(jsonPath("$[?(@.productoId == " + escaso + ")].proveedores[0].nombre")
                        .value("Distribuidora Ejemplo"))
                .andExpect(jsonPath("$[?(@.productoId == " + abundante + ")]").isEmpty());
    }

    @Test
    void soloBodegaVeElBajoStock() throws Exception {
        MvcResult cuenta = crearCuentaPorApi(rh, "Edgar", "ENTREGAS");
        MockHttpSession entregas = iniciarSesion(correoDe("edgar", leerId(cuenta, "$.terceroId")));

        mvc.perform(get("/api/inventario/bajo-stock").session(entregas)).andExpect(status().isForbidden());
    }

    @Test
    void laEntradaSumaAlFisicoYQuedaTrazable() throws Exception {
        long producto = crearProducto("ARR-01", "Arroz", 5);
        long proveedor = crearProveedor("Distribuidora Ejemplo");

        mvc.perform(post("/api/inventario/entradas").session(bodega).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\":" + producto + ",\"cantidad\":10,\"proveedorId\":" + proveedor
                                + ",\"referencia\":\"FAC-001\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("ENTRADA"))
                .andExpect(jsonPath("$.stockAnterior").value(0))
                .andExpect(jsonPath("$.stockFisico").value(10));

        Integer movimientos = jdbc.queryForObject(
                "select count(*) from bodega.movimiento_inventario where producto_id = ? and tipo = 'ENTRADA'",
                Integer.class, producto);
        assertThat(movimientos).isEqualTo(1);
    }

    @Test
    void laEntradaExigeUnProveedorExistenteYCantidadPositiva() throws Exception {
        long producto = crearProducto("ARR-01", "Arroz", 5);

        mvc.perform(post("/api/inventario/entradas").session(bodega).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\":" + producto + ",\"cantidad\":10,\"proveedorId\":999999}"))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/inventario/entradas").session(bodega).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\":" + producto + ",\"cantidad\":0,\"proveedorId\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unAjusteNoPuedeDejarElFisicoPorDebajoDeLasReservas() throws Exception {
        long producto = crearProducto("ARR-01", "Arroz", 5);
        registrarEntrada(producto, 10, crearProveedor("Distribuidora Ejemplo"));
        reservar(producto, 6);

        mvc.perform(post("/api/inventario/ajustes").session(bodega).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\":" + producto + ",\"nuevoStockFisico\":5,\"motivo\":\"Conteo\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"));

        mvc.perform(get("/api/productos").session(bodega))
                .andExpect(jsonPath("$[?(@.id == " + producto + ")].stockFisico").value(10)); // sin cambios
    }

    @Test
    void unAjusteValidoGuardaMotivoValorAnteriorYNuevo() throws Exception {
        long producto = crearProducto("ARR-01", "Arroz", 5);
        registrarEntrada(producto, 10, crearProveedor("Distribuidora Ejemplo"));

        mvc.perform(post("/api/inventario/ajustes").session(bodega).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\":" + producto
                                + ",\"nuevoStockFisico\":9,\"motivo\":\"Corrección por conteo\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("AJUSTE"))
                .andExpect(jsonPath("$.stockAnterior").value(10))
                .andExpect(jsonPath("$.stockFisico").value(9));

        String motivo = jdbc.queryForObject(
                "select motivo from bodega.movimiento_inventario where producto_id = ? and tipo = 'AJUSTE'",
                String.class, producto);
        assertThat(motivo).isEqualTo("Corrección por conteo");
    }

    @Test
    void unAjusteExigeMotivo() throws Exception {
        long producto = crearProducto("ARR-01", "Arroz", 5);

        mvc.perform(post("/api/inventario/ajustes").session(bodega).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\":" + producto + ",\"nuevoStockFisico\":9,\"motivo\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
    }

    @Test
    void noSePuedeAsociarDosVecesElMismoProductoAUnProveedor() throws Exception {
        long producto = crearProducto("ARR-01", "Arroz", 5);
        long proveedor = crearProveedor("Distribuidora Ejemplo");
        asociar(proveedor, producto);

        mvc.perform(post("/api/proveedores/" + proveedor + "/productos").session(bodega).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"productoId\":" + producto + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PRODUCTO_YA_ASOCIADO"));
    }

    // ---- apoyo

    private long crearProducto(String codigo, String nombre, int minimo) throws Exception {
        MvcResult r = mvc.perform(post("/api/productos").session(bodega).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + codigo + "\",\"nombre\":\"" + nombre
                                + "\",\"precioVenta\":\"5000.00\",\"stockMinimo\":" + minimo + "}"))
                .andExpect(status().isCreated()).andReturn();
        return leerId(r, "$.id");
    }

    /** Documento (NIT) de la empresa detrás de un proveedor, para repetir su registro. */
    private String empresaDe(long proveedorId) {
        return jdbc.queryForObject("select t.numero_documento from proveedores.proveedor p "
                + "join terceros.tercero t on t.id = p.tercero_id where p.id = ?", String.class, proveedorId);
    }

    private long crearProveedor(String razonSocial) throws Exception {
        TerceroDto empresa = empresa(razonSocial);
        MvcResult r = mvc.perform(post("/api/proveedores").session(bodega).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"terceroId\":" + empresa.id() + ",\"nombreContacto\":\"Lucía\"}"))
                .andExpect(status().isCreated()).andReturn();
        return leerId(r, "$.id");
    }

    private void asociar(long proveedor, long producto) throws Exception {
        mvc.perform(post("/api/proveedores/" + proveedor + "/productos").session(bodega).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"productoId\":" + producto + "}"))
                .andExpect(status().isCreated());
    }

    private void registrarEntrada(long producto, int cantidad, long proveedor) throws Exception {
        mvc.perform(post("/api/inventario/entradas").session(bodega).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\":" + producto + ",\"cantidad\":" + cantidad + ",\"proveedorId\":"
                                + proveedor + "}"))
                .andExpect(status().isCreated());
    }

    /** Simula lo que hará Ventas: una reserva ACTIVA con unidades de un producto. */
    private long reservar(long producto, int unidades) {
        Long reserva = jdbc.queryForObject(
                "insert into bodega.reserva_inventario (venta_id, fecha, estado) values (?, now(), 'ACTIVA') returning id",
                Long.class, System.nanoTime());
        jdbc.update("insert into bodega.detalle_reserva (reserva_id, producto_id, cantidad) values (?, ?, ?)",
                reserva, producto, unidades);
        return reserva;
    }
}
