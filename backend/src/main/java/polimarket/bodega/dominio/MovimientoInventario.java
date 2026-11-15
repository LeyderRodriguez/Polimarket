package polimarket.bodega.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import polimarket.bodega.api.TipoMovimiento;

@Entity
@Table(schema = "bodega", name = "movimiento_inventario")
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bodega_id", nullable = false)
    private Long bodegaId;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(nullable = false)
    private Instant fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimiento tipo;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "cantidad_anterior", nullable = false)
    private int cantidadAnterior;

    @Column(name = "cantidad_nueva", nullable = false)
    private int cantidadNueva;

    private String motivo;
    private String referencia;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "compra_id")
    private Long compraId;

    protected MovimientoInventario() {
    }

    public MovimientoInventario(Long bodegaId, Long productoId, Instant fecha, TipoMovimiento tipo, int cantidad,
            int cantidadAnterior, int cantidadNueva, String motivo, String referencia, Long usuarioId, Long compraId) {
        this.bodegaId = bodegaId;
        this.productoId = productoId;
        this.fecha = fecha;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.cantidadAnterior = cantidadAnterior;
        this.cantidadNueva = cantidadNueva;
        this.motivo = motivo;
        this.referencia = referencia;
        this.usuarioId = usuarioId;
        this.compraId = compraId;
    }

    public Long getId() { return id; }
    public Long getProductoId() { return productoId; }
    public TipoMovimiento getTipo() { return tipo; }
    public int getCantidad() { return cantidad; }
    public int getCantidadAnterior() { return cantidadAnterior; }
    public int getCantidadNueva() { return cantidadNueva; }
}
