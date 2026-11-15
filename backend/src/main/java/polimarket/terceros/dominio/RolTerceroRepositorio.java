package polimarket.terceros.dominio;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import polimarket.terceros.api.TipoRolTercero;

public interface RolTerceroRepositorio extends JpaRepository<RolTercero, Long> {

    boolean existsByTerceroIdAndTipoRol(Long terceroId, TipoRolTercero tipoRol);

    List<RolTercero> findByTipoRolAndActivoTrue(TipoRolTercero tipoRol);
}
