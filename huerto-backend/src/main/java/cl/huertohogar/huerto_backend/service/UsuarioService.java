
package cl.huertohogar.huerto_backend.service;

import cl.huertohogar.huerto_backend.dto.LoginUsuarioDTO;
import cl.huertohogar.huerto_backend.dto.RegistroUsuarioDTO;
import cl.huertohogar.huerto_backend.dto.TokenRespuestaDTO;
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
    private final JwtService jwtService;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // REGISTRO: Crear un usuario
    @Transactional
    public UsuarioRespuestaDTO registrar(RegistroUsuarioDTO datos) {

        String correo = datos.correo()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Comprobar si el correo ya existe
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El correo ya esta registrado"
            );
        }

        Usuario usuario = new Usuario();

        usuario.setNombre(datos.nombre().trim());
        usuario.setCorreo(correo);

        // Proteger la contraseña
        usuario.setPasswordHash(
                passwordEncoder.encode(datos.password())
        );

        // Asignar rol inicial
        usuario.setRol(Rol.CLIENTE);

        Usuario guardado = usuarioRepository.save(usuario);

        // Devolver informacion sin contraseña
        return new UsuarioRespuestaDTO(
                guardado.getId(),
                guardado.getNombre(),
                guardado.getCorreo(),
                guardado.getRol()
        );
    }

    // LOGIN: Iniciar sesion
    @Transactional(readOnly = true)
    public TokenRespuestaDTO iniciarSesion(LoginUsuarioDTO datos) {

        // 1. Normalizar el correo
        String correo = datos.correo()
                .trim()
                .toLowerCase(Locale.ROOT);

        // 2. Buscar usuario en MySQL
        Usuario usuario = usuarioRepository
                .findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Correo o contraseña incorrectos"
                ));

        // 3. Comprobar la contraseña con BCrypt
        boolean passwordCorrecta = passwordEncoder.matches(
                datos.password(),
                usuario.getPasswordHash()
        );

        if (!passwordCorrecta) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Correo o contraseña incorrectos"
            );
        }

        // 4. Generar JWT
        return jwtService.generarToken(usuario);
    }
}
