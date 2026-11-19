
package cl.huertohogar.huerto_backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CrearBoletaDTO(

        @NotEmpty(
                message = "La compra debe tener al menos un producto"
        )
        @Valid
        List<DetalleCompraDTO> detalles

) {
}
