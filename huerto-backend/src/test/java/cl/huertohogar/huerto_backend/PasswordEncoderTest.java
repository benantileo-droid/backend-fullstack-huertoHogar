
package cl.huertohogar.huerto_backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PasswordEncoderTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void debeProtegerYVerificarPassword() {

        // Contraseña ficticia utilizada solo en esta prueba
        String password = "ClaveDePrueba123!";

        // Generamos el hash BCrypt
        String hash = passwordEncoder.encode(password);

        // La contraseña original no debe guardarse igual
        assertNotEquals(password, hash);

        // La contraseña correcta debe coincidir
        assertTrue(passwordEncoder.matches(password, hash));

        // Una contraseña incorrecta debe fallar
        assertFalse(
                passwordEncoder.matches("ClaveIncorrecta", hash)
        );
    }
}
