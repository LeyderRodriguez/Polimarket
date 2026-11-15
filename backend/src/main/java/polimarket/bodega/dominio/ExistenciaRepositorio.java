package polimarket.bodega.dominio;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExistenciaRepositorio extends JpaRepository<Existencia, Long> {

    List<Existencia> findByBodegaId(Long bodegaId);

    Optional<Existencia> findByBodegaIdAndProductoId(Long bodegaId, Long productoId);

    /** Bloquea la fila hasta terminar la transacción: dos operaciones sobre el mismo producto se atienden una tras otra. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Existencia e where e.bodegaId = :bodegaId and e.productoId = :productoId")
    Optional<Existencia> bloquear(@Param("bodegaId") Long bodegaId, @Param("productoId") Long productoId);

    /** Unidades reservadas por ventas pendientes (reservas ACTIVAS) de un producto. */
    @Query(value = """
            select coalesce(sum(d.cantidad), 0)
            from bodega.detalle_reserva d
            join bodega.reserva_inventario r on r.id = d.reserva_id
            where r.estado = 'ACTIVA' and d.producto_id = :productoId
            """, nativeQuery = true)
    long reservadoDe(@Param("productoId") Long productoId);

    /** Pares [producto_id, unidades reservadas] de todos los productos con reservas ACTIVAS. */
    @Query(value = """
            select d.producto_id, sum(d.cantidad)
            from bodega.detalle_reserva d
            join bodega.reserva_inventario r on r.id = d.reserva_id
            where r.estado = 'ACTIVA'
            group by d.producto_id
            """, nativeQuery = true)
    List<Object[]> reservadoPorProducto();
}
