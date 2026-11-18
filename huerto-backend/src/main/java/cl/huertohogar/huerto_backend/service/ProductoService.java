
package cl.huertohogar.huerto_backend.service;

import cl.huertohogar.huerto_backend.dto.ProductoDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ProductoService {

    private final Map<Long, ProductoDTO> productos =
            new ConcurrentHashMap<>();

    private final AtomicLong ultimoId = new AtomicLong(2);

    public ProductoService() {
        productos.put(1L, new ProductoDTO(
                1L, "Manzana Roja", 1200, "Frutas",
                "Manzanas frescas", "manzana.jpg",
                false, 0, 30
        ));

        productos.put(2L, new ProductoDTO(
                2L, "Zanahoria", 990, "Verduras",
                "Zanahorias frescas", "zanahoria.jpg",
                true, 20, 15
        ));
    }

    public List<ProductoDTO> listar() {
        return productos.values().stream()
                .sorted(Comparator.comparing(ProductoDTO::id))
                .toList();
    }

    public Optional<ProductoDTO> buscarPorId(Long id) {
        return Optional.ofNullable(productos.get(id));
    }

    public ProductoDTO crear(ProductoDTO datos) {
        Long nuevoId = ultimoId.incrementAndGet();

        ProductoDTO nuevo = new ProductoDTO(
                nuevoId,
                datos.nombre(),
                datos.precio(),
                datos.categoria(),
                datos.descripcion(),
                datos.imagen(),
                datos.oferta(),
                datos.descuento(),
                datos.stock()
        );

        productos.put(nuevoId, nuevo);

        return nuevo;
    }
    public Optional<ProductoDTO> actualizar(
            Long id, ProductoDTO datos) {

        // Verificamos si existe el producto
        if (!productos.containsKey(id)) {
            return Optional.empty();
        }

        // Creamos el producto con los nuevos datos
        ProductoDTO actualizado = new ProductoDTO(
                id,
                datos.nombre(),
                datos.precio(),
                datos.categoria(),
                datos.descripcion(),
                datos.imagen(),
                datos.oferta(),
                datos.descuento(),
                datos.stock()
        );

        // Reemplazamos el producto anterior
        productos.put(id, actualizado);

        return Optional.of(actualizado);
    }
    public Optional<ProductoDTO> eliminar(Long id) {

        return Optional.ofNullable(productos.remove(id));

    }
}
