package polimarket.ventas.dominio;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import polimarket.ventas.api.EstadoVenta;

public interface VentaRepositorio extends JpaRepository<Venta, Long> {

    List<Venta> findByVendedorIdOrderByIdDesc(Long vendedorId);

    List<Venta> findByEstadoOrderById(EstadoVenta estado);

    /** Bloquea la venta: anular y despachar a la vez se atienden una tras otra y solo una gana. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Venta v where v.id = :id")
    Optional<Venta> bloquear(@Param("id") Long id);
}
