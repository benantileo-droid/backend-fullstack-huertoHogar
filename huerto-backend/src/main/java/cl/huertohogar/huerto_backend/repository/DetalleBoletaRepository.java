
package cl.huertohogar.huerto_backend.repository;

import cl.huertohogar.huerto_backend.entity.DetalleBoleta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleBoletaRepository
        extends JpaRepository<DetalleBoleta, Long> {

    // Buscar los productos de una boleta
    List<DetalleBoleta> findByBoleta_Id(Long boletaId);
}
