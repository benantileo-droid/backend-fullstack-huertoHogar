
package cl.huertohogar.huerto_backend.service;

import cl.huertohogar.huerto_backend.dto.ProductoDTO;
import cl.huertohogar.huerto_backend.entity.Producto;
import cl.huertohogar.huerto_backend.repository.ProductoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    // GET: Listar todos los productos
    public List<ProductoDTO> listar() {
        return productoRepository.findAll()
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    // GET: Buscar producto por ID
    public Optional<ProductoDTO> buscarPorId(Long id) {
        return productoRepository.findById(id)
                .map(this::convertirADTO);
    }

    // POST: Crear producto en MySQL
    public ProductoDTO crear(ProductoDTO datos) {

        Producto producto = convertirAEntidad(datos);

        Producto guardado = productoRepository.save(producto);

        return convertirADTO(guardado);
    }

    // PUT: Actualizar producto en MySQL
    @Transactional
    public Optional<ProductoDTO> actualizar(
            Long id, ProductoDTO datos) {

        return productoRepository.findById(id)
                .map(producto -> {

                    producto.setNombre(datos.nombre());
                    producto.setPrecio(datos.precio());
                    producto.setCategoria(datos.categoria());
                    producto.setDescripcion(datos.descripcion());
                    producto.setImagen(datos.imagen());
                    producto.setOferta(datos.oferta());
                    producto.setDescuento(datos.descuento());
                    producto.setStock(datos.stock());

                    Producto actualizado =
                            productoRepository.save(producto);

                    return convertirADTO(actualizado);
                });
    }

    // DELETE: Eliminar producto de MySQL
    @Transactional
    public Optional<ProductoDTO> eliminar(Long id) {

        return productoRepository.findById(id)
                .map(producto -> {

                    ProductoDTO eliminado = convertirADTO(producto);

                    productoRepository.delete(producto);

                    return eliminado;
                });
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
