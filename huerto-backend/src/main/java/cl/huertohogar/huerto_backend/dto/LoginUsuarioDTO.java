
package cl.huertohogar.huerto_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginUsuarioDTO(

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo debe ser valido")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        String password

) {
}
