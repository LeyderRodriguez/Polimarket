package polimarket.ventas.dominio;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleVentaRepositorio extends JpaRepository<DetalleVenta, Long> {

    List<DetalleVenta> findByVentaId(Long ventaId);

    List<DetalleVenta> findByVentaIdIn(Collection<Long> ventaIds);
}
