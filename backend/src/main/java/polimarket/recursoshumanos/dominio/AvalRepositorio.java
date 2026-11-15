package polimarket.recursoshumanos.dominio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AvalRepositorio extends JpaRepository<AvalRH, Long> {

    /** El aval vigente de un vendedor es el último que se registró. */
    Optional<AvalRH> findFirstByVendedorIdOrderByIdDesc(Long vendedorId);

    /** El último aval de cada uno de los vendedores dados, en una sola consulta. */
    @Query("select a from AvalRH a where a.id in "
            + "(select max(b.id) from AvalRH b where b.vendedorId in :vendedorIds group by b.vendedorId)")
    List<AvalRH> ultimosDe(@Param("vendedorIds") Collection<Long> vendedorIds);
}
