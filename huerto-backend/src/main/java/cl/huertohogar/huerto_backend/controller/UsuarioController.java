
package cl.huertohogar.huerto_backend.controller;

import cl.huertohogar.huerto_backend.dto.RegistroUsuarioDTO;
import cl.huertohogar.huerto_backend.dto.UsuarioRespuestaDTO;
import cl.huertohogar.huerto_backend.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(
        name = "Autenticacion",
        description = "Registro e inicio de sesion de usuarios"
)
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Operation(
            summary = "Registrar un usuario",
            description = "Crea un usuario con rol CLIENTE y contraseña protegida con BCrypt"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Usuario registrado correctamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos de registro invalidos"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "El correo ya esta registrado"
            )
    })
    @PostMapping("/registro")
    public ResponseEntity<UsuarioRespuestaDTO> registrar(
            @Valid @RequestBody RegistroUsuarioDTO datos) {

        UsuarioRespuestaDTO usuario = usuarioService.registrar(datos);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(usuario);
    }
}
