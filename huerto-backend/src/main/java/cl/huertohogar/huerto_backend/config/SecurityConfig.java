
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
                // API sin sesiones ni autenticacion por cookies
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth

                        // Swagger publico
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // Registro y login publicos
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/registro",
                                "/api/v1/auth/login"
                        ).permitAll()

                        // Consultar productos y estado
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/estado",
                                "/api/v1/productos",
                                "/api/v1/productos/**"
                        ).permitAll()

                        // Crear productos: ADMIN o VENDEDOR
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/productos"
                        ).hasAnyRole("ADMIN", "VENDEDOR")

                        // Actualizar productos: ADMIN o VENDEDOR
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/v1/productos/**"
                        ).hasAnyRole("ADMIN", "VENDEDOR")

                        // Eliminar productos: solo ADMIN
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/productos/**"
                        ).hasRole("ADMIN")

                        // El resto necesita autenticacion
                        .anyRequest().authenticated()
                )

                // Autenticacion mediante JWT
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(
                                        jwtAuthenticationConverter
                                )
                        )
                )

                // Desactivar el login temporal
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }

    // Convertir el rol del JWT en un permiso de Spring
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter roles =
                new JwtGrantedAuthoritiesConverter();

        roles.setAuthoritiesClaimName("rol");
        roles.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(roles);

        return converter;
    }
}
