package polimarket.seguridad.infraestructura;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Sesión administrada por el servidor (cookie JSESSIONID) con protección CSRF por cookie
 * (XSRF-TOKEN / X-XSRF-TOKEN). Los permisos por rol se aplican con {@code @PreAuthorize}.
 */
@Configuration
@EnableMethodSecurity
class ConfiguracionSeguridad {

    @Bean
    PasswordEncoder codificadorDeClaves() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager administradorDeAutenticacion(UserDetailsService detalles, PasswordEncoder codificador) {
        DaoAuthenticationProvider proveedor = new DaoAuthenticationProvider(detalles);
        proveedor.setPasswordEncoder(codificador);
        return new ProviderManager(proveedor);
    }

    @Bean
    SecurityContextRepository repositorioDeContexto() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * Permite que el cliente web (Angular, otro origen) llame a la API con la cookie de sesión.
     * El origen se configura con {@code polimarket.cors.origenes}; el cliente C++ no necesita CORS.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource( // el nombre exacto lo busca Spring Security
            @Value("${polimarket.cors.origenes:http://localhost:4200}") List<String> origenes) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(origenes);
        cors.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        cors.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", cors);
        return fuente;
    }

    @Bean
    SecurityFilterChain cadenaDeFiltros(HttpSecurity http, SecurityContextRepository repositorioDeContexto)
            throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers("/api/auth/login"))
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                .securityContext(contexto -> contexto.securityContextRepository(repositorioDeContexto))
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers("/api/auth/login").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint((req, res, e) -> escribir(res, 401, "NO_AUTENTICADO",
                                "Debe iniciar sesión."))
                        .accessDeniedHandler((req, res, e) -> escribir(res, 403, "SIN_PERMISO",
                                "No tiene permiso para esta acción.")))
                .formLogin(login -> login.disable())
                .httpBasic(basica -> basica.disable())
                .logout(salida -> salida.disable());
        return http.build();
    }

    private static void escribir(jakarta.servlet.http.HttpServletResponse res, int estado, String codigo,
            String mensaje) throws java.io.IOException {
        res.setStatus(estado);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write("{\"codigo\":\"" + codigo + "\",\"mensaje\":\"" + mensaje + "\"}");
    }
}
