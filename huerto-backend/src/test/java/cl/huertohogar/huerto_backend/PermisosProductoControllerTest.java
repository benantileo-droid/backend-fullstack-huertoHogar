
package cl.huertohogar.huerto_backend;

import cl.huertohogar.huerto_backend.entity.Producto;
import cl.huertohogar.huerto_backend.entity.Rol;
import cl.huertohogar.huerto_backend.entity.Usuario;
import cl.huertohogar.huerto_backend.repository.ProductoRepository;
import cl.huertohogar.huerto_backend.repository.UsuarioRepository;
import cl.huertohogar.huerto_backend.service.JwtService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PermisosProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    // Crear usuario temporal y obtener su JWT
    private String crearToken(Rol rol) {

        Usuario usuario = new Usuario();

        usuario.setNombre("Usuario de prueba");
        usuario.setCorreo(
                "rol." + UUID.randomUUID() + "@ejemplo.com"
        );
        usuario.setPasswordHash(
                passwordEncoder.encode("ClavePrueba123!")
        );
        usuario.setRol(rol);

        Usuario guardado =
                usuarioRepository.saveAndFlush(usuario);

        return jwtService.generarToken(guardado).token();
    }

    // JSON valido para las solicitudes
    private String productoJson() {
        return """
            {
                "nombre": "Manzana de prueba",
                "precio": 1200,
                "categoria": "Frutas",
                "descripcion": "Producto para probar permisos",
                "imagen": "manzana.jpg",
                "oferta": false,
                "descuento": 0,
                "stock": 20
            }
            """;
    }

    // Crear producto temporal directamente en MySQL
    private Producto crearProductoTemporal() {

        Producto producto = new Producto();

        producto.setNombre("Producto de prueba");
        producto.setPrecio(1500);
        producto.setCategoria("Frutas");
        producto.setDescripcion("Prueba de seguridad");
        producto.setImagen("prueba.jpg");
        producto.setOferta(false);
        producto.setDescuento(0);
        producto.setStock(10);

        return productoRepository.saveAndFlush(producto);
    }

    // PRUEBA 1: Visitante sin JWT no puede crear
    @Test
    void visitanteNoPuedeCrearProducto() throws Exception {

        mockMvc.perform(
                        post("/api/v1/productos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(productoJson())
                )
                .andExpect(status().isUnauthorized());
    }

    // PRUEBA 2: CLIENTE no puede crear
    @Test
    void clienteNoPuedeCrearProducto() throws Exception {

        String token = crearToken(Rol.CLIENTE);

        mockMvc.perform(
                        post("/api/v1/productos")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(productoJson())
                )
                .andExpect(status().isForbidden());
    }

    // PRUEBA 3: VENDEDOR no puede crear
    @Test
    void vendedorNoPuedeCrearProducto() throws Exception {

        String token = crearToken(Rol.VENDEDOR);

        long cantidadInicial = productoRepository.count();

        mockMvc.perform(
                        post("/api/v1/productos")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(productoJson())
                )
                .andExpect(status().isForbidden());

        assertEquals(
                cantidadInicial,
                productoRepository.count()
        );
    }

    // PRUEBA 4: VENDEDOR no puede actualizar
    @Test
    void vendedorNoPuedeActualizarProducto() throws Exception {

        String token = crearToken(Rol.VENDEDOR);

        Producto producto = crearProductoTemporal();

        Long id = producto.getId();

        mockMvc.perform(
                        put("/api/v1/productos/{id}", id)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(productoJson())
                )
                .andExpect(status().isForbidden());

        productoRepository.flush();

        // El precio debe seguir siendo el original
        assertEquals(
                1500,
                productoRepository.findById(id)
                        .orElseThrow()
                        .getPrecio()
                        .intValue()
        );
    }

    // PRUEBA 5: CLIENTE no puede actualizar
    @Test
    void clienteNoPuedeActualizarProducto() throws Exception {

        String token = crearToken(Rol.CLIENTE);

        mockMvc.perform(
                        put("/api/v1/productos/{id}", 999999L)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(productoJson())
                )
                .andExpect(status().isForbidden());
    }

    // PRUEBA 6: VENDEDOR no puede eliminar
    @Test
    void vendedorNoPuedeEliminarProducto() throws Exception {

        String token = crearToken(Rol.VENDEDOR);

        Producto producto = crearProductoTemporal();

        Long id = producto.getId();

        mockMvc.perform(
                        delete("/api/v1/productos/{id}", id)
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isForbidden());

        // Comprobar que el producto sigue existiendo
        assertTrue(productoRepository.existsById(id));
    }

    // PRUEBA 7: ADMIN puede eliminar
    @Test
    void adminPuedeEliminarProducto() throws Exception {

        String token = crearToken(Rol.ADMIN);

        Producto producto = crearProductoTemporal();

        Long id = producto.getId();

        mockMvc.perform(
                        delete("/api/v1/productos/{id}", id)
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isNoContent());

        productoRepository.flush();

        assertFalse(productoRepository.existsById(id));
    }

    // PRUEBA 8: Un JWT invalido debe rechazarse
    @Test
    void jwtInvalidoNoPuedeCrearProducto() throws Exception {

        mockMvc.perform(
                        post("/api/v1/productos")
                                .header(
                                        "Authorization",
                                        "Bearer token.invalido"
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(productoJson())
                )
                .andExpect(status().isUnauthorized());
    }
}
