
package cl.huertohogar.huerto_backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DetalleCompraDTO(

        @NotNull(message = "El producto es obligatorio")
        @Positive(message = "El ID debe ser positivo")
        Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a cero")
        Integer cantidad

) {
}
