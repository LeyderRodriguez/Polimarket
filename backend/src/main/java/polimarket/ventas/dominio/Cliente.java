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
import polimarket.compartido.ExcepcionNegocio;

@Entity
@Table(schema = "ventas", name = "cliente")
public class Cliente {

    private static final String SEPARADOR = ";";
    private static final int LARGO_MAXIMO = 500; // largo de la columna

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

    /** Reemplaza las preferencias. Se guardan unidas por «;», así que ninguna puede contener ese carácter. */
    public void cambiarPreferencias(List<String> nuevas) {
        List<String> limpias = nuevas == null ? List.of()
                : nuevas.stream().filter(p -> p != null).map(String::trim).filter(p -> !p.isEmpty()).toList();
        if (limpias.stream().anyMatch(p -> p.contains(SEPARADOR))) {
            throw ExcepcionNegocio.datosInvalidos("Una preferencia no puede contener el carácter «;».");
        }
        String unidas = String.join(SEPARADOR, limpias);
        if (unidas.length() > LARGO_MAXIMO) {
            throw ExcepcionNegocio.datosInvalidos("Las preferencias son demasiado largas (máximo " + LARGO_MAXIMO + " caracteres).");
        }
        this.preferencias = unidas.isEmpty() ? null : unidas;
    }

    public List<String> getPreferencias() {
        return preferencias == null || preferencias.isBlank() ? List.of() : Arrays.asList(preferencias.split(SEPARADOR));
    }

    public Long getId() { return id; }
    public Long getTerceroId() { return terceroId; }
}
