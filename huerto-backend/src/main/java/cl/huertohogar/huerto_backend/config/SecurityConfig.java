
package cl.huertohogar.huerto_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                // Excluir solamente el registro de la protección CSRF
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/api/v1/auth/registro"
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Permitir acceso a Swagger
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // Permitir consultar productos
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/estado",
                                "/api/v1/productos",
                                "/api/v1/productos/**"
                        ).permitAll()

                        // Permitir registrar nuevos usuarios
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/registro"
                        ).permitAll()

                        // Proteger los demás endpoints
                        .anyRequest().authenticated()
                )

                // Login temporal de Spring Security
                .formLogin(withDefaults());

        return http.build();
    }
}
