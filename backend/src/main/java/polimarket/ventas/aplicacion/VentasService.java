package polimarket.ventas.aplicacion;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import polimarket.bodega.api.EstadoReserva;
import polimarket.bodega.api.IInventario;
import polimarket.bodega.api.ItemReserva;
import polimarket.bodega.api.ProductoDto;
import polimarket.bodega.api.ReservaDto;
import polimarket.compartido.ExcepcionNegocio;
import polimarket.recursoshumanos.api.IAutorizacion;
import polimarket.ventas.api.ClienteDto;
import polimarket.ventas.api.EstadoVenta;
import polimarket.ventas.api.IClientes;
import polimarket.ventas.api.ItemVenta;
import polimarket.ventas.api.IVentas;
import polimarket.ventas.api.NuevaVenta;
import polimarket.ventas.api.VentaDto;
import polimarket.ventas.api.VentaPendienteDto;
import polimarket.ventas.api.VentaResumenDto;
import polimarket.ventas.dominio.DetalleVenta;
import polimarket.ventas.dominio.DetalleVentaRepositorio;
import polimarket.ventas.dominio.Venta;
import polimarket.ventas.dominio.VentaRepositorio;

@Service
@Transactional
class VentasService implements IVentas {

    private final VentaRepositorio ventas;
    private final DetalleVentaRepositorio detalles;
    private final IClientes clientes;
    private final IAutorizacion autorizacion;
    private final IInventario inventario;
    private final Clock reloj;

    VentasService(VentaRepositorio ventas, DetalleVentaRepositorio detalles, IClientes clientes,
            IAutorizacion autorizacion, IInventario inventario, Clock reloj) {
        this.ventas = ventas;
        this.detalles = detalles;
        this.clientes = clientes;
        this.autorizacion = autorizacion;
        this.inventario = inventario;
        this.reloj = reloj;
    }

    @Override
    public VentaDto registrar(long vendedorId, NuevaVenta n) {
        if (!autorizacion.estaAutorizado(vendedorId)) {
            throw ExcepcionNegocio.sinPermiso("VENDEDOR_SIN_AVAL", "El vendedor no tiene aval vigente.");
        }
        if (n == null || n.direccionEntrega() == null || n.direccionEntrega().isBlank()
                || n.productos() == null || n.productos().isEmpty()) {
            throw ExcepcionNegocio.datosInvalidos("La venta requiere cliente, dirección de entrega y al menos un producto.");
        }
        if (clientes.consultarCliente(n.clienteId()).isEmpty()) {
            throw ExcepcionNegocio.noEncontrado("No se encontró el cliente.");
        }
        Map<Long, Integer> pedido = unir(n.productos());
        Map<Long, ProductoDto> productos = inventario.consultarProductos(pedido.keySet()).stream()
                .collect(Collectors.toMap(ProductoDto::id, Function.identity()));
        if (productos.size() != pedido.size()) {
            throw ExcepcionNegocio.noEncontrado("Alguno de los productos no existe.");
        }
        // Los precios los pone el sistema, nunca el cliente.
        BigDecimal total = pedido.entrySet().stream()
                .map(e -> productos.get(e.getKey()).precioVenta().multiply(BigDecimal.valueOf(e.getValue())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Venta venta = ventas.save(new Venta(n.clienteId(), vendedorId, Instant.now(reloj), n.direccionEntrega().trim(), total));
        pedido.forEach((productoId, cantidad) -> detalles.save(
                new DetalleVenta(venta.getId(), productoId, cantidad, productos.get(productoId).precioVenta())));

        // Si falta stock lanza STOCK_INSUFICIENTE y se revierte la venta completa: nunca quedan reservas parciales.
        ReservaDto reserva = inventario.reservar(venta.getId(),
                pedido.entrySet().stream().map(e -> new ItemReserva(e.getKey(), e.getValue())).toList());
        return new VentaDto(venta.getId(), venta.getEstado(), venta.getTotal(), reserva.estado());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaResumenDto> listarVentasDelVendedor(long vendedorId) {
        List<Venta> lista = ventas.findByVendedorIdOrderByIdDesc(vendedorId);
        Map<Long, String> nombres = nombresDeClientes(lista);
        return lista.stream()
                .map(v -> new VentaResumenDto(v.getId(), nombres.get(v.getClienteId()), v.getEstado(), v.getTotal()))
                .toList();
    }

    @Override
    public VentaDto anular(long ventaId, long vendedorId) {
        Venta venta = ventas.bloquear(ventaId).orElseThrow(() -> ExcepcionNegocio.noEncontrado("No se encontró la venta."));
        if (!venta.getVendedorId().equals(vendedorId)) {
            throw ExcepcionNegocio.sinPermiso("SIN_PERMISO", "Solo puede anular sus propias ventas.");
        }
        venta.anular();
        ReservaDto reserva = inventario.liberarReserva(ventaId);
        return new VentaDto(ventaId, venta.getEstado(), venta.getTotal(), reserva.estado());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaPendienteDto> listarPendientesEntrega() {
        return aPendientes(ventas.findByEstadoOrderById(EstadoVenta.PENDIENTE_ENTREGA));
    }

    @Override
    public VentaPendienteDto marcarDespachada(long ventaId) {
        Venta venta = ventas.bloquear(ventaId).orElseThrow(() -> ExcepcionNegocio.noEncontrado("No se encontró la venta."));
        venta.marcarDespachada();
        return aPendientes(List.of(venta)).get(0);
    }

    // ---- Apoyo

    /** Une las líneas repetidas de un mismo producto sumando sus cantidades. */
    private static Map<Long, Integer> unir(List<ItemVenta> items) {
        Map<Long, Integer> pedido = new LinkedHashMap<>();
        for (ItemVenta item : items) {
            if (item.cantidad() <= 0) {
                throw ExcepcionNegocio.datosInvalidos("Las cantidades deben ser mayores que cero.");
            }
            pedido.merge(item.productoId(), item.cantidad(), Integer::sum);
        }
        return pedido;
    }

    private Map<Long, String> nombresDeClientes(List<Venta> lista) {
        return clientes.consultarClientes(lista.stream().map(Venta::getClienteId).distinct().toList()).stream()
                .collect(Collectors.toMap(ClienteDto::id, ClienteDto::nombre));
    }

    private List<VentaPendienteDto> aPendientes(List<Venta> lista) {
        if (lista.isEmpty()) {
            return List.of();
        }
        Map<Long, String> nombres = nombresDeClientes(lista);
        Map<Long, List<DetalleVenta>> detallesPorVenta = detalles
                .findByVentaIdIn(lista.stream().map(Venta::getId).toList()).stream()
                .collect(Collectors.groupingBy(DetalleVenta::getVentaId));
        Map<Long, ProductoDto> productos = inventario.consultarProductos(detallesPorVenta.values().stream()
                .flatMap(List::stream).map(DetalleVenta::getProductoId).distinct().toList()).stream()
                .collect(Collectors.toMap(ProductoDto::id, Function.identity()));
        return lista.stream()
                .map(v -> new VentaPendienteDto(v.getId(), nombres.get(v.getClienteId()), v.getDireccionEntrega(),
                        detallesPorVenta.getOrDefault(v.getId(), List.of()).stream()
                                .map(d -> new VentaPendienteDto.Producto(d.getProductoId(),
                                        productos.get(d.getProductoId()).nombre(), d.getCantidad()))
                                .toList()))
                .toList();
    }
}
