
package cl.huertohogar.huerto_backend;

import cl.huertohogar.huerto_backend.dto.*;
import cl.huertohogar.huerto_backend.entity.*;
import cl.huertohogar.huerto_backend.repository.*;
import cl.huertohogar.huerto_backend.service.BoletaService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BoletaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private BoletaRepository boletaRepository;

    @Autowired
    private BoletaService boletaService;

    private Usuario cliente;
    private Producto producto;

    @BeforeEach
    void prepararDatos() {

        cliente = crearUsuario(Rol.CLIENTE);

        producto = new Producto();
        producto.setNombre("Platano prueba");
        producto.setPrecio(1000);
        producto.setCategoria("Frutas");
        producto.setDescripcion("Producto para pruebas");
        producto.setImagen("platano.jpg");
        producto.setOferta(false);
        producto.setDescuento(0);
        producto.setStock(10);

        producto = productoRepository.save(producto);
    }

    // PRUEBA 1: compra exitosa
    @Test
    void clientePuedeComprarConStock() throws Exception {

        long boletasAntes = boletaRepository.count();

        mockMvc.perform(
                        post("/api/v1/boletas")
                                .with(autenticar(cliente))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(compra(3))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(3000))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.detalles[0].cantidad").value(3))
                .andExpect(jsonPath("$.detalles[0].subtotal").value(3000));

        Producto actualizado = productoRepository
                .findById(producto.getId())
                .orElseThrow();

        assertEquals(7, actualizado.getStock().intValue());
        assertEquals(boletasAntes + 1, boletaRepository.count());
    }

    // PRUEBA 2: stock insuficiente
    @Test
    void rechazaCompraSinStock() throws Exception {

        long boletasAntes = boletaRepository.count();

        mockMvc.perform(
                        post("/api/v1/boletas")
                                .with(autenticar(cliente))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(compra(15))
                )
                .andExpect(status().isConflict());

        Producto actualizado = productoRepository
                .findById(producto.getId())
                .orElseThrow();

        assertEquals(10, actualizado.getStock().intValue());
        assertEquals(boletasAntes, boletaRepository.count());
    }

    // PRUEBA 3: usuario sin JWT
    @Test
    void visitanteNoPuedeComprar() throws Exception {

        mockMvc.perform(
                        post("/api/v1/boletas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(compra(1))
                )
                .andExpect(status().isUnauthorized());
    }

    // PRUEBA 4: vendedor no puede comprar
    @Test
    void vendedorNoPuedeCrearBoleta() throws Exception {

        Usuario vendedor = crearUsuario(Rol.VENDEDOR);

        mockMvc.perform(
                        post("/api/v1/boletas")
                                .with(autenticar(vendedor))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(compra(1))
                )
                .andExpect(status().isForbidden());
    }

    // PRUEBA 5: cliente no puede listar todas
    @Test
    void clienteNoPuedeListarTodasLasBoletas() throws Exception {

        mockMvc.perform(
                        get("/api/v1/boletas")
                                .with(autenticar(cliente))
                )
                .andExpect(status().isForbidden());
    }

    // PRUEBA 6: vendedor puede consultar todas
    @Test
    void vendedorPuedeListarBoletas() throws Exception {

        Usuario vendedor = crearUsuario(Rol.VENDEDOR);

        mockMvc.perform(
                        get("/api/v1/boletas")
                                .with(autenticar(vendedor))
                )
                .andExpect(status().isOk());
    }

    // PRUEBA 7: cliente puede ver sus compras
    @Test
    void clientePuedeVerSusCompras() throws Exception {

        BoletaRespuestaDTO boleta = crearCompraCliente();

        mockMvc.perform(
                        get("/api/v1/boletas/mis-compras")
                                .with(autenticar(cliente))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(boleta.id()));
    }

    // PRUEBA 8: cliente no puede ver compras ajenas
    @Test
    void clienteNoPuedeVerBoletaAjena() throws Exception {

        BoletaRespuestaDTO boleta = crearCompraCliente();

        Usuario otroCliente = crearUsuario(Rol.CLIENTE);

        mockMvc.perform(
                        get("/api/v1/boletas/{id}", boleta.id())
                                .with(autenticar(otroCliente))
                )
                .andExpect(status().isNotFound());
    }

    // Crear usuarios temporales para las pruebas
    private Usuario crearUsuario(Rol rol) {

        Usuario usuario = new Usuario();
        usuario.setNombre("Usuario de prueba");
        usuario.setCorreo(
                UUID.randomUUID() + "@ejemplo.test"
        );
        usuario.setPasswordHash("hash-de-prueba");
        usuario.setRol(rol);

        return usuarioRepository.save(usuario);
    }

    // Simular un usuario con rol JWT
    private RequestPostProcessor autenticar(Usuario usuario) {

        return jwt()
                .jwt(token -> token
                        .subject(usuario.getCorreo())
                        .claim("rol", usuario.getRol().name())
                )
                .authorities(
                        new SimpleGrantedAuthority(
                                "ROLE_" + usuario.getRol().name()
                        )
                );
    }

    // Preparar el JSON para comprar
    private String compra(int cantidad) {

        return """
                {
                  "detalles": [
                    {
                      "productoId": %d,
                      "cantidad": %d
                    }
                  ]
                }
                """.formatted(producto.getId(), cantidad);
    }

    // Crear una compra de prueba
    private BoletaRespuestaDTO crearCompraCliente() {

        return boletaService.crear(
                new CrearBoletaDTO(
                        List.of(
                                new DetalleCompraDTO(
                                        producto.getId(),
                                        1
                                )
                        )
                ),
                cliente.getCorreo()
        );
    }

    // PRUEBA 9: ADMIN puede registrar una compra
    @Test
    void administradorPuedeComprar() throws Exception {

        Usuario administrador = crearUsuario(Rol.ADMIN);

        mockMvc.perform(
                        post("/api/v1/boletas")
                                .with(autenticar(administrador))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(compra(2))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(2000))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    // PRUEBA 10: aplicar descuento al precio real
    @Test
    void compraCalculaDescuentoCorrectamente()
            throws Exception {

        producto.setOferta(true);
        producto.setDescuento(20);
        productoRepository.save(producto);

        // 1000 - 20% = 800
        // 800 x 3 = 2400

        mockMvc.perform(
                        post("/api/v1/boletas")
                                .with(autenticar(cliente))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(compra(3))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(2400))
                .andExpect(
                        jsonPath("$.detalles[0].precioUnitario")
                                .value(800)
                )
                .andExpect(
                        jsonPath("$.detalles[0].subtotal")
                                .value(2400)
                );
    }

    // PRUEBA 11: rechazar toda la compra
    // si uno de los productos no tiene stock
    @Test
    void compraMultipleRechazadaPorStock()
            throws Exception {

        Producto segundoProducto = new Producto();

        segundoProducto.setNombre("Manzana prueba");
        segundoProducto.setPrecio(1500);
        segundoProducto.setCategoria("Frutas");
        segundoProducto.setDescripcion("Producto de prueba");
        segundoProducto.setImagen("manzana.jpg");
        segundoProducto.setOferta(false);
        segundoProducto.setDescuento(0);
        segundoProducto.setStock(0);

        segundoProducto =
                productoRepository.save(segundoProducto);

        long boletasAntes = boletaRepository.count();

        String json = """
                {
                  "detalles": [
                    {
                      "productoId": %d,
                      "cantidad": 2
                    },
                    {
                      "productoId": %d,
                      "cantidad": 1
                    }
                  ]
                }
                """.formatted(
                producto.getId(),
                segundoProducto.getId()
        );

        mockMvc.perform(
                        post("/api/v1/boletas")
                                .with(autenticar(cliente))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isConflict());

        Producto primeroActualizado = productoRepository
                .findById(producto.getId())
                .orElseThrow();

        assertEquals(
                10,
                primeroActualizado.getStock().intValue()
        );

        assertEquals(
                boletasAntes,
                boletaRepository.count()
        );
    }

}
