
package cl.huertohogar.huerto_backend.dto;

public record TokenRespuestaDTO(
        String token,
        String tipo,
        long expiraEnSegundos
) {
}
