
package cl.huertohogar.huerto_backend;

import cl.huertohogar.huerto_backend.entity.Rol;
import cl.huertohogar.huerto_backend.entity.Usuario;
import cl.huertohogar.huerto_backend.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @Transactional
    void registrarUsuarioEnMySQL() throws Exception {

        // 1. Crear un correo unico para la prueba
        String correo = "prueba." + UUID.randomUUID()
                + "@ejemplo.com";

        String password = "ClaveDePrueba123!";

        String usuarioJson = """
            {
                "nombre": "Cliente de prueba",
                "correo": "%s",
                "password": "ClaveDePrueba123!"
            }
            """.formatted(correo);

        // 2. Registrar mediante POST
        mockMvc.perform(
                        post("/api/v1/auth/registro")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(usuarioJson)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre")
                        .value("Cliente de prueba"))
                .andExpect(jsonPath("$.correo").value(correo))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        // 3. Comprobar que el usuario existe en MySQL
        usuarioRepository.flush();

        Usuario usuario = usuarioRepository
                .findByCorreo(correo)
                .orElseThrow();

        // 4. Comprobar el rol
        assertEquals(Rol.CLIENTE, usuario.getRol());

        // 5. Comprobar que la contraseña tiene hash
        assertNotEquals(password, usuario.getPasswordHash());

        // 6. Verificar que BCrypt reconoce la contraseña
        assertTrue(passwordEncoder.matches(
                password,
                usuario.getPasswordHash()
        ));

        // 7. Una contraseña incorrecta debe fallar
        assertFalse(passwordEncoder.matches(
                "ContraseñaIncorrecta",
                usuario.getPasswordHash()
        ));
    }

    @Test
    @Transactional
    void debeRechazarCorreoDuplicado() throws Exception {

        // Crear un correo unico para la prueba
        String correo = "duplicado." + UUID.randomUUID()
                + "@ejemplo.com";

        String usuarioJson = """
        {
            "nombre": "Cliente repetido",
            "correo": "%s",
            "password": "ClaveDePrueba123!"
        }
        """.formatted(correo);

        // Primer registro: debe funcionar
        mockMvc.perform(
                        post("/api/v1/auth/registro")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(usuarioJson)
                )
                .andExpect(status().isCreated());

        // Segundo registro: mismo correo, debe fallar
        mockMvc.perform(
                        post("/api/v1/auth/registro")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(usuarioJson)
                )
                .andExpect(status().isConflict());
    }

    @Test
    @Transactional
    void debeRechazarRegistroInvalido() throws Exception {

        // Caso 1: correo con formato incorrecto
        String correoInvalido = """
        {
            "nombre": "Cliente de prueba",
            "correo": "correo-invalido",
            "password": "ClaveDePrueba123!"
        }
        """;

        mockMvc.perform(
                        post("/api/v1/auth/registro")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(correoInvalido)
                )
                .andExpect(status().isBadRequest());

        // Caso 2: contraseña demasiado corta
        String correo = "invalido." + UUID.randomUUID()
                + "@ejemplo.com";

        String passwordCorta = """
        {
            "nombre": "Cliente de prueba",
            "correo": "%s",
            "password": "123"
        }
        """.formatted(correo);

        mockMvc.perform(
                        post("/api/v1/auth/registro")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(passwordCorta)
                )
                .andExpect(status().isBadRequest());

        // Verificar que el segundo usuario no se guardo
        assertFalse(usuarioRepository.existsByCorreo(correo));
    }


}
