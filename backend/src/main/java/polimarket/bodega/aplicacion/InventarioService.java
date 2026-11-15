package polimarket.bodega.aplicacion;

import static polimarket.bodega.dominio.Existencia.BODEGA_PRINCIPAL;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import polimarket.bodega.api.BajoStockDto;
import polimarket.bodega.api.CambioProducto;
import polimarket.bodega.api.DisponibilidadDto;
import polimarket.bodega.api.IInventario;
import polimarket.bodega.api.ItemReserva;
import polimarket.bodega.api.MovimientoDto;
import polimarket.bodega.api.NuevaEntrada;
import polimarket.bodega.api.NuevoAjuste;
import polimarket.bodega.api.NuevoProducto;
import polimarket.bodega.api.ProductoDto;
import polimarket.bodega.api.ReservaDto;
import polimarket.bodega.api.TipoMovimiento;
import polimarket.bodega.dominio.Compra;
import polimarket.bodega.dominio.CompraRepositorio;
import polimarket.bodega.dominio.DetalleCompra;
import polimarket.bodega.dominio.DetalleCompraRepositorio;
import polimarket.bodega.dominio.DetalleReserva;
import polimarket.bodega.dominio.DetalleReservaRepositorio;
import polimarket.bodega.dominio.Existencia;
import polimarket.bodega.dominio.ExistenciaRepositorio;
import polimarket.bodega.dominio.MovimientoInventario;
import polimarket.bodega.dominio.MovimientoRepositorio;
import polimarket.bodega.dominio.Producto;
import polimarket.bodega.dominio.ProductoRepositorio;
import polimarket.bodega.dominio.ReservaInventario;
import polimarket.bodega.dominio.ReservaRepositorio;
import polimarket.compartido.ExcepcionNegocio;
import polimarket.proveedores.api.IProveedores;

@Service
@Transactional
class InventarioService implements IInventario {

    private final ProductoRepositorio productos;
    private final ExistenciaRepositorio existencias;
    private final MovimientoRepositorio movimientos;
    private final CompraRepositorio compras;
    private final DetalleCompraRepositorio detallesCompra;
    private final ReservaRepositorio reservas;
    private final DetalleReservaRepositorio detallesReserva;
    private final IProveedores proveedores;
    private final Clock reloj;

    InventarioService(ProductoRepositorio productos, ExistenciaRepositorio existencias,
            MovimientoRepositorio movimientos, CompraRepositorio compras, DetalleCompraRepositorio detallesCompra,
            ReservaRepositorio reservas, DetalleReservaRepositorio detallesReserva, IProveedores proveedores,
            Clock reloj) {
        this.productos = productos;
        this.existencias = existencias;
        this.movimientos = movimientos;
        this.compras = compras;
        this.detallesCompra = detallesCompra;
        this.reservas = reservas;
        this.detallesReserva = detallesReserva;
        this.proveedores = proveedores;
        this.reloj = reloj;
    }

    // ---- Reservas

    @Override
    @Transactional(readOnly = true)
    public List<ProductoDto> consultarProductos(Collection<Long> productoIds) {
        Map<Long, Existencia> existenciaPorProducto = existenciasPorProducto();
        return productos.findAllById(productoIds).stream()
                .map(p -> aDto(p, existenciaPorProducto.get(p.getId())))
                .toList();
    }

    @Override
    public ReservaDto reservar(long ventaId, List<ItemReserva> items) {
        if (items == null || items.isEmpty()) {
            throw ExcepcionNegocio.datosInvalidos("La reserva debe incluir al menos un producto.");
        }
        // TreeMap: se bloquea siempre en orden de producto, así dos ventas no se bloquean entre sí (sin interbloqueo).
        Map<Long, Integer> pedido = new TreeMap<>();
        for (ItemReserva item : items) {
            if (item.cantidad() <= 0) {
                throw ExcepcionNegocio.datosInvalidos("Las cantidades deben ser mayores que cero.");
            }
            pedido.merge(item.productoId(), item.cantidad(), Integer::sum);
        }
        if (reservas.existsByVentaId(ventaId)) {
            throw ExcepcionNegocio.conflicto("RESERVA_EXISTENTE", "Esa venta ya tiene una reserva.");
        }
        // Se comprueba todo con las filas bloqueadas; si algo falla se lanza antes de guardar y la transacción se revierte.
        for (Map.Entry<Long, Integer> e : pedido.entrySet()) {
            Existencia existencia = bloquear(e.getKey());
            int reservado = (int) existencias.reservadoDe(e.getKey());
            if (!existencia.hayDisponibilidad(e.getValue(), reservado)) {
                String nombre = productos.findById(e.getKey()).map(Producto::getNombre).orElse("producto " + e.getKey());
                throw ExcepcionNegocio.conflicto("STOCK_INSUFICIENTE", "No hay unidades suficientes de " + nombre
                        + ": disponibles " + existencia.disponible(reservado) + ", solicitadas " + e.getValue() + ".");
            }
        }
        ReservaInventario reserva = reservas.save(new ReservaInventario(ventaId, Instant.now(reloj)));
        pedido.forEach((productoId, cantidad) -> detallesReserva.save(new DetalleReserva(reserva.getId(), productoId, cantidad)));
        return new ReservaDto(ventaId, reserva.getEstado());
    }

    @Override
    public ReservaDto liberarReserva(long ventaId) {
        ReservaInventario reserva = exigirReserva(ventaId);
        reserva.liberar();
        return new ReservaDto(ventaId, reserva.getEstado());
    }

    @Override
    public ReservaDto consumirReserva(long ventaId, long usuarioId) {
        ReservaInventario reserva = exigirReserva(ventaId);
        reserva.consumir();
        Instant ahora = Instant.now(reloj);
        for (DetalleReserva detalle : detallesReserva.findByReservaIdOrderByProductoId(reserva.getId())) {
            Existencia existencia = bloquear(detalle.getProductoId());
            int anterior = existencia.getCantidad();
            existencia.retirar(detalle.getCantidad());
            movimientos.save(new MovimientoInventario(BODEGA_PRINCIPAL, detalle.getProductoId(), ahora,
                    TipoMovimiento.SALIDA, detalle.getCantidad(), anterior, existencia.getCantidad(), null,
                    "VENTA-" + ventaId, usuarioId, null));
        }
        return new ReservaDto(ventaId, reserva.getEstado());
    }

    private ReservaInventario exigirReserva(long ventaId) {
        return reservas.bloquearPorVenta(ventaId)
                .orElseThrow(() -> ExcepcionNegocio.noEncontrado("La venta no tiene reserva."));
    }

    // ---- Catálogo

    @Override
    public ProductoDto crearProducto(NuevoProducto n) {
        if (n == null || vacio(n.codigo()) || vacio(n.nombre()) || n.precioVenta() == null) {
            throw ExcepcionNegocio.datosInvalidos("Código, nombre y precio de venta son obligatorios.");
        }
        if (n.precioVenta().signum() < 0 || n.stockMinimo() < 0) {
            throw ExcepcionNegocio.datosInvalidos("El precio y el stock mínimo no pueden ser negativos.");
        }
        String codigo = n.codigo().trim().toUpperCase();
        if (productos.existsByCodigo(codigo)) {
            throw ExcepcionNegocio.conflicto("CODIGO_DUPLICADO", "Ya existe un producto con ese código.");
        }
        Producto p = productos.save(new Producto(codigo, n.nombre().trim(), limpiar(n.descripcion()),
                limpiar(n.categoria()), n.precioVenta()));
        Existencia e = existencias.save(new Existencia(BODEGA_PRINCIPAL, p.getId(), 0, n.stockMinimo()));
        return aDto(p, e);
    }

    @Override
    public ProductoDto actualizarProducto(long productoId, CambioProducto c) {
        Producto p = exigirProducto(productoId);
        if (c.precioVenta() != null && c.precioVenta().signum() < 0) {
            throw ExcepcionNegocio.datosInvalidos("El precio no puede ser negativo.");
        }
        p.actualizar(limpiar(c.nombre()), limpiar(c.descripcion()), limpiar(c.categoria()), c.precioVenta());
        Existencia e = exigirExistencia(productoId);
        if (c.stockMinimo() != null) {
            e.cambiarStockMinimo(c.stockMinimo());
        }
        return aDto(p, e);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductoDto> consultarProducto(long productoId) {
        return productos.findById(productoId).map(p -> aDto(p, exigirExistencia(productoId)));
    }

    // ---- Disponibilidad (RSI-2) y bajo stock (RSI-4)

    @Override
    @Transactional(readOnly = true)
    public List<DisponibilidadDto> listarDisponibilidad() {
        Map<Long, Existencia> existenciaPorProducto = existenciasPorProducto();
        Map<Long, Integer> reservado = reservadoPorProducto();
        return productos.findByActivoTrueOrderByNombre().stream()
                .map(p -> disponibilidad(p, existenciaPorProducto.get(p.getId()), reservado.getOrDefault(p.getId(), 0)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DisponibilidadDto consultarDisponibilidad(long productoId) {
        Producto p = exigirProducto(productoId);
        return disponibilidad(p, exigirExistencia(productoId), (int) existencias.reservadoDe(productoId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BajoStockDto> listarBajoStock() {
        Map<Long, Existencia> existenciaPorProducto = existenciasPorProducto();
        Map<Long, Integer> reservado = reservadoPorProducto();
        return productos.findByActivoTrueOrderByNombre().stream()
                .filter(p -> existenciaPorProducto.get(p.getId()).bajoStock(reservado.getOrDefault(p.getId(), 0)))
                .map(p -> {
                    Existencia e = existenciaPorProducto.get(p.getId());
                    int res = reservado.getOrDefault(p.getId(), 0);
                    List<BajoStockDto.Proveedor> quienes = proveedores.consultarPorProducto(p.getCodigo()).stream()
                            .map(pr -> new BajoStockDto.Proveedor(pr.id(), pr.nombre())).toList();
                    return new BajoStockDto(p.getId(), p.getNombre(), e.getCantidad(), res, e.disponible(res),
                            e.getStockMinimo(), quienes);
                })
                .toList();
    }

    // ---- Movimientos

    @Override
    public MovimientoDto registrarEntrada(NuevaEntrada n, long usuarioId) {
        if (n.cantidad() <= 0) {
            throw ExcepcionNegocio.datosInvalidos("La cantidad de una entrada debe ser mayor que cero.");
        }
        BigDecimal costo = n.costoUnitario() == null ? BigDecimal.ZERO : n.costoUnitario();
        if (costo.signum() < 0) {
            throw ExcepcionNegocio.datosInvalidos("El costo unitario no puede ser negativo.");
        }
        exigirProducto(n.productoId());
        if (proveedores.consultarPorId(n.proveedorId()).isEmpty()) {
            throw ExcepcionNegocio.noEncontrado("No se encontró el proveedor.");
        }
        Existencia e = bloquear(n.productoId());
        int anterior = e.getCantidad();
        e.registrarEntrada(n.cantidad());

        Instant ahora = Instant.now(reloj);
        Compra compra = compras.save(new Compra(BODEGA_PRINCIPAL, n.proveedorId(), ahora, limpiar(n.referencia()),
                costo.multiply(BigDecimal.valueOf(n.cantidad()))));
        detallesCompra.save(new DetalleCompra(compra.getId(), n.productoId(), n.cantidad(), costo));
        MovimientoInventario m = movimientos.save(new MovimientoInventario(BODEGA_PRINCIPAL, n.productoId(), ahora,
                TipoMovimiento.ENTRADA, n.cantidad(), anterior, e.getCantidad(), null, limpiar(n.referencia()),
                usuarioId, compra.getId()));
        return aDto(m);
    }

    @Override
    public MovimientoDto registrarAjuste(NuevoAjuste n, long usuarioId) {
        if (vacio(n.motivo())) {
            throw ExcepcionNegocio.datosInvalidos("El motivo del ajuste es obligatorio.");
        }
        exigirProducto(n.productoId());
        Existencia e = bloquear(n.productoId());
        int anterior = e.getCantidad();
        if (n.nuevoStockFisico() == anterior) {
            throw ExcepcionNegocio.datosInvalidos("El ajuste no cambia el stock físico.");
        }
        e.ajustarA(n.nuevoStockFisico(), (int) existencias.reservadoDe(n.productoId()));
        MovimientoInventario m = movimientos.save(new MovimientoInventario(BODEGA_PRINCIPAL, n.productoId(),
                Instant.now(reloj), TipoMovimiento.AJUSTE, Math.abs(e.getCantidad() - anterior), anterior,
                e.getCantidad(), n.motivo().trim(), null, usuarioId, null));
        return aDto(m);
    }

    // ---- Apoyo

    private Producto exigirProducto(long productoId) {
        return productos.findById(productoId)
                .orElseThrow(() -> ExcepcionNegocio.noEncontrado("No se encontró el producto."));
    }

    private Existencia exigirExistencia(long productoId) {
        return existencias.findByBodegaIdAndProductoId(BODEGA_PRINCIPAL, productoId)
                .orElseThrow(() -> ExcepcionNegocio.noEncontrado("El producto no tiene existencia registrada."));
    }

    private Existencia bloquear(long productoId) {
        return existencias.bloquear(BODEGA_PRINCIPAL, productoId)
                .orElseThrow(() -> ExcepcionNegocio.noEncontrado("El producto no tiene existencia registrada."));
    }

    private Map<Long, Existencia> existenciasPorProducto() {
        return existencias.findByBodegaId(BODEGA_PRINCIPAL).stream()
                .collect(Collectors.toMap(Existencia::getProductoId, Function.identity()));
    }

    private Map<Long, Integer> reservadoPorProducto() {
        Map<Long, Integer> mapa = new HashMap<>();
        for (Object[] fila : existencias.reservadoPorProducto()) {
            mapa.put(((Number) fila[0]).longValue(), ((Number) fila[1]).intValue());
        }
        return mapa;
    }

    private static DisponibilidadDto disponibilidad(Producto p, Existencia e, int reservado) {
        return new DisponibilidadDto(p.getId(), p.getCodigo(), p.getNombre(), p.getPrecioVenta(), e.getCantidad(),
                reservado, e.disponible(reservado));
    }

    private static ProductoDto aDto(Producto p, Existencia e) {
        return new ProductoDto(p.getId(), p.getCodigo(), p.getNombre(), p.getDescripcion(), p.getCategoria(),
                p.getPrecioVenta(), e.getStockMinimo());
    }

    private static MovimientoDto aDto(MovimientoInventario m) {
        return new MovimientoDto(m.getId(), m.getProductoId(), m.getTipo(), m.getCantidad(), m.getCantidadAnterior(),
                m.getCantidadNueva());
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    private static String limpiar(String s) {
        return vacio(s) ? null : s.trim();
    }
}
