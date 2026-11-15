package polimarket.entregas.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import polimarket.entregas.api.EstadoEntrega;

/** Entrega de una venta. venta_id referencia a ventas.venta por identificador y es único. */
@Entity
@Table(schema = "entregas", name = "entrega")
public class Entrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "venta_id", nullable = false)
    private Long ventaId;

    @Column(name = "fecha_despacho", nullable = false)
    private Instant fechaDespacho;

    @Column(nullable = false)
    private String direccion;

    private String receptor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEntrega estado = EstadoEntrega.DESPACHADA;

    @Column(name = "responsable_id", nullable = false)
    private Long responsableId;

    protected Entrega() {
    }

    public Entrega(Long ventaId, Instant fechaDespacho, String direccion, String receptor, Long responsableId) {
        this.ventaId = ventaId;
        this.fechaDespacho = fechaDespacho;
        this.direccion = direccion;
        this.receptor = receptor;
        this.responsableId = responsableId;
    }

    public Long getId() { return id; }
    public Long getVentaId() { return ventaId; }
    public EstadoEntrega getEstado() { return estado; }
}
