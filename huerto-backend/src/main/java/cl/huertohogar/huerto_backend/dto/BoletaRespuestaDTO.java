
package cl.huertohogar.huerto_backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record BoletaRespuestaDTO(

        Long id,

        Long usuarioId,

        LocalDateTime fecha,

        Integer total,

        String estado,

        List<DetalleBoletaRespuestaDTO> detalles

) {
}
