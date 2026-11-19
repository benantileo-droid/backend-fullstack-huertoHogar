
package cl.huertohogar.huerto_backend.config;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        // Convertir el campo "rol" del JWT
        // en ROLE_ADMIN, ROLE_VENDEDOR o ROLE_CLIENTE

        JwtGrantedAuthoritiesConverter rolesConverter =
                new JwtGrantedAuthoritiesConverter();

        rolesConverter.setAuthoritiesClaimName("rol");
        rolesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtConverter =
                new JwtAuthenticationConverter();

        jwtConverter.setJwtGrantedAuthoritiesConverter(
                rolesConverter
        );

        http
                // API REST con JWT, sin sesiones por cookies
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Despachos internos de errores
                        .dispatcherTypeMatchers(
                                DispatcherType.ERROR,
                                DispatcherType.FORWARD
                        ).permitAll()

                        // =====================================
                        // SWAGGER: ACCESO PUBLICO
                        // =====================================

                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // =====================================
                        // AUTENTICACION: ACCESO PUBLICO
                        // =====================================

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/registro",
                                "/api/v1/auth/login"
                        ).permitAll()

                        // =====================================
                        // PRODUCTOS: CONSULTAS PUBLICAS
                        // =====================================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/estado",
                                "/api/v1/productos",
                                "/api/v1/productos/{id}"
                        ).permitAll()

                        // =====================================
                        // PRODUCTOS: SOLO ADMIN
                        // =====================================

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/productos"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/v1/productos/{id}"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/productos/{id}"
                        ).hasRole("ADMIN")

                        // =====================================
                        // BOLETAS: CREAR COMPRA
                        // CLIENTE Y ADMIN
                        // =====================================

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/boletas"
                        ).hasAnyRole("CLIENTE", "ADMIN")

                        // =====================================
                        // BOLETAS: MIS COMPRAS
                        // CLIENTE Y ADMIN
                        // =====================================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/boletas/mis-compras"
                        ).hasAnyRole("CLIENTE", "ADMIN")

                        // =====================================
                        // BOLETAS: LISTAR TODAS
                        // VENDEDOR Y ADMIN
                        // =====================================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/boletas"
                        ).hasAnyRole("VENDEDOR", "ADMIN")

                        // =====================================
                        // BOLETAS: CONSULTAR POR ID
                        // CLIENTE, VENDEDOR Y ADMIN
                        // =====================================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/boletas/{id}"
                        ).hasAnyRole(
                                "CLIENTE",
                                "VENDEDOR",
                                "ADMIN"
                        )

                        // =====================================
                        // BLOQUEAR RUTAS NO DEFINIDAS
                        // =====================================

                        .anyRequest().denyAll()
                )

                // =====================================
                // VALIDAR JWT
                // =====================================

                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        jwtConverter
                                )
                        )
                )

                // Desactivar login tradicional
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
