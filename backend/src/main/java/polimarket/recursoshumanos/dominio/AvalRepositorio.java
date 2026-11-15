package polimarket.recursoshumanos.dominio;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvalRepositorio extends JpaRepository<AvalRH, Long> {

    /** El aval vigente de un vendedor es el último que se registró. */
    Optional<AvalRH> findFirstByVendedorIdOrderByIdDesc(Long vendedorId);
}
