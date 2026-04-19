package orders.adapter.out.persistence;

import orders.domain.model.Order;
import orders.domain.model.OrderItem;
import orders.domain.model.OrderStatus;

import java.util.List;

/**
 * DTO de transferencia para serialización/deserialización HTTP.
 * Desacopla la representación externa (JSON/HTTP) del modelo de dominio.
 *
 * @param id         identificador del pedido
 * @param customerId identificador del cliente
 * @param items      lista de ítems como DTOs
 * @param status     estado del pedido como String
 * @param total      precio total del pedido
 */
public record OrderDto(
        String id,
        String customerId,
        List<ItemDto> items,
        String status,
        double total
) {

    /** DTO de un ítem individual. */
    public record ItemDto(String productId, int quantity, double price) {}

    /**
     * Convierte un Order de dominio a su representación DTO.
     *
     * @param order el pedido de dominio
     * @return DTO listo para serializar
     */
    public static OrderDto from(Order order) {
        List<ItemDto> itemDtos = order.items().stream()
                .map(i -> new ItemDto(i.productId(), i.quantity(), i.price()))
                .toList();
        return new OrderDto(
                order.id(),
                order.customerId(),
                itemDtos,
                order.status().name(),
                order.total()
        );
    }

    /**
     * Convierte el DTO de vuelta al modelo de dominio.
     *
     * @return Order de dominio reconstruido desde el DTO
     */
    public Order toDomain() {
        List<OrderItem> domainItems = items.stream()
                .map(i -> new OrderItem(i.productId(), i.quantity(), i.price()))
                .toList();
        return new Order(id, customerId, domainItems, OrderStatus.valueOf(status), total);
    }
}
