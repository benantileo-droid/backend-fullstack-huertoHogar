
package cl.huertohogar.huerto_backend.service;

import cl.huertohogar.huerto_backend.dto.RegistroUsuarioDTO;
import cl.huertohogar.huerto_backend.dto.UsuarioRespuestaDTO;
import cl.huertohogar.huerto_backend.entity.Rol;
import cl.huertohogar.huerto_backend.entity.Usuario;
import cl.huertohogar.huerto_backend.repository.UsuarioRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Registrar un nuevo usuario
    @Transactional
    public UsuarioRespuestaDTO registrar(RegistroUsuarioDTO datos) {

        // 1. Normalizar el correo
        String correo = datos.correo()
                .trim()
                .toLowerCase(Locale.ROOT);

        // 2. Comprobar si el correo ya existe
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El correo ya esta registrado"
            );
        }

        // 3. Crear el usuario
        Usuario usuario = new Usuario();

        usuario.setNombre(datos.nombre().trim());
        usuario.setCorreo(correo);

        // 4. Proteger la contraseña con BCrypt
        usuario.setPasswordHash(
                passwordEncoder.encode(datos.password())
        );

        // 5. Asignar el rol CLIENTE
        usuario.setRol(Rol.CLIENTE);

        // 6. Guardar en MySQL
        Usuario guardado = usuarioRepository.save(usuario);

        // 7. Devolver los datos sin contraseña
        return new UsuarioRespuestaDTO(
                guardado.getId(),
                guardado.getNombre(),
                guardado.getCorreo(),
                guardado.getRol()
        );
    }
}
