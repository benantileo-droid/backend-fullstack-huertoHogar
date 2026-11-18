
package cl.huertohogar.huerto_backend.service;

import cl.huertohogar.huerto_backend.dto.ProductoDTO;
import cl.huertohogar.huerto_backend.entity.Producto;
import cl.huertohogar.huerto_backend.repository.ProductoRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ProductoService {

    // Repositorio para conectar con MySQL
    private final ProductoRepository productoRepository;

    // Almacenamiento temporal de productos
    private final Map<Long, ProductoDTO> productos =
            new ConcurrentHashMap<>();

    private final AtomicLong ultimoId = new AtomicLong(2);

    // Constructor e inyeccion de dependencias
    public ProductoService(ProductoRepository productoRepository) {

        this.productoRepository = productoRepository;

        // Productos de prueba
        productos.put(1L, new ProductoDTO(
                1L,
                "Manzana Roja",
                1200,
                "Frutas",
                "Manzanas frescas",
                "manzana.jpg",
                false,
                0,
                30
        ));

        productos.put(2L, new ProductoDTO(
                2L,
                "Zanahoria",
                990,
                "Verduras",
                "Zanahorias frescas",
                "zanahoria.jpg",
                true,
                20,
                15
        ));
    }

    // GET: Listar todos los productos
    public List<ProductoDTO> listar() {

        return productoRepository.findAll()
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    // GET: Buscar un producto por ID
    public Optional<ProductoDTO> buscarPorId(Long id) {

        return productoRepository.findById(id)
                .map(this::convertirADTO);
    }

    // POST: Crear un producto en MySQL
    public ProductoDTO crear(ProductoDTO datos) {

        // Convertimos el DTO a Entity
        Producto producto = convertirAEntidad(datos);

        // Guardamos el producto en MySQL
        Producto guardado = productoRepository.save(producto);

        // Devolvemos el producto como DTO
        return convertirADTO(guardado);
    }


    // PUT: Actualizar un producto en MySQL
    public Optional<ProductoDTO> actualizar(
            Long id, ProductoDTO datos) {

        return productoRepository.findById(id)
                .map(producto -> {

                    // Modificamos los datos del producto
                    producto.setNombre(datos.nombre());
                    producto.setPrecio(datos.precio());
                    producto.setCategoria(datos.categoria());
                    producto.setDescripcion(datos.descripcion());
                    producto.setImagen(datos.imagen());
                    producto.setOferta(datos.oferta());
                    producto.setDescuento(datos.descuento());
                    producto.setStock(datos.stock());

                    // Guardamos los cambios en MySQL
                    Producto actualizado =
                            productoRepository.save(producto);

                    // Devolvemos el DTO actualizado
                    return convertirADTO(actualizado);
                });
    }


    // DELETE: Eliminar un producto
    public Optional<ProductoDTO> eliminar(Long id) {

        return Optional.ofNullable(productos.remove(id));
    }

    // Convertir DTO a Entity
    private Producto convertirAEntidad(ProductoDTO datos) {

        Producto producto = new Producto();

        producto.setNombre(datos.nombre());
        producto.setPrecio(datos.precio());
        producto.setCategoria(datos.categoria());
        producto.setDescripcion(datos.descripcion());
        producto.setImagen(datos.imagen());
        producto.setOferta(datos.oferta());
        producto.setDescuento(datos.descuento());
        producto.setStock(datos.stock());

        return producto;
    }

    // Convertir Entity a DTO
    private ProductoDTO convertirADTO(Producto producto) {

        return new ProductoDTO(
                producto.getId(),
                producto.getNombre(),
                producto.getPrecio(),
                producto.getCategoria(),
                producto.getDescripcion(),
                producto.getImagen(),
                producto.getOferta(),
                producto.getDescuento(),
                producto.getStock()
        );
    }
}
