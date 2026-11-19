
package cl.huertohogar.huerto_backend.controller;

import cl.huertohogar.huerto_backend.dto.UsuarioRespuestaDTO;
import cl.huertohogar.huerto_backend.repository.UsuarioRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Sort;
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
    // VALIDAR ROL ADMIN
    // ==========================================

    private void verificarAdministrador(
            Authentication authentication
    ) {

        boolean esAdmin =
                authentication != null &&
                        authentication.getAuthorities()
                                .stream()
                                .anyMatch(authority ->
                                        "ROLE_ADMIN".equals(
                                                authority.getAuthority()
                                        )
                                );

        if (!esAdmin) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Acceso exclusivo para administradores."
            );
        }
    }

    // ==========================================
    // GET - LISTAR USUARIOS REALES
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
                .map(usuario ->
                        new UsuarioRespuestaDTO(
                                usuario.getId(),
                                usuario.getNombre(),
                                usuario.getCorreo(),
                                usuario.getRol()
                        )
                )
                .toList();
    }
}
