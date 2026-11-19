
package cl.huertohogar.huerto_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {

        http
                // API REST con JWT, sin sesiones por cookies
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // SWAGGER: Acceso publico
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // AUTENTICACION: Registro y login publicos
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/registro",
                                "/api/v1/auth/login"
                        ).permitAll()

                        // ESTADO: Consulta publica
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/estado"
                        ).permitAll()

                        // PRODUCTOS: Consultas publicas
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/productos",
                                "/api/v1/productos/**"
                        ).permitAll()

                        // CREAR PRODUCTOS: Solo ADMIN
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/productos"
                        ).hasRole("ADMIN")

                        // ACTUALIZAR PRODUCTOS: Solo ADMIN
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/v1/productos/**"
                        ).hasRole("ADMIN")

                        // ELIMINAR PRODUCTOS: Solo ADMIN
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/productos/**"
                        ).hasRole("ADMIN")

                        // Demas rutas: Requieren autenticacion
                        .anyRequest().authenticated()
                )

                // Verificar tokens JWT
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(
                                        jwtAuthenticationConverter
                                )
                        )
                )

                // Desactivar login tradicional
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }

    // Convertir el rol del JWT en autoridad de Spring
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter roles =
                new JwtGrantedAuthoritiesConverter();

        // Leer el campo "rol" del token JWT
        roles.setAuthoritiesClaimName("rol");

        // CLIENTE -> ROLE_CLIENTE
        // VENDEDOR -> ROLE_VENDEDOR
        // ADMIN -> ROLE_ADMIN
        roles.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(roles);

        return converter;
    }
}
