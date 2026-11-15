package polimarket.proveedores.dominio;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoSuministradoRepositorio extends JpaRepository<ProductoSuministrado, Long> {

    boolean existsByProveedorIdAndCodigoProducto(Long proveedorId, String codigoProducto);

    List<ProductoSuministrado> findByCodigoProducto(String codigoProducto);
}
