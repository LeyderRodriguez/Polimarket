package polimarket.terceros.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Contrato del componente Terceros: datos comunes de personas y empresas, sin duplicados. */
public interface ITerceros {

    /** Registra un tercero. Lanza DOCUMENTO_DUPLICADO si ya existe ese documento. */
    TerceroDto registrar(NuevoTercero tercero);

    /** Actualiza solo los datos de contacto. Un dato nulo se deja como estaba. */
    TerceroDto actualizarContacto(long id, String telefono, String correo, String direccion);

    Optional<TerceroDto> consultarPorId(long id);

    List<TerceroDto> consultarPorIds(Collection<Long> ids);

    Optional<TerceroDto> consultarPorDocumento(String tipoDocumento, String numeroDocumento);

    /** Asigna un rol de negocio al tercero. Si ya lo tiene, no hace nada. */
    void asignarRol(long terceroId, TipoRolTercero rol);

    List<TerceroDto> consultarPorRol(TipoRolTercero rol);
}
