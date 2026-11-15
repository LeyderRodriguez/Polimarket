package polimarket.recursoshumanos.dominio;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VendedorRepositorio extends JpaRepository<Vendedor, Long> {

    Optional<Vendedor> findByEmpleadoId(Long empleadoId);

    /** Bloquea al vendedor: dos RH que resuelven su aval a la vez se atienden una tras otra. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vendedor v where v.id = :id")
    Optional<Vendedor> bloquear(@Param("id") Long id);

    boolean existsByEmpleadoId(Long empleadoId);
}
