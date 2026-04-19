package orders.application.service;

import orders.application.port.in.CreateOrderUseCase;
import orders.application.port.in.GetOrderUseCase;
import orders.application.port.in.UpdateOrderStatusUseCase;
import orders.application.port.out.OrderRepository;
import orders.domain.model.Order;
import orders.domain.model.OrderNotFoundException;

import java.util.Objects;

/**
 * Implementación de los tres casos de uso del dominio de pedidos.
 *
 * <p>Depende exclusivamente de interfaces (puertos); nunca de implementaciones
 * concretas de infraestructura. La inyección de dependencias se realiza
 * manualmente mediante el constructor, sin frameworks de IoC.
 */
public class OrderService implements
        CreateOrderUseCase,
        GetOrderUseCase,
        UpdateOrderStatusUseCase {

    private final OrderRepository repository;

    /**
     * Constructor con inyección manual del repositorio.
     *
     * @param repository implementación del puerto de salida (no nulo)
     * @throws NullPointerException si repository es nulo
     */
    public OrderService(OrderRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Precondición: {@code cmd.customerId()} no nulo, {@code cmd.items()} no vacío.
     * <p>Postcondición: pedido creado con estado PENDING y persistido.
     */
    @Override
    public Order execute(CreateOrderUseCase.Command cmd) {
        Objects.requireNonNull(cmd, "command must not be null");
        Order order = Order.create(cmd.customerId(), cmd.items());
        return repository.save(order);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Precondición: {@code orderId} no nulo.
     * <p>Postcondición: retorna el pedido o lanza {@link OrderNotFoundException}.
     */
    @Override
    public Order execute(String orderId) {
        Objects.requireNonNull(orderId, "orderId must not be null");
        return repository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Precondición: el pedido debe existir; la transición de estado debe ser válida.
     * <p>Postcondición: pedido actualizado y persistido con el nuevo estado.
     */
    @Override
    public Order execute(UpdateOrderStatusUseCase.Command cmd) {
        Objects.requireNonNull(cmd, "command must not be null");
        Order order = repository.findById(cmd.orderId())
                .orElseThrow(() -> new OrderNotFoundException(cmd.orderId()));
        Order updated = order.withStatus(cmd.newStatus());
        return repository.save(updated);
    }
}
