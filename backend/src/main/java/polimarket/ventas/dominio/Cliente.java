package polimarket.ventas.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(schema = "ventas", name = "cliente")
public class Cliente {

    private static final String SEPARADOR = ";";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tercero_id", nullable = false)
    private Long terceroId;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDate fechaRegistro;

    private String preferencias;

    protected Cliente() {
    }

    public Cliente(Long terceroId, LocalDate fechaRegistro, List<String> preferencias) {
        this.terceroId = terceroId;
        this.fechaRegistro = fechaRegistro;
        cambiarPreferencias(preferencias);
    }

    public void cambiarPreferencias(List<String> nuevas) {
        this.preferencias = nuevas == null || nuevas.isEmpty() ? null
                : String.join(SEPARADOR, nuevas.stream().map(String::trim).filter(s -> !s.isEmpty()).toList());
    }

    public List<String> getPreferencias() {
        return preferencias == null || preferencias.isBlank() ? List.of() : Arrays.asList(preferencias.split(SEPARADOR));
    }

    public Long getId() { return id; }
    public Long getTerceroId() { return terceroId; }
}
