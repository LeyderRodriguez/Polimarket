package polimarket.recursoshumanos.dominio;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpleadoRepositorio extends JpaRepository<Empleado, Long> {

    Optional<Empleado> findByTerceroId(Long terceroId);
}
