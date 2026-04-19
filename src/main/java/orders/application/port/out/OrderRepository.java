package orders.application.port.out;

import orders.domain.model.Order;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida: abstracción de la persistencia de pedidos.
 *
 * <p>El dominio define esta interfaz; la infraestructura la implementa.
 * Esto garantiza que el núcleo de la aplicación nunca dependa de tecnologías
 * de base de datos ni de ningún otro mecanismo de almacenamiento.
 */
public interface OrderRepository {

    /**
     * Persiste un pedido (inserción o actualización).
     *
     * <p>Precondición: {@code order} no nulo.
     * <p>Postcondición: el pedido queda almacenado y se retorna la instancia guardada.
     *
     * @param order el pedido a guardar
     * @return el pedido guardado
     */
    Order save(Order order);

    /**
     * Busca un pedido por su identificador único.
     *
     * <p>Postcondición: retorna un {@link Optional} con el pedido si existe, vacío si no.
     *
     * @param id identificador del pedido
     * @return Optional con el pedido, o vacío si no se encontró
     */
    Optional<Order> findById(String id);

    /**
     * Retorna todos los pedidos de un cliente.
     *
     * <p>Postcondición: lista nunca nula; puede estar vacía.
     *
     * @param customerId identificador del cliente
     * @return lista de pedidos del cliente
     */
    List<Order> findByCustomerId(String customerId);
}
