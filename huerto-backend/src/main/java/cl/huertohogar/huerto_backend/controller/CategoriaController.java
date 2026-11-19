
package cl.huertohogar.huerto_backend.controller;

import cl.huertohogar.huerto_backend.entity.Categoria;
import cl.huertohogar.huerto_backend.service.CategoriaService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public CategoriaController(
            CategoriaService categoriaService
    ) {
        this.categoriaService = categoriaService;
    }

    // ==========================================
    // DTO PARA RECIBIR DATOS
    // ==========================================

    public record CategoriaPeticion(
            String nombre,
            String descripcion
    ) {
    }

    // ==========================================
    // DTO PARA RESPONDER AL FRONTEND
    // ==========================================

    public record CategoriaRespuesta(
            Long id,
            String nombre,
            String descripcion
    ) {
    }

    // ==========================================
    // CONVERTIR ENTIDAD A RESPUESTA
    // ==========================================

    private CategoriaRespuesta convertirRespuesta(
            Categoria categoria
    ) {
        return new CategoriaRespuesta(
                categoria.getId(),
                categoria.getNombre(),
                categoria.getDescripcion()
        );
    }

    // ==========================================
    // VERIFICAR ROL ADMIN
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
    // GET - LISTAR TODAS LAS CATEGORIAS
    // ==========================================

    @GetMapping
    public List<CategoriaRespuesta> listar() {

        return categoriaService.listar()
                .stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    // ==========================================
    // GET - BUSCAR CATEGORIA POR ID
    // ==========================================

    @GetMapping("/{id}")
    public CategoriaRespuesta obtenerPorId(
            @PathVariable Long id
    ) {

        Categoria categoria =
                categoriaService.obtenerPorId(id);

        return convertirRespuesta(categoria);
    }

    // ==========================================
    // POST - CREAR CATEGORIA
    // ==========================================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaRespuesta crear(
            @RequestBody CategoriaPeticion datos,
            Authentication authentication
    ) {

        verificarAdministrador(authentication);

        Categoria categoria =
                categoriaService.crear(
                        datos.nombre(),
                        datos.descripcion()
                );

        return convertirRespuesta(categoria);
    }

    // ==========================================
    // PUT - ACTUALIZAR CATEGORIA
    // ==========================================

    @PutMapping("/{id}")
    public CategoriaRespuesta actualizar(
            @PathVariable Long id,
            @RequestBody CategoriaPeticion datos,
            Authentication authentication
    ) {

        verificarAdministrador(authentication);

        Categoria categoria =
                categoriaService.actualizar(
                        id,
                        datos.nombre(),
                        datos.descripcion()
                );

        return convertirRespuesta(categoria);
    }

    // ==========================================
    // DELETE - ELIMINAR CATEGORIA
    // ==========================================

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(
            @PathVariable Long id,
            Authentication authentication
    ) {

        verificarAdministrador(authentication);

        categoriaService.eliminar(id);
    }
}
