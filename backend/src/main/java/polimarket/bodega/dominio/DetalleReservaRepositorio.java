package polimarket.bodega.dominio;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleReservaRepositorio extends JpaRepository<DetalleReserva, Long> {

    List<DetalleReserva> findByReservaIdOrderByProductoId(Long reservaId);
}
