package polimarket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import polimarket.bodega.api.DisponibilidadDto;
import polimarket.bodega.api.IInventario;
import polimarket.bodega.api.ItemReserva;
import polimarket.bodega.api.NuevaEntrada;
import polimarket.bodega.api.NuevoProducto;
import polimarket.bodega.api.ProductoDto;
import polimarket.compartido.ExcepcionNegocio;
import polimarket.entregas.api.IEntregas;
import polimarket.proveedores.api.IProveedores;
import polimarket.proveedores.api.NuevoProveedor;
import polimarket.recursoshumanos.api.IAutorizacion;
import polimarket.recursoshumanos.api.IGestionPersonal;
import polimarket.terceros.api.ITerceros;
import polimarket.terceros.api.NuevoTercero;
import polimarket.terceros.api.TerceroDto;
import polimarket.terceros.api.TipoTercero;
import polimarket.ventas.api.IClientes;
import polimarket.ventas.api.IVentas;
import polimarket.ventas.api.ItemVenta;
import polimarket.ventas.api.NuevaVenta;

/**
 * Pruebas con hilos reales y transacciones que sí se confirman (sin @Transactional en la prueba), para comprobar
 * los bloqueos de fila: son las que respaldan que dos ventas no reservan las mismas unidades ni se despacha dos veces.
 */
@SpringBootTest
@Import(ContenedorPostgres.class)
class ConcurrenciaTest {

    @Autowired IInventario inventario;
    @Autowired ITerceros terceros;
    @Autowired IProveedores proveedores;
    @Autowired IGestionPersonal personal;
    @Autowired IAutorizacion autorizacion;
    @Autowired IClientes clientes;
    @Autowired IVentas ventas;
    @Autowired IEntregas entregas;
    @Autowired JdbcTemplate jdbc;

    @Test
    void veinteVentasSimultaneasDeUnaUnidadSobreDiezEnBodegaSoloAceptanDiez() throws Exception {
        ProductoDto producto = productoConStock(10);
        long base = System.nanoTime();

        List<Callable<String>> tareas = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            long ventaId = base + i;
            tareas.add(() -> intentar(() -> inventario.reservar(ventaId, List.of(new ItemReserva(producto.id(), 1)))));
        }
        List<String> resultados = ejecutarALaVez(tareas);

        assertThat(resultados.stream().filter("OK"::equals)).hasSize(10);
        assertThat(resultados.stream().filter("STOCK_INSUFICIENTE"::equals)).hasSize(10);
        DisponibilidadDto d = inventario.consultarDisponibilidad(producto.id());
        assertThat(d.stockFisico()).isEqualTo(10);
        assertThat(d.cantidadReservada()).isEqualTo(10);
        assertThat(d.cantidadDisponible()).isZero(); // nunca se reservó más de lo que había
    }

    @Test
    void cincoDespachosSimultaneosDeLaMismaVentaSoloUnoGana() throws Exception {
        ProductoDto producto = productoConStock(10);
        long vendedorId = vendedorConAval();
        long clienteId = clientes.registrarCliente(persona("Cliente").id(), List.of()).id();
        long ventaId = ventas.registrar(vendedorId,
                new NuevaVenta(clienteId, "Calle 10", List.of(new ItemVenta(producto.id(), 6)))).ventaId();

        List<Callable<String>> tareas = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tareas.add(() -> intentar(() -> entregas.despachar(ventaId, 1L)));
        }
        List<String> resultados = ejecutarALaVez(tareas);

        assertThat(resultados.stream().filter("OK"::equals)).hasSize(1);
        assertThat(resultados.stream().filter("ESTADO_INVALIDO"::equals)).hasSize(4);
        DisponibilidadDto d = inventario.consultarDisponibilidad(producto.id());
        assertThat(d.stockFisico()).isEqualTo(4); // se descontó una sola vez
        assertThat(d.cantidadReservada()).isZero();
        Integer entregasDeLaVenta = jdbc.queryForObject("select count(*) from entregas.entrega where venta_id = ?",
                Integer.class, ventaId);
        assertThat(entregasDeLaVenta).isEqualTo(1);
    }

    @Test
    void unaVentaQueFallaPorStockNoDejaNiVentaNiReservasGuardadas() {
        ProductoDto abundante = productoConStock(10);
        ProductoDto escaso = productoConStock(2);
        long vendedorId = vendedorConAval();
        long clienteId = clientes.registrarCliente(persona("Cliente").id(), List.of()).id();

        assertThatThrownBy(() -> ventas.registrar(vendedorId, new NuevaVenta(clienteId, "Calle 10",
                List.of(new ItemVenta(abundante.id(), 1), new ItemVenta(escaso.id(), 3)))))
                .isInstanceOf(ExcepcionNegocio.class)
                .extracting(e -> ((ExcepcionNegocio) e).getCodigo())
                .isEqualTo("STOCK_INSUFICIENTE");

        // La transacción se confirmó de verdad y se revirtió completa: no queda la venta ni reservas a medias.
        assertThat(ventas.listarVentasDelVendedor(vendedorId)).isEmpty();
        assertThat(inventario.consultarDisponibilidad(abundante.id()).cantidadReservada()).isZero();
        assertThat(inventario.consultarDisponibilidad(escaso.id()).cantidadReservada()).isZero();
    }

    // ---- apoyo

    private ProductoDto productoConStock(int unidades) {
        ProductoDto producto = inventario.crearProducto(new NuevoProducto("CONC-" + System.nanoTime(), "Producto concurrente",
                null, null, new BigDecimal("1000"), 0));
        long proveedor = proveedores.registrar(new NuevoProveedor(empresa().id(), null, null)).id();
        inventario.registrarEntrada(new NuevaEntrada(producto.id(), unidades, proveedor, null, null), 1L);
        return producto;
    }

    private long vendedorConAval() {
        TerceroDto rh = persona("RH");
        personal.registrarEmpleado(rh.id(), "Recursos Humanos");
        long vendedorId = personal.registrarVendedor(persona("Vendedor").id(), null).id();
        autorizacion.aprobar(vendedorId, rh.id(), null);
        return vendedorId;
    }

    private TerceroDto persona(String nombres) {
        return terceros.registrar(new NuevoTercero(TipoTercero.NATURAL, "CC", String.valueOf(System.nanoTime()), nombres,
                "Concurrente", null, null, null, null));
    }

    private TerceroDto empresa() {
        return terceros.registrar(new NuevoTercero(TipoTercero.JURIDICA, "NIT", String.valueOf(System.nanoTime()), null,
                null, "Proveedor concurrente", null, null, null));
    }

    /** Devuelve OK o el código del error de negocio, para poder contar resultados. */
    private static String intentar(Runnable accion) {
        try {
            accion.run();
            return "OK";
        } catch (ExcepcionNegocio e) {
            return e.getCodigo();
        }
    }

    /** Lanza todas las tareas a la vez: esperan en una barrera y arrancan juntas. */
    private static List<String> ejecutarALaVez(List<Callable<String>> tareas) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tareas.size());
        CountDownLatch salida = new CountDownLatch(1);
        try {
            List<Future<String>> futuros = new ArrayList<>();
            for (Callable<String> tarea : tareas) {
                futuros.add(pool.submit(() -> {
                    salida.await();
                    return tarea.call();
                }));
            }
            salida.countDown();
            List<String> resultados = new ArrayList<>();
            for (Future<String> futuro : futuros) {
                resultados.add(futuro.get());
            }
            return resultados;
        } finally {
            pool.shutdownNow();
        }
    }
}
