package polimarket.recursoshumanos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import polimarket.compartido.ExcepcionNegocio;
import polimarket.recursoshumanos.api.EstadoAval;
import polimarket.recursoshumanos.dominio.AvalRH;

/** Prueba unitaria de la regla del aval: no necesita Spring ni base de datos. */
class AvalRHTest {

    private static final Instant AHORA = Instant.parse("2026-11-14T12:00:00Z");

    @Test
    void unAvalNuevoEmpiezaPendiente() {
        AvalRH aval = AvalRH.solicitar(1L, AHORA);

        assertThat(aval.getEstado()).isEqualTo(EstadoAval.PENDIENTE);
        assertThat(aval.getFechaRespuesta()).isNull();
    }

    @Test
    void resolverGuardaResponsableObservacionYFecha() {
        AvalRH aval = AvalRH.solicitar(1L, AHORA);

        aval.resolver(EstadoAval.APROBADO, 7L, "Documentación revisada", AHORA);

        assertThat(aval.getEstado()).isEqualTo(EstadoAval.APROBADO);
        assertThat(aval.getResponsableId()).isEqualTo(7L);
        assertThat(aval.getObservacion()).isEqualTo("Documentación revisada");
        assertThat(aval.getFechaRespuesta()).isEqualTo(AHORA);
    }

    @Test
    void unAvalYaResueltoNoSeModifica() {
        AvalRH aval = AvalRH.solicitar(1L, AHORA);
        aval.resolver(EstadoAval.APROBADO, 7L, null, AHORA);

        assertThatThrownBy(() -> aval.resolver(EstadoAval.RECHAZADO, 7L, null, AHORA))
                .isInstanceOf(ExcepcionNegocio.class)
                .extracting(e -> ((ExcepcionNegocio) e).getCodigo())
                .isEqualTo("ESTADO_INVALIDO");
    }

    @Test
    void noSeResuelveComoPendiente() {
        AvalRH aval = AvalRH.solicitar(1L, AHORA);

        assertThatThrownBy(() -> aval.resolver(EstadoAval.PENDIENTE, 7L, null, AHORA))
                .isInstanceOf(ExcepcionNegocio.class);
    }
}
