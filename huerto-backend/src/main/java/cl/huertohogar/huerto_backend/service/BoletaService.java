
package cl.huertohogar.huerto_backend.service;

import cl.huertohogar.huerto_backend.dto.*;
import cl.huertohogar.huerto_backend.entity.*;
import cl.huertohogar.huerto_backend.repository.*;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Service
public class BoletaService {

    private final BoletaRepository boletaRepository;
    private final DetalleBoletaRepository detalleBoletaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;

    public BoletaService(
            BoletaRepository boletaRepository,
            DetalleBoletaRepository detalleBoletaRepository,
            ProductoRepository productoRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.boletaRepository = boletaRepository;
        this.detalleBoletaRepository = detalleBoletaRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public BoletaRespuestaDTO crear(
            CrearBoletaDTO solicitud,
            String correoAutenticado
    ) {

        // 1. Validar que haya un usuario autenticado
        if (correoAutenticado == null
                || correoAutenticado.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Debes iniciar sesion"
            );
        }

        // 2. Buscar al usuario en la base de datos
        String correo = correoAutenticado
                .trim()
                .toLowerCase(Locale.ROOT);

        Usuario usuario = usuarioRepository
                .findByCorreo(correo)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Usuario no encontrado"
                        )
                );

        // 3. Comprobar que el carrito no este vacio
        if (solicitud == null
                || solicitud.detalles() == null
                || solicitud.detalles().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La compra debe contener productos"
            );
        }

        // 4. Validar las cantidades y evitar duplicados
        // TreeMap ordena los IDs para bloquearlos
        // siempre en el mismo orden.
        Map<Long, Integer> cantidades = new TreeMap<>();

        for (DetalleCompraDTO item : solicitud.detalles()) {

            if (item == null
                    || item.productoId() == null
                    || item.productoId() <= 0
                    || item.cantidad() == null
                    || item.cantidad() <= 0) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Producto o cantidad invalida"
                );
            }

            if (cantidades.putIfAbsent(
                    item.productoId(),
                    item.cantidad()
            ) != null) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No repitas productos en la misma compra"
                );
            }
        }

        // 5. Consultar precios y verificar stock
        List<LineaCompra> lineas = new ArrayList<>();

        long totalCalculado = 0;

        for (Map.Entry<Long, Integer> entrada
                : cantidades.entrySet()) {

            Long productoId = entrada.getKey();
            int cantidad = entrada.getValue();

            // Bloquear el producto durante la transaccion
            Producto producto = productoRepository
                    .buscarPorIdParaCompra(productoId)
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Producto no encontrado: " + productoId
                            )
                    );

            // Comprobar stock
            if (producto.getStock() == null
                    || producto.getStock() < cantidad) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Stock insuficiente para "
                                + producto.getNombre()
                );
            }

            // El precio lo determina el backend
            int precioUnitario = calcularPrecioFinal(producto);

            long subtotal = (long) precioUnitario * cantidad;

            // Proteger los campos Integer de valores excesivos
            if (subtotal > Integer.MAX_VALUE
                    || totalCalculado + subtotal > Integer.MAX_VALUE) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El monto de la compra es demasiado alto"
                );
            }

            totalCalculado += subtotal;

            lineas.add(new LineaCompra(
                    producto,
                    cantidad,
                    precioUnitario,
                    (int) subtotal
            ));
        }

        // 6. Crear la boleta
        Boleta boleta = new Boleta();

        boleta.setUsuario(usuario);
        boleta.setFecha(LocalDateTime.now());
        boleta.setTotal((int) totalCalculado);
        boleta.setEstado("PENDIENTE");

        Boleta boletaGuardada = boletaRepository.save(boleta);

        // 7. Guardar detalles y descontar stock
        List<DetalleBoletaRespuestaDTO> detallesRespuesta =
                new ArrayList<>();

        for (LineaCompra linea : lineas) {

            Producto producto = linea.producto();

            producto.setStock(
                    producto.getStock() - linea.cantidad()
            );

            productoRepository.save(producto);

            DetalleBoleta detalle = new DetalleBoleta();

            detalle.setBoleta(boletaGuardada);
            detalle.setProducto(producto);
            detalle.setCantidad(linea.cantidad());
            detalle.setPrecioUnitario(linea.precioUnitario());
            detalle.setSubtotal(linea.subtotal());

            DetalleBoleta detalleGuardado =
                    detalleBoletaRepository.save(detalle);

            detallesRespuesta.add(
                    new DetalleBoletaRespuestaDTO(
                            detalleGuardado.getId(),
                            producto.getId(),
                            producto.getNombre(),
                            detalleGuardado.getCantidad(),
                            detalleGuardado.getPrecioUnitario(),
                            detalleGuardado.getSubtotal()
                    )
            );
        }

        // 8. Devolver la respuesta al frontend
        return new BoletaRespuestaDTO(
                boletaGuardada.getId(),
                usuario.getId(),
                boletaGuardada.getFecha(),
                boletaGuardada.getTotal(),
                boletaGuardada.getEstado(),
                detallesRespuesta
        );
    }

    // Listar todas las boletas: ADMIN y VENDEDOR
    @Transactional(readOnly = true)
    public List<BoletaRespuestaDTO> listarTodas() {

        return boletaRepository
                .findAllByOrderByFechaDesc()
                .stream()
                .map(this::convertirARespuesta)
                .toList();
    }

    // Consultar las compras del usuario autenticado
    @Transactional(readOnly = true)
    public List<BoletaRespuestaDTO> listarMisCompras(
            String correoAutenticado
    ) {

        Usuario usuario = usuarioRepository
                .findByCorreo(
                        correoAutenticado.trim()
                                .toLowerCase(Locale.ROOT)
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Usuario no encontrado"
                        )
                );

        return boletaRepository
                .findByUsuario_IdOrderByFechaDesc(
                        usuario.getId()
                )
                .stream()
                .map(this::convertirARespuesta)
                .toList();
    }

    // Consultar una boleta por ID
    @Transactional(readOnly = true)
    public BoletaRespuestaDTO buscarPorId(
            Long id,
            String correoAutenticado,
            boolean puedeVerTodas
    ) {

        Boleta boleta = boletaRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Boleta no encontrada"
                        )
                );

        // Un CLIENTE solo puede ver sus propias boletas.
        // ADMIN y VENDEDOR pueden consultar todas.
        if (!puedeVerTodas
                && !boleta.getUsuario().getCorreo()
                .equalsIgnoreCase(correoAutenticado)) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Boleta no encontrada"
            );
        }

        return convertirARespuesta(boleta);
    }

    // Convertir la entidad y sus detalles a un DTO
    private BoletaRespuestaDTO convertirARespuesta(
            Boleta boleta
    ) {

        List<DetalleBoletaRespuestaDTO> detalles =
                detalleBoletaRepository
                        .findByBoleta_Id(boleta.getId())
                        .stream()
                        .map(detalle ->
                                new DetalleBoletaRespuestaDTO(
                                        detalle.getId(),
                                        detalle.getProducto().getId(),
                                        detalle.getProducto().getNombre(),
                                        detalle.getCantidad(),
                                        detalle.getPrecioUnitario(),
                                        detalle.getSubtotal()
                                )
                        )
                        .toList();

        return new BoletaRespuestaDTO(
                boleta.getId(),
                boleta.getUsuario().getId(),
                boleta.getFecha(),
                boleta.getTotal(),
                boleta.getEstado(),
                detalles
        );
    }


    // Calcular el precio considerando ofertas
    private int calcularPrecioFinal(Producto producto) {

        Integer precio = producto.getPrecio();

        if (precio == null || precio <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El producto tiene un precio invalido"
            );
        }

        int descuento = 0;

        if (Boolean.TRUE.equals(producto.getOferta())) {
            descuento = producto.getDescuento() == null
                    ? 0
                    : producto.getDescuento();
        }

        if (descuento < 0 || descuento > 100) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El descuento del producto es invalido"
            );
        }

        // Calculo en pesos chilenos, redondeado al entero
        return Math.toIntExact(
                ((long) precio * (100L - descuento) + 50L) / 100L
        );
    }

    // Datos internos usados mientras procesamos la compra
    private record LineaCompra(
            Producto producto,
            int cantidad,
            int precioUnitario,
            int subtotal
    ) {
    }
}
