
package cl.huertohogar.huerto_backend.dto;

import cl.huertohogar.huerto_backend.entity.Rol;

public record UsuarioRespuestaDTO(
        Long id,
        String nombre,
        String correo,
        Rol rol
) {
}
