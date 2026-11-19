
package cl.huertohogar.huerto_backend.repository;

import cl.huertohogar.huerto_backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository
        extends JpaRepository<Usuario, Long> {

    // Buscar usuario por correo
    Optional<Usuario> findByCorreo(String correo);

    // Comprobar si ya existe ese correo
    boolean existsByCorreo(String correo);
}
