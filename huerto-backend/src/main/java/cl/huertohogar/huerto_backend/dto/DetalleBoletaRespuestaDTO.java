
package cl.huertohogar.huerto_backend.dto;

public record DetalleBoletaRespuestaDTO(

        Long id,

        Long productoId,

        String nombreProducto,

        Integer cantidad,

        Integer precioUnitario,

        Integer subtotal

) {
}
