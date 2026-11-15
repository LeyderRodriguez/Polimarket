package polimarket.compartido;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Reloj inyectable: las reglas con fechas se pueden probar con un reloj fijo. */
@Configuration
class ConfiguracionReloj {

    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }
}
