
package cl.huertohogar.huerto_backend.repository;

import cl.huertohogar.huerto_backend.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoriaRepository
        extends JpaRepository<Categoria, Long> {

    // Buscar categoria por nombre
    Optional<Categoria> findByNombreIgnoreCase(String nombre);

    // Comprobar si existe una categoria
    boolean existsByNombreIgnoreCase(String nombre);
}
