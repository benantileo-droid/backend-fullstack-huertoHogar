
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
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/estado",
                                "/api/v1/productos",
                                "/api/v1/productos/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(withDefaults());

        return http.build();
    }
}
