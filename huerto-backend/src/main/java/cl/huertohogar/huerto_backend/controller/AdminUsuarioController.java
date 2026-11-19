
package cl.huertohogar.huerto_backend.controller;

import cl.huertohogar.huerto_backend.dto.UsuarioRespuestaDTO;
import cl.huertohogar.huerto_backend.entity.Rol;
import cl.huertohogar.huerto_backend.entity.Usuario;
import cl.huertohogar.huerto_backend.repository.UsuarioRepository;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/usuarios")
public class AdminUsuarioController {

    private final UsuarioRepository usuarioRepository;

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public AdminUsuarioController(
            UsuarioRepository usuarioRepository
    ) {
        this.usuarioRepository = usuarioRepository;
    }

    // ==========================================
    // DTO PARA CAMBIAR ROL
    // ==========================================

    public record CambiarRolPeticion(
            Rol rol
    ) {
    }

    // ==========================================
    // CONVERTIR USUARIO A DTO
    // ==========================================

    private UsuarioRespuestaDTO convertirRespuesta(
            Usuario usuario
    ) {

        return new UsuarioRespuestaDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getRol()
        );
    }

    // ==========================================
    // VERIFICAR ADMINISTRADOR
    // ==========================================

    private Usuario verificarAdministrador(
            Authentication authentication
    ) {

        boolean tieneRolAdmin =
                authentication != null &&
                        authentication.isAuthenticated() &&
                        authentication.getAuthorities()
                                .stream()
                                .anyMatch(authority ->
                                        "ROLE_ADMIN".equals(
                                                authority.getAuthority()
                                        )
                                );

        if (!tieneRolAdmin) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Acceso exclusivo para administradores."
            );
        }

        // Verificamos también el rol actual en MySQL.
        // Esto evita confiar solamente en el rol
        // que quedó guardado en un JWT antiguo.

        return usuarioRepository
                .findByCorreo(authentication.getName())
                .filter(usuario ->
                        usuario.getRol() == Rol.ADMIN
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "La cuenta ya no tiene permisos ADMIN."
                        )
                );
    }

    // ==========================================
    // GET - LISTAR USUARIOS
    // ==========================================

    @GetMapping
    @Transactional(readOnly = true)
    public List<UsuarioRespuestaDTO> listar(
            Authentication authentication
    ) {

        verificarAdministrador(authentication);

        return usuarioRepository
                .findAll(
                        Sort.by(
                                Sort.Direction.ASC,
                                "id"
                        )
                )
                .stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    // ==========================================
    // PUT - CAMBIAR ROL DE UN USUARIO
    // ==========================================

    @PutMapping("/{id}/rol")
    @Transactional
    public UsuarioRespuestaDTO cambiarRol(
            @PathVariable Long id,
            @RequestBody CambiarRolPeticion datos,
            Authentication authentication
    ) {

        // 1. Confirmar que quien solicita
        // la modificación sea ADMIN.

        Usuario administrador =
                verificarAdministrador(authentication);

        // 2. Validar el nuevo rol.

        if (datos == null || datos.rol() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debes indicar un rol válido."
            );
        }

        Rol nuevoRol = datos.rol();

        // 3. Buscar el usuario en MySQL.

        Usuario usuario =
                usuarioRepository.findById(id)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "El usuario no existe."
                                )
                        );

        // 4. Evitar que el administrador
        // elimine su propio permiso ADMIN.

        boolean esSuPropiaCuenta =
                usuario.getId().equals(
                        administrador.getId()
                );

        if (
                esSuPropiaCuenta &&
                        nuevoRol != Rol.ADMIN
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No puedes quitarte tu propio rol ADMIN."
            );
        }

        // 5. Impedir que el sistema se quede
        // sin administradores.

        if (
                usuario.getRol() == Rol.ADMIN &&
                        nuevoRol != Rol.ADMIN
        ) {

            long cantidadAdministradores =
                    usuarioRepository.findAll()
                            .stream()
                            .filter(u ->
                                    u.getRol() == Rol.ADMIN
                            )
                            .count();

            if (cantidadAdministradores <= 1) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Debe existir al menos un administrador."
                );
            }
        }

        // 6. Actualizar el rol.

        usuario.setRol(nuevoRol);

        Usuario guardado =
                usuarioRepository.save(usuario);

        // 7. Devolver únicamente datos públicos.

        return convertirRespuesta(guardado);
    }
}
