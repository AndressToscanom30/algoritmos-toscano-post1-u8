package orders.application.port.in;

import orders.domain.model.Order;
import orders.domain.model.OrderNotFoundException;

/**
 * Puerto de entrada: caso de uso para obtener un pedido por su ID.
 *
 * <p>Precondición: {@code orderId} no nulo.
 * <p>Postcondición: retorna el pedido existente o lanza {@link OrderNotFoundException}.
 */
public interface GetOrderUseCase {

    /**
     * Busca y retorna el pedido con el ID indicado.
     *
     * @param orderId identificador del pedido
     * @return el pedido encontrado
     * @throws OrderNotFoundException   si no existe ningún pedido con ese ID
     * @throws IllegalArgumentException si orderId es nulo
     */
    Order execute(String orderId);
}
