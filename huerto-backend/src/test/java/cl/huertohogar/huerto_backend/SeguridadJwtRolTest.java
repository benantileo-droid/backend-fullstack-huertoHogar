
package cl.huertohogar.huerto_backend;

import cl.huertohogar.huerto_backend.config.SecurityConfig;
import cl.huertohogar.huerto_backend.entity.Rol;
import cl.huertohogar.huerto_backend.entity.Usuario;
import cl.huertohogar.huerto_backend.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;

import org.springframework.http.HttpHeaders;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// =============================================
// PRUEBAS DE SEGURIDAD JWT Y ROLES
// HUERTO HOGAR
// =============================================

@WebMvcTest(
        controllers = SeguridadJwtRolTest.ControladorPrueba.class
)
@Import({
        SecurityConfig.class,
        SeguridadJwtRolTest.ControladorPrueba.class
})
class SeguridadJwtRolTest {

    // =========================================
    // DATOS SIMULADOS
    // =========================================

    private static final String CORREO =
            "prueba@huertohogar.cl";

    private static final String TOKEN =
            "jwt-de-prueba";

    private static final String RUTA =
            "/api/v1/admin/usuarios";

    // =========================================
    // MOCKMVC
    // =========================================

    @Autowired
    private MockMvc mockMvc;

    // =========================================
    // DEPENDENCIAS SIMULADAS
    // =========================================

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // =========================================
    // CONTROLADOR SIMULADO
    // =========================================

    @RestController
    public static class ControladorPrueba {

        @GetMapping("/api/v1/admin/usuarios")
        public String usuarios() {

            return "USUARIOS";
        }
    }

    // =========================================
    // CREAR JWT DE PRUEBA
    // =========================================

    private Jwt crearJwt(String rol) {

        Jwt.Builder builder =
                Jwt.withTokenValue(TOKEN)
                        .header("alg", "HS256")
                        .subject(CORREO)
                        .issuedAt(
                                Instant.now().minusSeconds(60)
                        )
                        .expiresAt(
                                Instant.now().plusSeconds(3600)
                        );

        if (rol != null) {
            builder.claim("rol", rol);
        }

        return builder.build();
    }

    // =========================================
    // SIMULAR JWT DECODIFICADO
    // =========================================

    private void configurarToken(String rol) {

        when(jwtDecoder.decode(TOKEN))
                .thenReturn(crearJwt(rol));
    }

    // =========================================
    // SIMULAR USUARIO DE MYSQL
    // =========================================

    private void configurarUsuario(Rol rolActual) {

        Usuario usuario = mock(Usuario.class);

        when(usuario.getRol())
                .thenReturn(rolActual);

        when(usuarioRepository.findByCorreo(CORREO))
                .thenReturn(Optional.of(usuario));
    }

    // =========================================
    // PRUEBA 1
    // ADMIN CON ROL CORRECTO
    // =========================================

    @Test
    void adminConRolCorrectoPuedeAcceder()
            throws Exception {

        configurarToken("ADMIN");

        configurarUsuario(Rol.ADMIN);

        mockMvc.perform(
                        get(RUTA)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + TOKEN
                                )
                )
                .andExpect(status().isOk());

        verify(usuarioRepository)
                .findByCorreo(CORREO);
    }

    // =========================================
    // PRUEBA 2
    // JWT ANTIGUO DESPUES DE CAMBIAR EL ROL
    // =========================================

    @Test
    void tokenAntiguoDebeRechazarseSiCambiaElRol()
            throws Exception {

        // El JWT fue emitido cuando el usuario
        // era ADMIN.

        configurarToken("ADMIN");

        // Simular el rol almacenado en MySQL.
        //
        // AtomicReference permite cambiar el rol
        // durante la prueba, manteniendolo estable
        // mientras se procesa cada solicitud.

        AtomicReference<Rol> rolActual =
                new AtomicReference<>(Rol.ADMIN);

        Usuario usuario = mock(Usuario.class);

        // Devolver el rol actual en cada consulta.
        //
        // Ya no usamos thenReturn(ADMIN, CLIENTE),
        // porque SecurityConfig consulta getRol()
        // mas de una vez en una misma solicitud.

        when(usuario.getRol())
                .thenAnswer(
                        invocation -> rolActual.get()
                );

        when(usuarioRepository.findByCorreo(CORREO))
                .thenReturn(Optional.of(usuario));

        // =====================================
        // PRIMERA SOLICITUD
        // JWT: ADMIN
        // MYSQL: ADMIN
        // RESULTADO: 200
        // =====================================

        mockMvc.perform(
                        get(RUTA)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + TOKEN
                                )
                )
                .andExpect(status().isOk());

        // =====================================
        // SIMULAR CAMBIO DE ROL EN MYSQL
        // =====================================

        rolActual.set(Rol.CLIENTE);

        // =====================================
        // SEGUNDA SOLICITUD
        // MISMO JWT: ADMIN
        // MYSQL: CLIENTE
        // RESULTADO: 401
        // =====================================

        mockMvc.perform(
                        get(RUTA)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + TOKEN
                                )
                )
                .andExpect(status().isUnauthorized());

        // Comprobar que Spring consulto al
        // repositorio en ambas solicitudes.

        verify(
                usuarioRepository,
                times(2)
        ).findByCorreo(CORREO);
    }

    // =========================================
    // PRUEBA 3
    // USUARIO INEXISTENTE
    // =========================================

    @Test
    void tokenDeUsuarioInexistenteDebeRechazarse()
            throws Exception {

        configurarToken("ADMIN");

        when(usuarioRepository.findByCorreo(CORREO))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        get(RUTA)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + TOKEN
                                )
                )
                .andExpect(status().isUnauthorized());
    }

    // =========================================
    // PRUEBA 4
    // JWT SIN ROL
    // =========================================

    @Test
    void tokenSinRolDebeRechazarse()
            throws Exception {

        configurarToken(null);

        mockMvc.perform(
                        get(RUTA)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + TOKEN
                                )
                )
                .andExpect(status().isUnauthorized());
    }

    // =========================================
    // PRUEBA 5
    // CLIENTE NO PUEDE ADMINISTRAR USUARIOS
    // =========================================

    @Test
    void clienteConRolCorrectoNoPuedeAdministrarUsuarios()
            throws Exception {

        configurarToken("CLIENTE");

        configurarUsuario(Rol.CLIENTE);

        mockMvc.perform(
                        get(RUTA)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + TOKEN
                                )
                )
                .andExpect(status().isForbidden());
    }

    // =========================================
    // PRUEBA 6
    // SIN JWT
    // =========================================

    @Test
    void solicitudSinTokenDebeRechazarse()
            throws Exception {

        mockMvc.perform(
                        get(RUTA)
                )
                .andExpect(status().isUnauthorized());
    }
}
