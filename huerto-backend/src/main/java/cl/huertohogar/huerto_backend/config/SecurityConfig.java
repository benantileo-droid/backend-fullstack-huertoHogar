
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
                // Excluir registro y login de CSRF
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/api/v1/auth/registro",
                                "/api/v1/auth/login"
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Permitir Swagger
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

                        // Permitir registro y login
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/registro",
                                "/api/v1/auth/login"
                        ).permitAll()

                        // Proteger los demas endpoints
                        .anyRequest().authenticated()
                )

                // Login temporal de Spring Security
                .formLogin(withDefaults());

        return http.build();
    }
}
