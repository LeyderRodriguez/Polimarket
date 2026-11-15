package polimarket.recursoshumanos.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(schema = "recursos_humanos", name = "vendedor")
public class Vendedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empleado_id", nullable = false)
    private Long empleadoId;

    private String zona;

    protected Vendedor() {
    }

    public Vendedor(Long empleadoId, String zona) {
        this.empleadoId = empleadoId;
        this.zona = zona;
    }

    public Long getId() { return id; }
    public Long getEmpleadoId() { return empleadoId; }
    public String getZona() { return zona; }

    /** Código derivado del identificador, por ejemplo VEN-0001. */
    public String getCodigo() {
        return String.format("VEN-%04d", id);
    }
}
