package polimarket.recursoshumanos.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import polimarket.compartido.ExcepcionNegocio;
import polimarket.recursoshumanos.EstadoAval;

/** Un aval de RH sobre un vendedor. Un vendedor puede acumular varios con el tiempo. */
@Entity
@Table(schema = "recursos_humanos", name = "aval_rh")
public class AvalRH {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vendedor_id", nullable = false)
    private Long vendedorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoAval estado;

    @Column(name = "fecha_solicitud", nullable = false)
    private Instant fechaSolicitud;

    @Column(name = "fecha_respuesta")
    private Instant fechaRespuesta;

    @Column(name = "responsable_id")
    private Long responsableId;

    private String observacion;

    protected AvalRH() {
    }

    public static AvalRH solicitar(Long vendedorId, Instant ahora) {
        AvalRH aval = new AvalRH();
        aval.vendedorId = vendedorId;
        aval.estado = EstadoAval.PENDIENTE;
        aval.fechaSolicitud = ahora;
        return aval;
    }

    /** Resuelve un aval pendiente. Un aval ya resuelto no se modifica: se abre otro. */
    public void resolver(EstadoAval destino, Long responsableId, String observacion, Instant ahora) {
        if (destino == EstadoAval.PENDIENTE) {
            throw ExcepcionNegocio.datosInvalidos("Un aval solo se resuelve como APROBADO o RECHAZADO.");
        }
        if (estado != EstadoAval.PENDIENTE) {
            throw ExcepcionNegocio.conflicto("ESTADO_INVALIDO", "El aval ya fue resuelto.");
        }
        this.estado = destino;
        this.responsableId = responsableId;
        this.observacion = observacion;
        this.fechaRespuesta = ahora;
    }

    public Long getId() { return id; }
    public Long getVendedorId() { return vendedorId; }
    public EstadoAval getEstado() { return estado; }
    public Long getResponsableId() { return responsableId; }
    public String getObservacion() { return observacion; }
    public Instant getFechaRespuesta() { return fechaRespuesta; }
}
