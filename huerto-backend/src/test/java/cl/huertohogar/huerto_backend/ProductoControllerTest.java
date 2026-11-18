
package cl.huertohogar.huerto_backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import cl.huertohogar.huerto_backend.repository.ProductoRepository;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductoControllerTest {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void crearProducto() throws Exception {

        String productoJson = """
            {
                "nombre": "Platano",
                "precio": 1490,
                "categoria": "Frutas",
                "descripcion": "Platanos frescos",
                "imagen": "platano.jpg",
                "oferta": false,
                "descuento": 0,
                "stock": 20
            }
            """;

        // Simulamos una solicitud POST
        mockMvc.perform(post("/api/v1/productos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson))

                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Platano"))
                .andExpect(jsonPath("$.id").isNumber());

        // Comprobamos que aparece en el catálogo
        mockMvc.perform(get("/api/v1/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$[?(@.nombre == 'Platano')]"
                ).exists());
    }
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void noDebeCrearProductoInvalido() throws Exception {

        String productoJson = """
        {
            "nombre": "",
            "precio": -1500,
            "categoria": "Frutas",
            "descripcion": "Producto de prueba",
            "imagen": "prueba.jpg",
            "oferta": false,
            "descuento": 0,
            "stock": -10
        }
        """;

        mockMvc.perform(post("/api/v1/productos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson))

                .andExpect(status().isBadRequest());
    }
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void actualizarProducto() throws Exception {

        String productoActualizado = """
        {
            "nombre": "Manzana Roja",
            "precio": 1500,
            "categoria": "Frutas",
            "descripcion": "Manzanas frescas",
            "imagen": "manzana.jpg",
            "oferta": false,
            "descuento": 0,
            "stock": 30
        }
        """;

        // Enviamos PUT para actualizar la manzana
        mockMvc.perform(put("/api/v1/productos/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoActualizado))

                // Verificamos que la actualización funcionó
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.precio").value(1500));

        // Consultamos nuevamente el producto
        mockMvc.perform(get("/api/v1/productos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precio").value(1500));
    }
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void noDebeActualizarProductoInexistente() throws Exception {

        String productoJson = """
        {
            "nombre": "Manzana Verde",
            "precio": 1500,
            "categoria": "Frutas",
            "descripcion": "Manzanas verdes",
            "imagen": "manzana-verde.jpg",
            "oferta": false,
            "descuento": 0,
            "stock": 20
        }
        """;

        mockMvc.perform(put("/api/v1/productos/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson))

                .andExpect(status().isNotFound());
    }
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void noDebeActualizarConPrecioNegativo() throws Exception {

        String productoJson = """
        {
            "nombre": "Manzana Roja",
            "precio": -1500,
            "categoria": "Frutas",
            "descripcion": "Manzanas frescas",
            "imagen": "manzana.jpg",
            "oferta": false,
            "descuento": 0,
            "stock": 30
        }
        """;

        mockMvc.perform(put("/api/v1/productos/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson))

                .andExpect(status().isBadRequest());
    }
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void eliminarProducto() throws Exception {

        // Producto que utilizaremos para la prueba
        String productoJson = """
        {
            "nombre": "Pera de prueba",
            "precio": 1100,
            "categoria": "Frutas",
            "descripcion": "Pera para probar DELETE",
            "imagen": "pera.jpg",
            "oferta": false,
            "descuento": 0,
            "stock": 10
        }
        """;

        // Primero creamos un producto
        String respuesta = mockMvc.perform(
                        post("/api/v1/productos")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(productoJson))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Obtenemos el ID que generó el backend
        Number id = com.jayway.jsonpath.JsonPath.read(
                respuesta, "$.id"
        );

        // Eliminamos el producto
        mockMvc.perform(
                        delete("/api/v1/productos/{id}", id)
                                .with(csrf()))
                .andExpect(status().isNoContent());

        // Comprobamos que ya no existe
        mockMvc.perform(
                        get("/api/v1/productos/{id}", id))
                .andExpect(status().isNotFound());
    }
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void noDebeEliminarProductoInexistente() throws Exception {

        // Intentamos eliminar un producto que no existe
        mockMvc.perform(
                        delete("/api/v1/productos/99")
                                .with(csrf()))

                // Esperamos HTTP 404 Not Found
                .andExpect(status().isNotFound());
    }
    @Test
    @Transactional
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void crearProductoEnMySQL() throws Exception {

        // Contamos los productos antes de comenzar
        long cantidadInicial = productoRepository.count();

        // Producto que utilizaremos en la prueba
        String productoJson = """
        {
            "nombre": "Manzana de prueba MySQL",
            "precio": 1200,
            "categoria": "Frutas",
            "descripcion": "Manzanas frescas",
            "imagen": "manzana.jpg",
            "oferta": false,
            "descuento": 0,
            "stock": 30
        }
        """;

        // Simulamos el POST
        mockMvc.perform(post("/api/v1/productos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson))

                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre")
                        .value("Manzana de prueba MySQL"))
                .andExpect(jsonPath("$.precio").value(1200));

        // Comprobamos que MySQL tiene un producto mas
        assertEquals(
                cantidadInicial + 1,
                productoRepository.count()
        );
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void actualizarProductoEnMySQL() throws Exception {

        // 1. Creamos un producto de prueba
        String productoJson = """
        {
            "nombre": "Manzana de prueba PUT",
            "precio": 1200,
            "categoria": "Frutas",
            "descripcion": "Manzanas frescas",
            "imagen": "manzana.jpg",
            "oferta": false,
            "descuento": 0,
            "stock": 30
        }
        """;

        String respuesta = mockMvc.perform(
                        post("/api/v1/productos")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(productoJson))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 2. Obtenemos el ID generado por MySQL
        Number idGenerado =
                com.jayway.jsonpath.JsonPath.read(
                        respuesta, "$.id"
                );

        long id = idGenerado.longValue();

        // 3. Preparamos los nuevos datos
        String productoActualizado = """
        {
            "nombre": "Manzana de prueba PUT",
            "precio": 1500,
            "categoria": "Frutas",
            "descripcion": "Manzanas frescas",
            "imagen": "manzana.jpg",
            "oferta": false,
            "descuento": 0,
            "stock": 30
        }
        """;

        // 4. Actualizamos mediante PUT
        mockMvc.perform(
                        put("/api/v1/productos/{id}", id)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(productoActualizado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.precio").value(1500));

        // 5. Verificamos la actualización mediante GET
        mockMvc.perform(
                        get("/api/v1/productos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precio").value(1500));

        // 6. Comprobamos mediante el Repository
        productoRepository.flush();

        assertEquals(
                1500,
                productoRepository.findById(id)
                        .orElseThrow()
                        .getPrecio()
                        .intValue()
        );
    }


}
