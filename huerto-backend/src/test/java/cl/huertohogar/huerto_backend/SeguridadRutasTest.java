
package cl.huertohogar.huerto_backend;

import cl.huertohogar.huerto_backend.config.SecurityConfig;
import cl.huertohogar.huerto_backend.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;

import org.springframework.http.ResponseEntity;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ==========================================
// PRUEBAS DE SEGURIDAD - HUERTO HOGAR
// ==========================================

// Probamos las reglas reales de SecurityConfig
// con controladores simulados.
//
// No necesitamos conectar MySQL ni modificar
// productos, usuarios o boletas.

@WebMvcTest(
        controllers = SeguridadRutasTest.ControladorPrueba.class
)
@Import({
        SecurityConfig.class,
        SeguridadRutasTest.ControladorPrueba.class
})
class SeguridadRutasTest {

    @Autowired
    private MockMvc mockMvc;

    // ======================================
    // DEPENDENCIAS SIMULADAS
    // ======================================

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // ======================================
    // CONTROLADOR DE PRUEBA
    // ======================================

    @RestController
    public static class ControladorPrueba {

        @GetMapping("/api/v1/productos")
        public ResponseEntity<String> productos() {

            return ResponseEntity.ok(
                    "PRODUCTOS"
            );
        }

        @GetMapping("/api/v1/boletas")
        public ResponseEntity<String> boletas() {

            return ResponseEntity.ok(
                    "BOLETAS"
            );
        }

        @GetMapping("/api/v1/boletas/mis-compras")
        public ResponseEntity<String> misCompras() {

            return ResponseEntity.ok(
                    "MIS COMPRAS"
            );
        }

        @GetMapping("/api/v1/admin/usuarios")
        public ResponseEntity<String> usuarios() {

            return ResponseEntity.ok(
                    "USUARIOS"
            );
        }
    }

    // ======================================
    // PRUEBA 1
    // PRODUCTOS PUBLICOS
    // ======================================

    @Test
    void productosSinSesionDebePermitirAcceso()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/productos")
                )
                .andExpect(status().isOk());
    }

    // ======================================
    // PRUEBA 2
    // SIN TOKEN NO PUEDE VER BOLETAS
    // ======================================

    @Test
    void boletasSinSesionDebeResponder401()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/boletas")
                )
                .andExpect(status().isUnauthorized());
    }

    // ======================================
    // PRUEBA 3
    // ADMIN PUEDE CONSULTAR USUARIOS
    // ======================================

    @Test
    void adminPuedeConsultarUsuarios()
            throws Exception {

        mockMvc.perform(

                        get("/api/v1/admin/usuarios")

                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority(
                                                "ROLE_ADMIN"
                                        )
                                ))

                )
                .andExpect(status().isOk());
    }

    // ======================================
    // PRUEBA 4
    // VENDEDOR NO PUEDE CONSULTAR USUARIOS
    // ======================================

    @Test
    void vendedorNoPuedeConsultarUsuarios()
            throws Exception {

        mockMvc.perform(

                        get("/api/v1/admin/usuarios")

                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority(
                                                "ROLE_VENDEDOR"
                                        )
                                ))

                )
                .andExpect(status().isForbidden());
    }

    // ======================================
    // PRUEBA 5
    // CLIENTE NO PUEDE CONSULTAR USUARIOS
    // ======================================

    @Test
    void clienteNoPuedeConsultarUsuarios()
            throws Exception {

        mockMvc.perform(

                        get("/api/v1/admin/usuarios")

                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority(
                                                "ROLE_CLIENTE"
                                        )
                                ))

                )
                .andExpect(status().isForbidden());
    }

    // ======================================
    // PRUEBA 6
    // VENDEDOR PUEDE CONSULTAR BOLETAS
    // ======================================

    @Test
    void vendedorPuedeConsultarBoletas()
            throws Exception {

        mockMvc.perform(

                        get("/api/v1/boletas")

                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority(
                                                "ROLE_VENDEDOR"
                                        )
                                ))

                )
                .andExpect(status().isOk());
    }

    // ======================================
    // PRUEBA 7
    // CLIENTE NO PUEDE CONSULTAR TODAS
    // LAS BOLETAS
    // ======================================

    @Test
    void clienteNoPuedeConsultarTodasLasBoletas()
            throws Exception {

        mockMvc.perform(

                        get("/api/v1/boletas")

                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority(
                                                "ROLE_CLIENTE"
                                        )
                                ))

                )
                .andExpect(status().isForbidden());
    }

    // ======================================
    // PRUEBA 8
    // CLIENTE PUEDE CONSULTAR MIS COMPRAS
    // ======================================

    @Test
    void clientePuedeConsultarMisCompras()
            throws Exception {

        mockMvc.perform(

                        get("/api/v1/boletas/mis-compras")

                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority(
                                                "ROLE_CLIENTE"
                                        )
                                ))

                )
                .andExpect(status().isOk());
    }

    // ======================================
    // PRUEBA 9
    // VENDEDOR NO PUEDE CONSULTAR
    // MIS COMPRAS
    // ======================================

    @Test
    void vendedorNoPuedeConsultarMisCompras()
            throws Exception {

        mockMvc.perform(

                        get("/api/v1/boletas/mis-compras")

                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority(
                                                "ROLE_VENDEDOR"
                                        )
                                ))

                )
                .andExpect(status().isForbidden());
    }

    // ======================================
    // PRUEBA 10
    // ADMIN PUEDE CONSULTAR BOLETAS
    // ======================================

    @Test
    void adminPuedeConsultarBoletas()
            throws Exception {

        mockMvc.perform(

                        get("/api/v1/boletas")

                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority(
                                                "ROLE_ADMIN"
                                        )
                                ))

                )
                .andExpect(status().isOk());
    }
}
