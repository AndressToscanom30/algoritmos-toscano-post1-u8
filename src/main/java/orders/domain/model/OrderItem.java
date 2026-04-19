package orders.domain.model;

/**
 * Value object que representa un ítem dentro de un pedido.
 * Invariante: quantity > 0, price >= 0.
 *
 * @param productId identificador del producto (no nulo)
 * @param quantity  cantidad solicitada (debe ser > 0)
 * @param price     precio unitario (debe ser >= 0)
 */
public record OrderItem(String productId, int quantity, double price) {

    /**
     * Constructor compacto con validaciones de contrato.
     *
     * @throws IllegalArgumentException si productId es nulo, quantity <= 0 o price < 0
     */
    public OrderItem {
        if (productId == null || productId.isBlank())
            throw new IllegalArgumentException("productId must not be null or blank");
        if (quantity <= 0)
            throw new IllegalArgumentException("quantity must be > 0, got: " + quantity);
        if (price < 0)
            throw new IllegalArgumentException("price must be >= 0, got: " + price);
    }
}
