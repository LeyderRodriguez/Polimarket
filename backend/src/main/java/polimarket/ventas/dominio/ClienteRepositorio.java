package polimarket.ventas.dominio;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepositorio extends JpaRepository<Cliente, Long> {

    boolean existsByTerceroId(Long terceroId);
}
