package orders.adapter.out.persistence;

import orders.application.port.out.OrderRepository;
import orders.domain.model.Order;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador de persistencia: implementación en memoria del puerto {@link OrderRepository}.
 *
 * <p>Útil para pruebas unitarias y desarrollo local sin necesidad de base de datos.
 * Utiliza {@link ConcurrentHashMap} para ser seguro en entornos concurrentes.
 *
 * <p>Implementa el puerto de salida definido en la capa de aplicación, cumpliendo
 * el principio de inversión de dependencias de la arquitectura hexagonal.
 */
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, Order> store = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     *
     * <p>Postcondición: el pedido queda almacenado bajo su {@code id}.
     * Si ya existía un pedido con ese ID, se reemplaza (semántica de upsert).
     */
    @Override
    public Order save(Order order) {
        store.put(order.id(), order);
        return order;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Order> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Order> findByCustomerId(String customerId) {
        return store.values().stream()
                .filter(o -> o.customerId().equals(customerId))
                .toList();
    }

    /** Retorna el número de pedidos almacenados (útil para assertions en tests). */
    public int size() {
        return store.size();
    }

    /** Limpia todos los pedidos (útil para resetear estado entre tests). */
    public void clear() {
        store.clear();
    }
}
