
package cl.huertohogar.huerto_backend.dto;

public record ProductoDTO(
        Long id,
        String nombre,
        Integer precio,
        String categoria,
        String descripcion,
        String imagen,
        Boolean oferta,
        Integer descuento,
        Integer stock
) {
}
