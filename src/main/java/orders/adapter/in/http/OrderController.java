package orders.adapter.in.http;

import orders.application.port.in.CreateOrderUseCase;
import orders.application.port.in.GetOrderUseCase;
import orders.application.port.in.UpdateOrderStatusUseCase;
import orders.domain.model.OrderItem;
import orders.domain.model.OrderNotFoundException;
import orders.domain.model.OrderStatus;

import java.util.List;
import java.util.Objects;

/**
 * Adaptador HTTP ligero: traduce peticiones/respuestas JSON al protocolo del dominio.
 *
 * <p>Este adaptador depende de los puertos de entrada (interfaces), nunca de
 * implementaciones concretas. Puede envolverse con cualquier framework HTTP
 * (Javalin, Spring MVC, Jakarta Servlets, etc.) sin cambiar la lógica.
 *
 * <p>Rutas conceptuales:
 * <ul>
 *   <li>POST   /orders                  → {@link #handleCreate(CreateOrderRequest)}</li>
 *   <li>GET    /orders/{id}             → {@link #handleGet(String)}</li>
 *   <li>PATCH  /orders/{id}/status      → {@link #handleUpdateStatus(String, String)}</li>
 * </ul>
 */
public class OrderController {

    private final CreateOrderUseCase createOrder;
    private final GetOrderUseCase getOrder;
    private final UpdateOrderStatusUseCase updateStatus;

    /**
     * Constructor con inyección manual de los tres casos de uso.
     *
     * @param createOrder  caso de uso de creación
     * @param getOrder     caso de uso de consulta
     * @param updateStatus caso de uso de actualización de estado
     */
    public OrderController(CreateOrderUseCase createOrder,
                           GetOrderUseCase getOrder,
                           UpdateOrderStatusUseCase updateStatus) {
        this.createOrder  = Objects.requireNonNull(createOrder);
        this.getOrder     = Objects.requireNonNull(getOrder);
        this.updateStatus = Objects.requireNonNull(updateStatus);
    }

    // -------------------------------------------------------------------------
    // Requests / Responses (records internos del adaptador HTTP)
    // -------------------------------------------------------------------------

    /**
     * Cuerpo de la petición POST /orders.
     *
     * @param customerId ID del cliente
     * @param items      lista de ítems
     */
    public record CreateOrderRequest(String customerId, List<ItemRequest> items) {}

    /**
     * Ítem dentro de {@link CreateOrderRequest}.
     *
     * @param productId ID del producto
     * @param qty       cantidad
     * @param price     precio unitario
     */
    public record ItemRequest(String productId, int qty, double price) {}

    /**
     * Respuesta genérica del controlador.
     *
     * @param id         ID del pedido
     * @param customerId ID del cliente
     * @param status     estado actual
     * @param total      precio total
     * @param itemCount  número de ítems
     */
    public record OrderResponse(String id, String customerId, String status,
                                double total, int itemCount) {

        /** Construye una OrderResponse a partir de un Order de dominio. */
        public static OrderResponse from(orders.domain.model.Order order) {
            return new OrderResponse(
                    order.id(),
                    order.customerId(),
                    order.status().name(),
                    order.total(),
                    order.items().size()
            );
        }
    }

    // -------------------------------------------------------------------------
    // Handlers
    // -------------------------------------------------------------------------

    /**
     * Maneja POST /orders — crea un nuevo pedido.
     *
     * <p>Postcondición: retorna la respuesta con los datos del pedido creado (HTTP 201).
     *
     * @param req datos de la petición
     * @return respuesta con el pedido creado
     * @throws IllegalArgumentException si la petición viola precondiciones
     */
    public OrderResponse handleCreate(CreateOrderRequest req) {
        Objects.requireNonNull(req, "request must not be null");

        List<OrderItem> items = req.items().stream()
                .map(i -> new OrderItem(i.productId(), i.qty(), i.price()))
                .toList();

        var cmd = new CreateOrderUseCase.Command(req.customerId(), items);
        return OrderResponse.from(createOrder.execute(cmd));   // → HTTP 201
    }

    /**
     * Maneja GET /orders/{id} — obtiene un pedido por su ID.
     *
     * <p>Postcondición: retorna el pedido (HTTP 200) o lanza {@link OrderNotFoundException} (→ 404).
     *
     * @param id identificador del pedido
     * @return respuesta con los datos del pedido
     * @throws OrderNotFoundException si no existe el pedido
     */
    public OrderResponse handleGet(String id) {
        return OrderResponse.from(getOrder.execute(id));       // → HTTP 200 o 404
    }

    /**
     * Maneja PATCH /orders/{id}/status — actualiza el estado de un pedido.
     *
     * <p>Precondición: {@code status} debe ser un valor válido de {@link OrderStatus}.
     * <p>Postcondición: retorna el pedido con el estado actualizado (HTTP 200).
     *
     * @param id     identificador del pedido
     * @param status nuevo estado como String (nombre del enum)
     * @return respuesta con el pedido actualizado
     * @throws IllegalArgumentException si el status no es un valor válido de OrderStatus
     * @throws IllegalStateException    si la transición de estado no es válida
     * @throws OrderNotFoundException   si no existe el pedido
     */
    public OrderResponse handleUpdateStatus(String id, String status) {
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid order status: " + status);
        }
        var cmd = new UpdateOrderStatusUseCase.Command(id, newStatus);
        return OrderResponse.from(updateStatus.execute(cmd));
    }
}
