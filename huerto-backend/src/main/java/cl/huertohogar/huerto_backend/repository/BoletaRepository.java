
package cl.huertohogar.huerto_backend.repository;

import cl.huertohogar.huerto_backend.entity.Boleta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoletaRepository
        extends JpaRepository<Boleta, Long> {

    // Buscar las boletas de un usuario
    List<Boleta> findByUsuario_IdOrderByFechaDesc(
            Long usuarioId
    );

    // Listar todas las boletas, de la mas reciente
    // a la mas antigua
    List<Boleta> findAllByOrderByFechaDesc();
}
