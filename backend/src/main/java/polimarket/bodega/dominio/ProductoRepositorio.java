package polimarket.bodega.dominio;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepositorio extends JpaRepository<Producto, Long> {

    boolean existsByCodigo(String codigo);

    List<Producto> findByActivoTrueOrderByNombre();
}
