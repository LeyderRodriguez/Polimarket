package polimarket.bodega.dominio;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepositorio extends JpaRepository<ReservaInventario, Long> {

    boolean existsByVentaId(Long ventaId);

    /** Bloquea la reserva: dos solicitudes sobre la misma venta (liberar, consumir) se atienden una tras otra. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReservaInventario r where r.ventaId = :ventaId")
    Optional<ReservaInventario> bloquearPorVenta(@Param("ventaId") Long ventaId);
}
