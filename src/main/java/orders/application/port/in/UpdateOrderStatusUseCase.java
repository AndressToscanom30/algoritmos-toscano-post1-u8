package orders.application.port.in;

import orders.domain.model.Order;
import orders.domain.model.OrderNotFoundException;
import orders.domain.model.OrderStatus;

/**
 * Puerto de entrada: caso de uso para actualizar el estado de un pedido.
 *
 * <p>Precondición: el pedido con {@code command.orderId()} debe existir,
 * y la transición desde el estado actual a {@code command.newStatus()} debe ser válida.
 * <p>Postcondición: el pedido queda con el nuevo estado y se retorna la versión actualizada.
 */
public interface UpdateOrderStatusUseCase {

    /**
     * Comando que encapsula el ID del pedido y el nuevo estado deseado.
     *
     * @param orderId   ID del pedido a actualizar (no nulo)
     * @param newStatus nuevo estado al que se quiere transicionar
     */
    record Command(String orderId, OrderStatus newStatus) {}

    /**
     * Ejecuta la actualización del estado.
     *
     * @param command datos de la actualización
     * @return el pedido con el estado actualizado
     * @throws OrderNotFoundException si no existe el pedido con el ID indicado
     * @throws IllegalStateException  si la transición de estado no es válida
     */
    Order execute(Command command);
}
