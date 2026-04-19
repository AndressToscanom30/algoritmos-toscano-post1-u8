package orders.domain.model;

/**
 * Estados posibles de un pedido.
 * Las transiciones válidas son:
 * PENDING → CONFIRMED → SHIPPED → DELIVERED
 * PENDING → CANCELLED
 * CONFIRMED → CANCELLED
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    /**
     * Verifica si la transición al nuevo estado es válida.
     *
     * @param next el estado al que se desea transicionar
     * @return true si la transición es permitida
     */
    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING    -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED  -> next == SHIPPED   || next == CANCELLED;
            case SHIPPED    -> next == DELIVERED;
            case DELIVERED  -> false;
            case CANCELLED  -> false;
        };
    }
}
