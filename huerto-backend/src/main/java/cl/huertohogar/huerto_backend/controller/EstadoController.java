
package cl.huertohogar.huerto_backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class EstadoController {

    @GetMapping("/estado")
    public Map<String, String> estado() {
        return Map.of(
                "aplicacion", "Huerto Hogar",
                "estado", "funcionando"
        );
    }
}
