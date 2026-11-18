
package cl.huertohogar.huerto_backend.repository;

import cl.huertohogar.huerto_backend.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository
        extends JpaRepository<Producto, Long> {
}
