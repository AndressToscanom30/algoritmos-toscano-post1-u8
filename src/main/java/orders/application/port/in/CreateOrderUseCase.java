package orders.application.port.in;

import orders.domain.model.Order;
import orders.domain.model.OrderItem;

import java.util.List;

/**
 * Puerto de entrada: caso de uso para crear un pedido.
 *
 * <p>Precondición: {@code command.customerId()} no nulo ni vacío,
 * {@code command.items()} no vacío.
 * <p>Postcondición: pedido creado con estado PENDING; se retorna el pedido con ID asignado.
 */
public interface CreateOrderUseCase {

    /**
     * Comando de creación con los datos necesarios para crear un pedido.
     *
     * @param customerId identificador del cliente (no nulo)
     * @param items      lista de ítems (al menos uno)
     */
    record Command(String customerId, List<OrderItem> items) {}

    /**
     * Ejecuta la creación del pedido.
     *
     * @param command datos del pedido a crear
     * @return el pedido recién creado con su ID asignado
     * @throws IllegalArgumentException si el comando viola las precondiciones
     */
    Order execute(Command command);
}
