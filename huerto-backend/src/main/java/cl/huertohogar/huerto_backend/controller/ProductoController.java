
package cl.huertohogar.huerto_backend.controller;

import cl.huertohogar.huerto_backend.dto.ProductoDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api/v1/productos")
public class ProductoController {

    @GetMapping
    public List<ProductoDTO> listarProductos() {

        return List.of(
                new ProductoDTO(
                        1L,
                        "Manzana Roja",
                        1200,
                        "Frutas",
                        "Manzanas frescas",
                        "manzana.jpg",
                        false,
                        0,
                        30
                ),
                new ProductoDTO(
                        2L,
                        "Zanahoria",
                        990,
                        "Verduras",
                        "Zanahorias frescas",
                        "zanahoria.jpg",
                        true,
                        20,
                        15
                )
        );
    }
    @GetMapping("/{id}")
    public ResponseEntity<ProductoDTO> buscarPorId(
            @PathVariable Long id) {

        return listarProductos().stream()
                .filter(producto -> producto.id().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.notFound().build()
                );
    }
}
