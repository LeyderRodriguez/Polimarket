package polimarket.terceros.aplicacion;

import static polimarket.compartido.Textos.limpiar;
import static polimarket.compartido.Textos.vacio;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import polimarket.compartido.ExcepcionNegocio;
import polimarket.terceros.api.ITerceros;
import polimarket.terceros.api.NuevoTercero;
import polimarket.terceros.api.TerceroDto;
import polimarket.terceros.api.TipoRolTercero;
import polimarket.terceros.api.TipoTercero;
import polimarket.terceros.dominio.RolTercero;
import polimarket.terceros.dominio.RolTerceroRepositorio;
import polimarket.terceros.dominio.Tercero;
import polimarket.terceros.dominio.TerceroRepositorio;

@Service
@Transactional
class TercerosService implements ITerceros {

    private final TerceroRepositorio terceros;
    private final RolTerceroRepositorio roles;
    private final Clock reloj;

    TercerosService(TerceroRepositorio terceros, RolTerceroRepositorio roles, Clock reloj) {
        this.terceros = terceros;
        this.roles = roles;
        this.reloj = reloj;
    }

    @Override
    public TerceroDto registrar(NuevoTercero n) {
        validar(n);
        String tipoDocumento = n.tipoDocumento().trim().toUpperCase();
        String numeroDocumento = n.numeroDocumento().trim();
        if (terceros.existsByTipoDocumentoAndNumeroDocumento(tipoDocumento, numeroDocumento)) {
            throw ExcepcionNegocio.conflicto("DOCUMENTO_DUPLICADO", "Ya existe un tercero con ese documento.");
        }
        Tercero t = terceros.save(new Tercero(n.tipo(), tipoDocumento, numeroDocumento, limpiar(n.nombres()),
                limpiar(n.apellidos()), limpiar(n.razonSocial()), limpiar(n.telefono()), limpiar(n.correo()),
                limpiar(n.direccion())));
        return aDto(t);
    }

    @Override
    public TerceroDto actualizarContacto(long id, String telefono, String correo, String direccion) {
        Tercero t = terceros.findById(id).orElseThrow(() -> ExcepcionNegocio.noEncontrado("No se encontró el tercero."));
        t.actualizarContacto(limpiar(telefono), limpiar(correo), limpiar(direccion));
        return aDto(t);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TerceroDto> consultarPorId(long id) {
        return terceros.findById(id).map(TercerosService::aDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TerceroDto> consultarPorIds(Collection<Long> ids) {
        return terceros.findAllById(ids).stream().map(TercerosService::aDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TerceroDto> consultarPorDocumento(String tipoDocumento, String numeroDocumento) {
        if (tipoDocumento == null || numeroDocumento == null) {
            return Optional.empty();
        }
        return terceros.findByTipoDocumentoAndNumeroDocumento(tipoDocumento.trim().toUpperCase(), numeroDocumento.trim())
                .map(TercerosService::aDto);
    }

    @Override
    public void asignarRol(long terceroId, TipoRolTercero rol) {
        if (!terceros.existsById(terceroId)) {
            throw ExcepcionNegocio.noEncontrado("No se encontró el tercero.");
        }
        if (!roles.existsByTerceroIdAndTipoRol(terceroId, rol)) {
            roles.save(new RolTercero(terceroId, rol, LocalDate.now(reloj)));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TerceroDto> consultarPorRol(TipoRolTercero rol) {
        List<Long> ids = roles.findByTipoRolAndActivoTrue(rol).stream().map(RolTercero::getTerceroId).toList();
        return consultarPorIds(ids);
    }

    private static void validar(NuevoTercero n) {
        if (n == null || n.tipo() == null || vacio(n.tipoDocumento()) || vacio(n.numeroDocumento())) {
            throw ExcepcionNegocio.datosInvalidos("Tipo, tipo de documento y número de documento son obligatorios.");
        }
        if (n.tipo() == TipoTercero.NATURAL && (vacio(n.nombres()) || vacio(n.apellidos()))) {
            throw ExcepcionNegocio.datosInvalidos("Una persona natural requiere nombres y apellidos.");
        }
        if (n.tipo() == TipoTercero.JURIDICA && vacio(n.razonSocial())) {
            throw ExcepcionNegocio.datosInvalidos("Una persona jurídica requiere razón social.");
        }
    }

    private static TerceroDto aDto(Tercero t) {
        return new TerceroDto(t.getId(), t.getTipo(), t.getTipoDocumento(), t.getNumeroDocumento(), t.getNombres(),
                t.getApellidos(), t.getRazonSocial(), t.getTelefono(), t.getCorreo(), t.getDireccion());
    }
}
