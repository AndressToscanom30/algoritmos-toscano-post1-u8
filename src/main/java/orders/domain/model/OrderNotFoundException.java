package orders.domain.model;

/**
 * Excepción lanzada cuando no se encuentra un pedido con el ID solicitado.
 * Pertenece al dominio; no depende de ningún framework.
 */
public class OrderNotFoundException extends RuntimeException {

    private final String orderId;

    /**
     * @param orderId el ID del pedido que no fue encontrado
     */
    public OrderNotFoundException(String orderId) {
        super("Order not found with id: " + orderId);
        this.orderId = orderId;
    }

    /** @return el ID del pedido que causó la excepción */
    public String getOrderId() {
        return orderId;
    }
}
