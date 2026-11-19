
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

    // Crear un usuario temporal con el rol indicado
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

        Usuario guardado = usuarioRepository.saveAndFlush(usuario);

        return jwtService.generarToken(guardado).token();
    }

    // Datos validos para crear un producto
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

    // 1. Un visitante sin JWT no puede crear productos
    @Test
    void visitanteNoPuedeCrearProducto() throws Exception {

        mockMvc.perform(
                        post("/api/v1/productos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(productoJson())
                )
                .andExpect(status().isUnauthorized());
    }

    // 2. Un CLIENTE con JWT no puede crear productos
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

    // 3. Un VENDEDOR puede crear productos
    @Test
    void vendedorPuedeCrearProducto() throws Exception {

        String token = crearToken(Rol.VENDEDOR);

        mockMvc.perform(
                        post("/api/v1/productos")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(productoJson())
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre")
                        .value("Manzana de prueba"));
    }

    // 4. Un VENDEDOR no puede eliminar productos
    @Test
    void vendedorNoPuedeEliminarProducto() throws Exception {

        String token = crearToken(Rol.VENDEDOR);

        mockMvc.perform(
                        delete("/api/v1/productos/{id}", 999999L)
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isForbidden());
    }

    // 5. Un ADMIN puede eliminar productos
    @Test
    void adminPuedeEliminarProducto() throws Exception {

        String token = crearToken(Rol.ADMIN);

        Producto producto = new Producto();

        producto.setNombre("Producto para eliminar");
        producto.setPrecio(1500);
        producto.setCategoria("Frutas");
        producto.setDescripcion("Prueba ADMIN");
        producto.setImagen("prueba.jpg");
        producto.setOferta(false);
        producto.setDescuento(0);
        producto.setStock(10);

        Producto guardado =
                productoRepository.saveAndFlush(producto);

        Long id = guardado.getId();

        mockMvc.perform(
                        delete("/api/v1/productos/{id}", id)
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isNoContent());

        productoRepository.flush();

        assertFalse(productoRepository.existsById(id));
    }

    @Test
    void vendedorPuedeActualizarProducto() throws Exception {

        // Generar un JWT de VENDEDOR
        String token = crearToken(Rol.VENDEDOR);

        // Crear un producto para actualizar
        String respuesta = mockMvc.perform(
                        post("/api/v1/productos")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(productoJson())
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Obtener el ID del producto creado
        Number idGenerado =
                com.jayway.jsonpath.JsonPath.read(
                        respuesta, "$.id"
                );

        long id = idGenerado.longValue();

        // Nuevos datos del producto
        String actualizadoJson = """
        {
            "nombre": "Manzana actualizada",
            "precio": 2500,
            "categoria": "Frutas",
            "descripcion": "Precio actualizado por vendedor",
            "imagen": "manzana.jpg",
            "oferta": false,
            "descuento": 0,
            "stock": 20
        }
        """;

        // Actualizar utilizando PUT
        mockMvc.perform(
                        put("/api/v1/productos/{id}", id)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(actualizadoJson)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precio").value(2500));

        // Comprobar el cambio en MySQL
        productoRepository.flush();

        assertEquals(
                2500,
                productoRepository.findById(id)
                        .orElseThrow()
                        .getPrecio()
                        .intValue()
        );
    }


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
