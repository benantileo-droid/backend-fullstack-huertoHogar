
package cl.huertohogar.huerto_backend.controller;

import cl.huertohogar.huerto_backend.dto.BoletaRespuestaDTO;
import cl.huertohogar.huerto_backend.dto.CrearBoletaDTO;
import cl.huertohogar.huerto_backend.service.BoletaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/boletas")
@Tag(name = "Boletas", description = "Gestion de compras")
@SecurityRequirement(name = "bearerAuth")
public class BoletaController {

    private final BoletaService boletaService;

    public BoletaController(BoletaService boletaService) {
        this.boletaService = boletaService;
    }

    // CLIENTE y ADMIN pueden crear compras
    @PostMapping
    @Operation(summary = "Registrar una compra")
    public ResponseEntity<BoletaRespuestaDTO> crear(
            @Valid @RequestBody CrearBoletaDTO solicitud,
            JwtAuthenticationToken autenticacion
    ) {

        String correo = autenticacion
                .getToken()
                .getSubject();

        BoletaRespuestaDTO respuesta =
                boletaService.crear(solicitud, correo);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(respuesta);
    }

    // CLIENTE y ADMIN: consultar sus propias compras
    @GetMapping("/mis-compras")
    @Operation(summary = "Consultar mis compras")
    public ResponseEntity<List<BoletaRespuestaDTO>>
    listarMisCompras(
            JwtAuthenticationToken autenticacion
    ) {

        String correo = autenticacion
                .getToken()
                .getSubject();

        return ResponseEntity.ok(
                boletaService.listarMisCompras(correo)
        );
    }

    // VENDEDOR y ADMIN: consultar todas las compras
    @GetMapping
    @Operation(summary = "Listar todas las boletas")
    public ResponseEntity<List<BoletaRespuestaDTO>>
    listarTodas() {

        return ResponseEntity.ok(
                boletaService.listarTodas()
        );
    }

    // CLIENTE: solo sus propias boletas
    // VENDEDOR y ADMIN: todas las boletas
    @GetMapping("/{id}")
    @Operation(summary = "Consultar una boleta por ID")
    public ResponseEntity<BoletaRespuestaDTO> buscarPorId(
            @PathVariable Long id,
            JwtAuthenticationToken autenticacion
    ) {

        String correo = autenticacion
                .getToken()
                .getSubject();

        boolean puedeVerTodas = autenticacion
                .getAuthorities()
                .stream()
                .anyMatch(autoridad ->
                        autoridad.getAuthority()
                                .equals("ROLE_ADMIN")
                                ||
                                autoridad.getAuthority()
                                        .equals("ROLE_VENDEDOR")
                );

        BoletaRespuestaDTO respuesta =
                boletaService.buscarPorId(
                        id,
                        correo,
                        puedeVerTodas
                );

        return ResponseEntity.ok(respuesta);
    }
}
