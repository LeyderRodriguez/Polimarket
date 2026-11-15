package polimarket.recursoshumanos.dominio;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendedorRepositorio extends JpaRepository<Vendedor, Long> {

    Optional<Vendedor> findByEmpleadoId(Long empleadoId);

    boolean existsByEmpleadoId(Long empleadoId);
}
