package polimarket.terceros.dominio;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TerceroRepositorio extends JpaRepository<Tercero, Long> {

    Optional<Tercero> findByTipoDocumentoAndNumeroDocumento(String tipoDocumento, String numeroDocumento);

    boolean existsByTipoDocumentoAndNumeroDocumento(String tipoDocumento, String numeroDocumento);
}
