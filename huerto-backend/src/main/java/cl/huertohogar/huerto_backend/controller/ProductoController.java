
package cl.huertohogar.huerto_backend.controller;

import cl.huertohogar.huerto_backend.dto.ProductoDTO;
import cl.huertohogar.huerto_backend.service.ProductoService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/v1/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    // GET: Listar todos los productos
    @GetMapping
    public List<ProductoDTO> listarProductos() {
        return productoService.listar();
    }

    // GET: Buscar un producto por ID
    @GetMapping("/{id}")
    public ResponseEntity<ProductoDTO> buscarPorId(
            @PathVariable Long id) {

        return productoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.notFound().build());
    }

    // POST: Crear un nuevo producto
    @PostMapping
    public ResponseEntity<ProductoDTO> crearProducto(
            @Valid @RequestBody ProductoDTO datos) {

        ProductoDTO nuevo = productoService.crear(datos);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(nuevo);
    }
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
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(
            @PathVariable Long id) {

        if (productoService.eliminar(id).isPresent()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.notFound().build();
    }
}
