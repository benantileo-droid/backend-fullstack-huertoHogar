
package cl.huertohogar.huerto_backend;

import cl.huertohogar.huerto_backend.dto.ProductoDTO;
import cl.huertohogar.huerto_backend.entity.Categoria;
import cl.huertohogar.huerto_backend.entity.Producto;
import cl.huertohogar.huerto_backend.repository.CategoriaRepository;
import cl.huertohogar.huerto_backend.repository.ProductoRepository;
import cl.huertohogar.huerto_backend.service.ProductoService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ProductoCategoriaTest {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Test
    void debeRelacionarProductosConCategoria() {

        // 1. Crear un nombre unico para la prueba
        String nombreCategoria = "FrutasPrueba"
                + UUID.randomUUID().toString().replace("-", "");

        long categoriasIniciales = categoriaRepository.count();

        // 2. Crear una manzana
        ProductoDTO manzana = productoService.crear(
                new ProductoDTO(
                        null,
                        "Manzana de prueba",
                        1200,
                        nombreCategoria,
                        "Manzana fresca",
                        "manzana.jpg",
                        false,
                        0,
                        20
                )
        );

        // 3. Crear una pera con la misma categoria
        ProductoDTO pera = productoService.crear(
                new ProductoDTO(
                        null,
                        "Pera de prueba",
                        1500,
                        nombreCategoria.toLowerCase(Locale.ROOT),
                        "Pera fresca",
                        "pera.jpg",
                        false,
                        0,
                        15
                )
        );

        // 4. Ejecutar las operaciones SQL pendientes
        productoRepository.flush();
        categoriaRepository.flush();

        // 5. Buscar la categoria en MySQL
        Categoria categoria = categoriaRepository
                .findByNombreIgnoreCase(nombreCategoria)
                .orElseThrow();

        // 6. Buscar ambos productos
        Producto manzanaGuardada = productoRepository
                .findById(manzana.id())
                .orElseThrow();

        Producto peraGuardada = productoRepository
                .findById(pera.id())
                .orElseThrow();

        // 7. Comprobar que tienen una categoria relacionada
        assertNotNull(manzanaGuardada.getCategoriaEntidad());
        assertNotNull(peraGuardada.getCategoriaEntidad());

        // 8. Comprobar que ambos usan la misma categoria
        assertEquals(
                categoria.getId(),
                manzanaGuardada.getCategoriaEntidad().getId()
        );

        assertEquals(
                categoria.getId(),
                peraGuardada.getCategoriaEntidad().getId()
        );

        // 9. Comprobar que solo se creo una categoria
        assertEquals(
                categoriasIniciales + 1,
                categoriaRepository.count()
        );
    }
}
