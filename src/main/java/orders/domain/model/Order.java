package orders.domain.model;

import java.util.List;
import java.util.UUID;

/**
 * Entidad principal del dominio: representa un pedido de cliente.
 * Sin dependencias de frameworks externos; contiene lógica de negocio pura.
 *
 * <p>Invariantes:
 * <ul>
 *   <li>{@code id} no nulo</li>
 *   <li>{@code customerId} no nulo ni vacío</li>
 *   <li>{@code items} no nulo ni vacío</li>
 *   <li>{@code total} >= 0</li>
 * </ul>
 *
 * @param id         identificador único del pedido (UUID)
 * @param customerId identificador del cliente que realizó el pedido
 * @param items      lista inmutable de ítems del pedido
 * @param status     estado actual del pedido
 * @param total      precio total calculado como suma de (price * quantity) por ítem
 */
public record Order(
        String id,
        String customerId,
        List<OrderItem> items,
        OrderStatus status,
        double total
) {

    /**
     * Constructor compacto con validaciones de invariantes.
     *
     * @throws IllegalArgumentException si algún campo viola las invariantes
     */
    public Order {
        if (id == null || id.isBlank())
            throw new IllegalArgumentException("id must not be null or blank");
        if (customerId == null || customerId.isBlank())
            throw new IllegalArgumentException("customerId must not be null or blank");
        if (items == null || items.isEmpty())
            throw new IllegalArgumentException("Order must have at least one item");
        if (total < 0)
            throw new IllegalArgumentException("total must be >= 0, got: " + total);
    }

    /**
     * Crea un nuevo pedido en estado PENDING calculando el total automáticamente.
     *
     * <p>Precondición: {@code customerId} no nulo, {@code items} no vacío.
     * <p>Postcondición: pedido creado con estado PENDING, ID generado como UUID, total calculado.
     *
     * @param customerId identificador del cliente
     * @param items      lista de ítems (al menos uno)
     * @return nuevo pedido en estado PENDING con ID único
     * @throws IllegalArgumentException si customerId es nulo o items está vacío
     */
    public static Order create(String customerId, List<OrderItem> items) {
        if (customerId == null || customerId.isBlank())
            throw new IllegalArgumentException("customerId must not be null or blank");
        if (items == null || items.isEmpty())
            throw new IllegalArgumentException("Order must have at least one item");

        double total = items.stream()
                .mapToDouble(i -> i.price() * i.quantity())
                .sum();

        return new Order(
                UUID.randomUUID().toString(),
                customerId,
                List.copyOf(items),
                OrderStatus.PENDING,
                total
        );
    }

    /**
     * Retorna una copia del pedido con el nuevo estado aplicado.
     *
     * <p>Precondición: la transición desde el estado actual al {@code newStatus} debe ser válida.
     * <p>Postcondición: retorna un nuevo Order con el estado actualizado; el original no se modifica.
     *
     * @param newStatus el estado al que se desea transicionar
     * @return nuevo Order con el estado actualizado
     * @throws IllegalStateException si la transición no es válida
     */
    public Order withStatus(OrderStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Invalid transition: " + this.status + " → " + newStatus);
        }
        return new Order(id, customerId, items, newStatus, total);
    }
}
