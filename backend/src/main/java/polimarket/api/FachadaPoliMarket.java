package polimarket.api;

import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import polimarket.compartido.ExcepcionNegocio;
import polimarket.recursoshumanos.AvalDto;
import polimarket.recursoshumanos.IAutorizacion;
import polimarket.recursoshumanos.IGestionPersonal;
import polimarket.recursoshumanos.VendedorDto;
import polimarket.seguridad.IAutenticacion;
import polimarket.seguridad.NuevoUsuario;
import polimarket.seguridad.RolUsuario;
import polimarket.seguridad.UsuarioDto;
import polimarket.seguridad.UsuarioSesion;
import polimarket.terceros.ITerceros;
import polimarket.terceros.NuevoTercero;
import polimarket.terceros.TerceroDto;

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

    public FachadaPoliMarket(ITerceros terceros, IAutenticacion autenticacion, IGestionPersonal personal,
            IAutorizacion autorizacion) {
        this.terceros = terceros;
        this.autenticacion = autenticacion;
        this.personal = personal;
        this.autorizacion = autorizacion;
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

    // ---- Apoyo

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
