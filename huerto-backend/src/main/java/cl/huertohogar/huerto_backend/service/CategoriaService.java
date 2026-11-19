
package cl.huertohogar.huerto_backend.service;

import cl.huertohogar.huerto_backend.entity.Categoria;
import cl.huertohogar.huerto_backend.entity.Producto;
import cl.huertohogar.huerto_backend.repository.CategoriaRepository;
import cl.huertohogar.huerto_backend.repository.ProductoRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    public CategoriaService(
            CategoriaRepository categoriaRepository,
            ProductoRepository productoRepository
    ) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
    }

    // ==========================================
    // LISTAR CATEGORIAS
    // ==========================================

    @Transactional(readOnly = true)
    public List<Categoria> listar() {
        return categoriaRepository.findAll();
    }

    // ==========================================
    // OBTENER CATEGORIA POR ID
    // ==========================================

    @Transactional(readOnly = true)
    public Categoria obtenerPorId(Long id) {

        return categoriaRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "La categoría no existe."
                        )
                );
    }

    // ==========================================
    // CREAR CATEGORIA
    // ==========================================

    @Transactional
    public Categoria crear(
            String nombre,
            String descripcion
    ) {

        String nombreLimpio = validarNombre(nombre);

        if (categoriaRepository.existsByNombreIgnoreCase(nombreLimpio)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe una categoría con ese nombre."
            );
        }

        Categoria categoria = new Categoria();

        categoria.setNombre(nombreLimpio);
        categoria.setDescripcion(
                limpiarDescripcion(descripcion)
        );

        return categoriaRepository.save(categoria);
    }

    // ==========================================
    // ACTUALIZAR CATEGORIA
    // ==========================================

    @Transactional
    public Categoria actualizar(
            Long id,
            String nombre,
            String descripcion
    ) {

        Categoria categoria = obtenerPorId(id);

        String nombreNuevo = validarNombre(nombre);
        String nombreAnterior = categoria.getNombre();

        boolean cambioNombre =
                !nombreAnterior.equals(nombreNuevo);

        if (
                !nombreAnterior.equalsIgnoreCase(nombreNuevo) &&
                        categoriaRepository.existsByNombreIgnoreCase(nombreNuevo)
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe otra categoría con ese nombre."
            );
        }

        categoria.setNombre(nombreNuevo);

        categoria.setDescripcion(
                limpiarDescripcion(descripcion)
        );

        Categoria guardada =
                categoriaRepository.save(categoria);

        // Los productos conservan el ID de categoría.
        // También sincronizamos el nombre de categoría
        // que todavía existe como String en Producto.

        if (cambioNombre) {

            List<Producto> productos =
                    productoRepository.findAll();

            for (Producto producto : productos) {

                if (
                        perteneceACategoria(
                                producto,
                                guardada,
                                nombreAnterior
                        )
                ) {

                    producto.setCategoria(nombreNuevo);
                    producto.setCategoriaEntidad(guardada);

                    productoRepository.save(producto);
                }
            }
        }

        return guardada;
    }

    // ==========================================
    // ELIMINAR CATEGORIA
    // ==========================================

    @Transactional
    public void eliminar(Long id) {

        Categoria categoria = obtenerPorId(id);

        // Evitamos eliminar categorías que
        // tengan productos asociados.

        boolean tieneProductos =
                productoRepository.findAll()
                        .stream()
                        .anyMatch(producto ->
                                perteneceACategoria(
                                        producto,
                                        categoria,
                                        categoria.getNombre()
                                )
                        );

        if (tieneProductos) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No puedes eliminar una categoría " +
                            "que tiene productos asociados."
            );
        }

        categoriaRepository.delete(categoria);
    }

    // ==========================================
    // VALIDACIONES
    // ==========================================

    private String validarNombre(String nombre) {

        if (nombre == null || nombre.trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre de la categoría es obligatorio."
            );
        }

        return nombre.trim();
    }

    private String limpiarDescripcion(String descripcion) {

        return descripcion == null
                ? ""
                : descripcion.trim();
    }

    // ==========================================
    // RELACION CON PRODUCTOS
    // ==========================================

    private boolean perteneceACategoria(
            Producto producto,
            Categoria categoria,
            String nombreAnterior
    ) {

        boolean coincideRelacion =
                producto.getCategoriaEntidad() != null &&
                        Objects.equals(
                                producto.getCategoriaEntidad().getId(),
                                categoria.getId()
                        );

        boolean coincideNombre =
                producto.getCategoria() != null &&
                        producto.getCategoria()
                                .equalsIgnoreCase(nombreAnterior);

        return coincideRelacion || coincideNombre;
    }
}
