package polimarket.bodega;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import polimarket.bodega.dominio.Existencia;
import polimarket.compartido.ExcepcionNegocio;

/** Reglas de inventario sin Spring ni base de datos. Ejemplo del enunciado: físico 10, reservado 6. */
class ExistenciaTest {

    private static Existencia conFisico(int fisico, int minimo) {
        return new Existencia(Existencia.BODEGA_PRINCIPAL, 1L, fisico, minimo);
    }

    @Test
    void disponibleEsFisicoMenosReservado() {
        assertThat(conFisico(10, 5).disponible(6)).isEqualTo(4);
    }

    @Test
    void haySoloDisponibilidadParaLoQueNoEstaReservado() {
        Existencia e = conFisico(10, 5);

        assertThat(e.hayDisponibilidad(4, 6)).isTrue();
        assertThat(e.hayDisponibilidad(5, 6)).isFalse();
    }

    @Test
    void bajoStockSeMideSobreLoDisponibleNoSobreElFisico() {
        Existencia e = conFisico(10, 5);

        assertThat(e.bajoStock(0)).isFalse(); // disponible 10, mínimo 5
        assertThat(e.bajoStock(6)).isTrue();  // disponible 4 < 5
        assertThat(e.bajoStock(5)).isFalse(); // disponible 5 no es menor que 5
    }

    @Test
    void unaEntradaSumaAlFisico() {
        Existencia e = conFisico(10, 5);

        e.registrarEntrada(7);

        assertThat(e.getCantidad()).isEqualTo(17);
    }

    @Test
    void unaEntradaDebeSerPositiva() {
        assertThatThrownBy(() -> conFisico(10, 5).registrarEntrada(0)).isInstanceOf(ExcepcionNegocio.class);
    }

    @Test
    void unAjusteNoPuedeDejarElFisicoPorDebajoDeLasReservas() {
        Existencia e = conFisico(10, 5);

        assertThatThrownBy(() -> e.ajustarA(5, 6))
                .isInstanceOf(ExcepcionNegocio.class)
                .extracting(x -> ((ExcepcionNegocio) x).getCodigo())
                .isEqualTo("STOCK_INSUFICIENTE");
        assertThat(e.getCantidad()).isEqualTo(10); // no cambió
    }

    @Test
    void unAjusteNoPuedeDejarElFisicoNegativo() {
        assertThatThrownBy(() -> conFisico(10, 5).ajustarA(-1, 0)).isInstanceOf(ExcepcionNegocio.class);
    }

    @Test
    void unAjusteValidoFijaElNuevoFisico() {
        Existencia e = conFisico(10, 5);

        e.ajustarA(6, 6);

        assertThat(e.getCantidad()).isEqualTo(6);
    }
}
