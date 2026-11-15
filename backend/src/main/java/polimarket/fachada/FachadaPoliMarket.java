package polimarket.fachada;

import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import polimarket.bodega.api.BajoStockDto;
import polimarket.bodega.api.CambioProducto;
import polimarket.bodega.api.DisponibilidadDto;
import polimarket.bodega.api.IInventario;
import polimarket.bodega.api.MovimientoDto;
import polimarket.bodega.api.NuevaEntrada;
import polimarket.bodega.api.NuevoAjuste;
import polimarket.bodega.api.NuevoProducto;
import polimarket.bodega.api.ProductoDto;
import polimarket.compartido.ExcepcionNegocio;
import polimarket.entregas.api.EntregaDto;
import polimarket.entregas.api.IEntregas;
import polimarket.proveedores.api.CambioProveedor;
import polimarket.proveedores.api.IProveedores;
import polimarket.proveedores.api.NuevoProveedor;
import polimarket.proveedores.api.ProveedorDto;
import polimarket.recursoshumanos.api.AvalDto;
import polimarket.recursoshumanos.api.IAutorizacion;
import polimarket.recursoshumanos.api.IGestionPersonal;
import polimarket.recursoshumanos.api.VendedorDto;
import polimarket.seguridad.api.IAutenticacion;
import polimarket.seguridad.api.NuevoUsuario;
import polimarket.seguridad.api.RolUsuario;
import polimarket.seguridad.api.UsuarioDto;
import polimarket.seguridad.api.UsuarioSesion;
import polimarket.terceros.api.ITerceros;
import polimarket.terceros.api.NuevoTercero;
import polimarket.terceros.api.TerceroDto;
import polimarket.ventas.api.ClienteDto;
import polimarket.ventas.api.IClientes;
import polimarket.ventas.api.IVentas;
import polimarket.ventas.api.NuevaVenta;
import polimarket.ventas.api.VentaDto;
import polimarket.ventas.api.VentaPendienteDto;
import polimarket.ventas.api.VentaResumenDto;

/**
 * Fachada (patrón Facade): único punto de entrada para los clientes. No contiene reglas de
 * negocio de un componente; las delega en sus interfaces y coordina operaciones que cruzan
 * varios componentes, como crear una cuenta.
 */
@Service
public class FachadaPoliMarket {

    private static final Set<RolUsuario> ROLES_QUE_RH_PUEDE_ASIGNAR =
            Set.of(RolUsuario.VENDEDOR, RolUsuario.BODEGA, RolUsuario.ENTREGAS);

    private final ITerceros terceros;
    private final IAutenticacion autenticacion;
    private final IGestionPersonal personal;
    private final IAutorizacion autorizacion;
    private final IInventario inventario;
    private final IProveedores proveedores;
    private final IClientes clientes;
    private final IVentas ventas;
    private final IEntregas entregas;

    public FachadaPoliMarket(ITerceros terceros, IAutenticacion autenticacion, IGestionPersonal personal,
            IAutorizacion autorizacion, IInventario inventario, IProveedores proveedores, IClientes clientes,
            IVentas ventas, IEntregas entregas) {
        this.terceros = terceros;
        this.autenticacion = autenticacion;
        this.personal = personal;
        this.autorizacion = autorizacion;
        this.inventario = inventario;
        this.proveedores = proveedores;
        this.clientes = clientes;
        this.ventas = ventas;
        this.entregas = entregas;
    }

    // ---- Sesión

    @Transactional(readOnly = true)
    public PerfilDto perfil(UsuarioSesion usuario) {
        String nombre = terceros.consultarPorId(usuario.getTerceroId())
                .map(TerceroDto::nombreCompleto)
                .orElse(usuario.getUsername());
        return new PerfilDto(usuario.getId(), nombre, usuario.getRol());
    }

    // ---- Terceros

    @Transactional(readOnly = true)
    public TerceroDto buscarTercero(UsuarioSesion usuario, String tipoDocumento, String numeroDocumento) {
        exigirAvalSiEsVendedor(usuario);
        return terceros.consultarPorDocumento(tipoDocumento, numeroDocumento)
                .orElseThrow(() -> ExcepcionNegocio.noEncontrado("No se encontró el tercero."));
    }

    @Transactional
    public TerceroDto registrarTercero(UsuarioSesion usuario, NuevoTercero tercero) {
        exigirAvalSiEsVendedor(usuario);
        return terceros.registrar(tercero);
    }

    // ---- Cuentas

    /**
     * Crea la cuenta de un tercero existente. Si el rol es VENDEDOR registra también al vendedor con
     * aval PENDIENTE; aprobarlo es un paso aparte. Todo ocurre en una transacción: o se crea completo o no se crea.
     */
    @Transactional
    public CuentaCreadaDto registrarCuenta(long terceroId, String correo, String contrasena, RolUsuario rol) {
        if (rol == null || !ROLES_QUE_RH_PUEDE_ASIGNAR.contains(rol)) {
            throw ExcepcionNegocio.datosInvalidos("Recursos Humanos solo puede asignar los roles VENDEDOR, BODEGA o ENTREGAS.");
        }
        if (terceros.consultarPorId(terceroId).isEmpty()) {
            throw ExcepcionNegocio.noEncontrado("No se encontró el tercero.");
        }
        UsuarioDto cuenta = autenticacion.crearUsuario(new NuevoUsuario(terceroId, correo, contrasena, rol));
        return switch (rol) {
            case VENDEDOR -> {
                VendedorDto vendedor = personal.registrarVendedor(terceroId, null);
                yield conVendedor(cuenta, vendedor);
            }
            case BODEGA -> {
                personal.registrarEmpleado(terceroId, "Bodega");
                yield conVendedor(cuenta, null);
            }
            case ENTREGAS -> {
                personal.registrarEmpleado(terceroId, "Entregas");
                yield conVendedor(cuenta, null);
            }
            default -> throw new IllegalStateException("Rol no permitido: " + rol);
        };
    }

    // ---- Vendedores y aval

    @Transactional(readOnly = true)
    public List<VendedorDto> listarVendedores() {
        return personal.listarVendedores();
    }

    @Transactional(readOnly = true)
    public AvalDto consultarAutorizacion(UsuarioSesion usuario, long vendedorId) {
        if (usuario.getRol() == RolUsuario.VENDEDOR) {
            boolean esSuyo = personal.consultarVendedorPorTercero(usuario.getTerceroId())
                    .map(v -> v.id() == vendedorId)
                    .orElse(false);
            if (!esSuyo) {
                throw ExcepcionNegocio.sinPermiso("SIN_PERMISO", "Solo puede consultar su propio aval.");
            }
        }
        return autorizacion.consultarAval(vendedorId);
    }

    @Transactional
    public AvalDto aprobarAval(UsuarioSesion responsable, long vendedorId, String observacion) {
        return autorizacion.aprobar(vendedorId, responsable.getTerceroId(), observacion);
    }

    @Transactional
    public AvalDto rechazarAval(UsuarioSesion responsable, long vendedorId, String observacion) {
        return autorizacion.rechazar(vendedorId, responsable.getTerceroId(), observacion);
    }

    // ---- Productos y disponibilidad (RSI-2)

    /** Quien consulta disponibilidad: Bodega, o un vendedor con aval vigente. */
    @Transactional(readOnly = true)
    public List<DisponibilidadDto> listarDisponibilidad(UsuarioSesion usuario) {
        exigirAvalSiEsVendedor(usuario);
        return inventario.listarDisponibilidad();
    }

    @Transactional
    public ProductoDto crearProducto(NuevoProducto producto) {
        return inventario.crearProducto(producto);
    }

    @Transactional
    public ProductoDto actualizarProducto(long productoId, CambioProducto cambio) {
        return inventario.actualizarProducto(productoId, cambio);
    }

    // ---- Inventario (RSI-4)

    @Transactional(readOnly = true)
    public List<BajoStockDto> listarBajoStock() {
        return inventario.listarBajoStock();
    }

    @Transactional
    public MovimientoDto registrarEntrada(UsuarioSesion usuario, NuevaEntrada entrada) {
        return inventario.registrarEntrada(entrada, usuario.getId());
    }

    @Transactional
    public MovimientoDto registrarAjuste(UsuarioSesion usuario, NuevoAjuste ajuste) {
        return inventario.registrarAjuste(ajuste, usuario.getId());
    }

    // ---- Proveedores

    @Transactional
    public ProveedorDto registrarProveedor(NuevoProveedor proveedor) {
        return proveedores.registrar(proveedor);
    }

    @Transactional
    public ProveedorDto actualizarProveedor(long proveedorId, CambioProveedor cambio) {
        return proveedores.actualizar(proveedorId, cambio);
    }

    @Transactional(readOnly = true)
    public List<ProveedorDto> listarProveedores() {
        return proveedores.listar();
    }

    /**
     * Asocia un producto de Bodega a un proveedor. Proveedores guarda el código del producto, así que la
     * fachada traduce el id a código: ninguno de los dos componentes conoce al otro.
     */
    @Transactional
    public void asociarProductoAProveedor(long proveedorId, long productoId) {
        ProductoDto producto = inventario.consultarProducto(productoId)
                .orElseThrow(() -> ExcepcionNegocio.noEncontrado("No se encontró el producto."));
        proveedores.asociarProducto(proveedorId, producto.codigo(), null);
    }

    // ---- Clientes (vendedor con aval)

    @Transactional(readOnly = true)
    public List<ClienteDto> listarClientes(UsuarioSesion usuario) {
        exigirAvalSiEsVendedor(usuario);
        return clientes.listarClientes();
    }

    @Transactional
    public ClienteDto registrarCliente(UsuarioSesion usuario, long terceroId, List<String> preferencias) {
        exigirAvalSiEsVendedor(usuario);
        return clientes.registrarCliente(terceroId, preferencias);
    }

    /** Actualiza las preferencias del cliente (Ventas) y/o los datos de contacto de su tercero (Terceros). */
    @Transactional
    public ClienteDto actualizarCliente(UsuarioSesion usuario, long clienteId, List<String> preferencias,
            String telefono, String correo, String direccion) {
        exigirAvalSiEsVendedor(usuario);
        ClienteDto cliente = clientes.consultarCliente(clienteId)
                .orElseThrow(() -> ExcepcionNegocio.noEncontrado("No se encontró el cliente."));
        if (telefono != null || correo != null || direccion != null) {
            terceros.actualizarContacto(cliente.terceroId(), telefono, correo, direccion);
        }
        return preferencias == null ? cliente : clientes.actualizarPreferencias(clienteId, preferencias);
    }

    // ---- Ventas y entregas (RSI-3, RSI-5, RSI-6)

    /** El vendedor sale de la sesión; el cliente no puede registrar una venta a nombre de otro. */
    @Transactional
    public VentaDto registrarVenta(UsuarioSesion usuario, NuevaVenta venta) {
        return ventas.registrar(vendedorDe(usuario).id(), venta);
    }

    @Transactional(readOnly = true)
    public List<VentaResumenDto> misVentas(UsuarioSesion usuario) {
        return ventas.listarVentasDelVendedor(vendedorDe(usuario).id());
    }

    @Transactional
    public VentaDto anularVenta(UsuarioSesion usuario, long ventaId) {
        return ventas.anular(ventaId, vendedorDe(usuario).id());
    }

    @Transactional(readOnly = true)
    public List<VentaPendienteDto> pendientesEntrega() {
        return entregas.listarPendientes();
    }

    @Transactional
    public EntregaDto despachar(UsuarioSesion usuario, long ventaId) {
        return entregas.despachar(ventaId, usuario.getId());
    }

    // ---- Apoyo

    private VendedorDto vendedorDe(UsuarioSesion usuario) {
        return personal.consultarVendedorPorTercero(usuario.getTerceroId())
                .orElseThrow(() -> ExcepcionNegocio.sinPermiso("SIN_PERMISO", "La cuenta no corresponde a un vendedor."));
    }

    /** Un vendedor sin aval vigente no puede usar las funciones de vendedor. Otros roles no se ven afectados. */
    private void exigirAvalSiEsVendedor(UsuarioSesion usuario) {
        if (usuario.getRol() != RolUsuario.VENDEDOR) {
            return;
        }
        boolean autorizado = personal.consultarVendedorPorTercero(usuario.getTerceroId())
                .map(v -> autorizacion.estaAutorizado(v.id()))
                .orElse(false);
        if (!autorizado) {
            throw ExcepcionNegocio.sinPermiso("VENDEDOR_SIN_AVAL", "El vendedor no tiene aval vigente.");
        }
    }

    private static CuentaCreadaDto conVendedor(UsuarioDto cuenta, VendedorDto vendedor) {
        return new CuentaCreadaDto(cuenta.id(), cuenta.terceroId(), cuenta.correo(), cuenta.rol(), cuenta.activo(),
                vendedor == null ? null : vendedor.id(), vendedor == null ? null : vendedor.estadoAval());
    }
}
