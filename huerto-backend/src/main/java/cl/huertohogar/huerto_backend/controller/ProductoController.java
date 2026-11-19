
package cl.huertohogar.huerto_backend.controller;

import cl.huertohogar.huerto_backend.dto.ProductoDTO;
import cl.huertohogar.huerto_backend.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;

@RestController
@RequestMapping("/api/v1/productos")
@Tag(
        name = "Productos",
        description = "Gestion de productos de Huerto Hogar"
)
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    // GET: Listar todos los productos
    @Operation(
            summary = "Listar todos los productos",
            description = "Obtiene los productos almacenados en MySQL"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Listado obtenido correctamente"
    )
    @GetMapping
    public List<ProductoDTO> listarProductos() {
        return productoService.listar();
    }

    // GET: Buscar un producto por ID
    @Operation(
            summary = "Buscar producto por ID",
            description = "Busca un producto por su identificador"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Producto encontrado"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Producto no encontrado"
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductoDTO> buscarPorId(
            @PathVariable Long id) {

        return productoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.notFound().build());
    }

    // POST: Crear un nuevo producto
    @Operation(
            summary = "Crear un producto",
            description = "Registra un nuevo producto en MySQL"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Producto creado correctamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos del producto invalidos"
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<ProductoDTO> crearProducto(
            @Valid @RequestBody ProductoDTO datos) {

        ProductoDTO nuevo = productoService.crear(datos);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(nuevo);
    }
    @Operation(
            summary = "Actualizar un producto",
            description = "Modifica los datos de un producto existente"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Producto actualizado correctamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos invalidos"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Producto no encontrado"
            )
    })
    // PUT: Actualizar un producto existente
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    public ResponseEntity<ProductoDTO> actualizarProducto(
            @PathVariable Long id,
            @Valid @RequestBody ProductoDTO datos) {

        return productoService.actualizar(id, datos)
                .map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.notFound().build());
    }
    // DELETE: Eliminar un producto por su ID
    @Operation(
            summary = "Eliminar un producto",
            description = "Elimina un producto de MySQL por su ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Producto eliminado correctamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Producto no encontrado"
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(
            @PathVariable Long id) {

        if (productoService.eliminar(id).isPresent()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.notFound().build();
    }

}
