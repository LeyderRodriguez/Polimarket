package polimarket.terceros.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import polimarket.terceros.TipoRolTercero;

@Entity
@Table(schema = "terceros", name = "rol_tercero")
public class RolTercero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tercero_id", nullable = false)
    private Long terceroId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_rol", nullable = false)
    private TipoRolTercero tipoRol;

    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDate fechaAsignacion;

    @Column(nullable = false)
    private boolean activo = true;

    protected RolTercero() {
    }

    public RolTercero(Long terceroId, TipoRolTercero tipoRol, LocalDate fechaAsignacion) {
        this.terceroId = terceroId;
        this.tipoRol = tipoRol;
        this.fechaAsignacion = fechaAsignacion;
    }

    public Long getTerceroId() { return terceroId; }
    public TipoRolTercero getTipoRol() { return tipoRol; }
    public boolean isActivo() { return activo; }
}
