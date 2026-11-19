
package cl.huertohogar.huerto_backend.service;

import cl.huertohogar.huerto_backend.dto.TokenRespuestaDTO;
import cl.huertohogar.huerto_backend.entity.Usuario;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;

    // El token durara 1 hora (3600 segundos)
    private static final long DURACION_TOKEN = 3600;

    public JwtService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    // Generar JWT para un usuario autenticado
    public TokenRespuestaDTO generarToken(Usuario usuario) {

        // 1. Obtener el momento actual
        Instant ahora = Instant.now();

        // 2. Definir la informacion del token
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("huerto-hogar")
                .issuedAt(ahora)
                .expiresAt(
                        ahora.plusSeconds(DURACION_TOKEN)
                )
                .subject(usuario.getCorreo())
                .claim("usuarioId", usuario.getId())
                .claim("rol", usuario.getRol().name())
                .build();

        // 3. Indicar el algoritmo de firma
        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        // 4. Generar el token firmado
        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();

        // 5. Devolver el JWT
        return new TokenRespuestaDTO(
                token,
                "Bearer",
                DURACION_TOKEN
        );
    }
}
