
package cl.huertohogar.huerto_backend.dto;

import jakarta.validation.constraints.*;

public record ProductoDTO(

        Long id,

        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotNull(message = "El precio es obligatorio")
        @Positive(message = "El precio debe ser mayor a 0")
        Integer precio,

        @NotBlank(message = "La categoria es obligatoria")
        String categoria,

        String descripcion,

        String imagen,

        @NotNull(message = "Debe indicar si tiene oferta")
        Boolean oferta,

        @NotNull(message = "El descuento es obligatorio")
        @Min(value = 0, message = "El descuento no puede ser negativo")
        @Max(value = 100, message = "El descuento no puede superar 100")
        Integer descuento,

        @NotNull(message = "El stock es obligatorio")
        @PositiveOrZero(message = "El stock no puede ser negativo")
        Integer stock

) {}
