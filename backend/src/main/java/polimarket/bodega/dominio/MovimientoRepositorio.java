package polimarket.bodega.dominio;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoRepositorio extends JpaRepository<MovimientoInventario, Long> {
}
