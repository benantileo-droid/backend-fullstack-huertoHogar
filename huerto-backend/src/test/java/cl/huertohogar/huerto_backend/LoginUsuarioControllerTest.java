
package cl.huertohogar.huerto_backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LoginUsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    @Transactional
    void debeIniciarSesionYGenerarJWT() throws Exception {

        // 1. Crear un correo unico para esta prueba
        String correo = "login." + UUID.randomUUID()
                + "@ejemplo.com";

        // 2. Registrar un usuario temporal
        String registroJson = """
            {
                "nombre": "Cliente de prueba JWT",
                "correo": "%s",
                "password": "ClaveSegura123!"
            }
            """.formatted(correo);

        mockMvc.perform(
                        post("/api/v1/auth/registro")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registroJson)
                )
                .andExpect(status().isCreated());

        // 3. Iniciar sesion
        String loginJson = """
            {
                "correo": "%s",
                "password": "ClaveSegura123!"
            }
            """.formatted(correo);

        String respuesta = mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEnSegundos").value(3600))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 4. Extraer el token de la respuesta
        String token = com.jayway.jsonpath.JsonPath
                .read(respuesta, "$.token");

        // 5. Verificar la firma y validez del JWT
        Jwt jwt = jwtDecoder.decode(token);

        // 6. Comprobar su informacion
        assertEquals(correo, jwt.getSubject());
        assertEquals("CLIENTE", jwt.getClaimAsString("rol"));
        assertEquals("huerto-hogar",
                jwt.getClaimAsString("iss"));

        // 7. Comprobar que aun no ha vencido
        assertNotNull(jwt.getExpiresAt());
        assertTrue(jwt.getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    @Transactional
    void debeRechazarPasswordIncorrecta() throws Exception {

        // 1. Crear un correo unico
        String correo = "login.invalido."
                + UUID.randomUUID()
                + "@ejemplo.com";

        // 2. Registrar un usuario con contraseña correcta
        String registroJson = """
        {
            "nombre": "Cliente de prueba",
            "correo": "%s",
            "password": "ClaveCorrecta123!"
        }
        """.formatted(correo);

        mockMvc.perform(
                        post("/api/v1/auth/registro")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registroJson)
                )
                .andExpect(status().isCreated());

        // 3. Intentar iniciar sesion con otra contraseña
        String loginIncorrecto = """
        {
            "correo": "%s",
            "password": "ClaveEquivocada123!"
        }
        """.formatted(correo);

        // 4. Debe devolver 401 y no entregar un JWT
        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginIncorrecto)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

}
