package polimarket.proveedores.dominio;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorRepositorio extends JpaRepository<Proveedor, Long> {

    boolean existsByTerceroId(Long terceroId);
}
