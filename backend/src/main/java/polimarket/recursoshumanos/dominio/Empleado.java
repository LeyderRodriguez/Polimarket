package polimarket.recursoshumanos.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import polimarket.recursoshumanos.EstadoEmpleado;

@Entity
@Table(schema = "recursos_humanos", name = "empleado")
public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tercero_id", nullable = false)
    private Long terceroId;

    @Column(nullable = false)
    private String cargo;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDate fechaIngreso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEmpleado estado = EstadoEmpleado.ACTIVO;

    protected Empleado() {
    }

    public Empleado(Long terceroId, String cargo, LocalDate fechaIngreso) {
        this.terceroId = terceroId;
        this.cargo = cargo;
        this.fechaIngreso = fechaIngreso;
    }

    public Long getId() { return id; }
    public Long getTerceroId() { return terceroId; }
    public String getCargo() { return cargo; }
    public EstadoEmpleado getEstado() { return estado; }
}
