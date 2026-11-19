
package cl.huertohogar.huerto_backend.config;

import cl.huertohogar.huerto_backend.entity.Usuario;
import cl.huertohogar.huerto_backend.repository.UsuarioRepository;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            UsuarioRepository usuarioRepository
    ) throws Exception {

        // =====================================
        // CONVERSION DE ROLES JWT
        // =====================================

        JwtGrantedAuthoritiesConverter rolesConverter =
                new JwtGrantedAuthoritiesConverter();

        rolesConverter.setAuthoritiesClaimName("rol");
        rolesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtConverter =
                new JwtAuthenticationConverter();

        // =====================================
        // NUEVO: VERIFICAR ROL ACTUAL EN MYSQL
        // =====================================

        jwtConverter.setJwtGrantedAuthoritiesConverter(
                (Jwt jwt) -> {

                    // 1. Obtener el correo del JWT.

                    String correo = jwt.getSubject();

                    // 2. Obtener el rol guardado
                    // en el momento de iniciar sesión.

                    String rolToken =
                            jwt.getClaimAsString("rol");

                    if (
                            correo == null ||
                                    correo.isBlank() ||
                                    rolToken == null ||
                                    rolToken.isBlank()
                    ) {

                        throw new BadCredentialsException(
                                "El JWT no contiene los datos requeridos."
                        );
                    }

                    // 3. Consultar el usuario actual
                    // directamente en MySQL.

                    Usuario usuario =
                            usuarioRepository.findByCorreo(correo)
                                    .orElseThrow(() ->
                                            new BadCredentialsException(
                                                    "El usuario ya no existe."
                                            )
                                    );

                    // 4. Comprobar que el rol actual
                    // coincida con el rol del JWT.

                    if (
                            usuario.getRol() == null ||
                                    !usuario.getRol().name()
                                            .equalsIgnoreCase(rolToken)
                    ) {

                        throw new BadCredentialsException(
                                "El rol del usuario cambió. " +
                                        "Debe iniciar sesión nuevamente."
                        );
                    }

                    // 5. Si el rol coincide,
                    // conceder las autoridades del JWT.

                    Collection<GrantedAuthority> authorities =
                            rolesConverter.convert(jwt);

                    return authorities;
                }
        );

        // =====================================
        // SEGURIDAD HTTP
        // =====================================

        http
                .cors(Customizer.withDefaults())

                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // =========================
                        // ERRORES INTERNOS
                        // =========================

                        .dispatcherTypeMatchers(
                                DispatcherType.ERROR,
                                DispatcherType.FORWARD
                        ).permitAll()

                        // =========================
                        // SWAGGER
                        // =========================

                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // =========================
                        // AUTENTICACION PUBLICA
                        // =========================

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/registro",
                                "/api/v1/auth/login"
                        ).permitAll()

                        // =========================
                        // PRODUCTOS - LECTURA
                        // =========================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/estado",
                                "/api/v1/productos",
                                "/api/v1/productos/{id}"
                        ).permitAll()

                        // =========================
                        // PRODUCTOS - SOLO ADMIN
                        // =========================

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

                        // =========================
                        // CATEGORIAS - LECTURA
                        // =========================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/categorias",
                                "/api/v1/categorias/**"
                        ).permitAll()

                        // =========================
                        // CATEGORIAS - SOLO ADMIN
                        // =========================

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/categorias"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/v1/categorias/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/categorias/**"
                        ).hasRole("ADMIN")

                        // =========================
                        // BOLETAS - CREAR
                        // =========================

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/boletas"
                        ).hasAnyRole(
                                "CLIENTE",
                                "ADMIN"
                        )

                        // =========================
                        // BOLETAS - MIS COMPRAS
                        // =========================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/boletas/mis-compras"
                        ).hasAnyRole(
                                "CLIENTE",
                                "ADMIN"
                        )

                        // =========================
                        // BOLETAS - LISTAR TODAS
                        // =========================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/boletas"
                        ).hasAnyRole(
                                "VENDEDOR",
                                "ADMIN"
                        )

                        // =========================
                        // BOLETAS - DETALLE
                        // =========================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/boletas/{id}"
                        ).hasAnyRole(
                                "CLIENTE",
                                "VENDEDOR",
                                "ADMIN"
                        )

                        // =========================
                        // USUARIOS - LISTAR
                        // =========================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/admin/usuarios"
                        ).hasRole("ADMIN")

                        // =========================
                        // USUARIOS - CAMBIAR ROL
                        // =========================

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/v1/admin/usuarios/{id}/rol"
                        ).hasRole("ADMIN")

                        // =========================
                        // RESTO DE RUTAS
                        // =========================

                        .anyRequest().denyAll()
                )

                // =====================================
                // AUTENTICACION JWT
                // =====================================

                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        jwtConverter
                                )
                        )
                )

                // =====================================
                // DESACTIVAR LOGIN TRADICIONAL
                // =====================================

                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
