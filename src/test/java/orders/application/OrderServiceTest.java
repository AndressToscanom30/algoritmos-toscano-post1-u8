package orders.application;

import orders.adapter.out.persistence.InMemoryOrderRepository;
import orders.application.port.in.CreateOrderUseCase;
import orders.application.port.in.UpdateOrderStatusUseCase;
import orders.application.service.OrderService;
import orders.domain.model.Order;
import orders.domain.model.OrderItem;
import orders.domain.model.OrderNotFoundException;
import orders.domain.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests del servicio de aplicación.
 *
 * <p>Checkpoint 2: verifica los tres casos de uso usando {@link InMemoryOrderRepository}
 * sin ningún mock framework.
 *
 * <p>No depende de ninguna tecnología de infraestructura.
 */
@DisplayName("OrderService — casos de uso")
class OrderServiceTest {

    private InMemoryOrderRepository repository;
    private OrderService service;

    /** Ítems de prueba reutilizables */
    private static final List<OrderItem> SAMPLE_ITEMS = List.of(
            new OrderItem("prod-A", 2, 10.0),
            new OrderItem("prod-B", 1, 25.0)
    );

    @BeforeEach
    void setUp() {
        repository = new InMemoryOrderRepository();
        service    = new OrderService(repository);
    }

    // =========================================================================
    // Checkpoint 2a: CreateOrderUseCase
    // =========================================================================

    @Nested
    @DisplayName("CreateOrderUseCase")
    class CreateOrderTests {

        @Test
        @DisplayName("Crea pedido con estado PENDING y total calculado correctamente")
        void createOrder_happyPath() {
            var cmd = new CreateOrderUseCase.Command("customer-1", SAMPLE_ITEMS);
            Order result = service.execute(cmd);

            assertNotNull(result.id(),                   "El ID no debe ser nulo");
            assertEquals("customer-1", result.customerId());
            assertEquals(OrderStatus.PENDING, result.status());
            assertEquals(45.0, result.total(), 0.001,   "Total: 2*10 + 1*25 = 45");
            assertEquals(2, result.items().size());
        }

        @Test
        @DisplayName("El ID generado es único en cada creación")
        void createOrder_generatesUniqueIds() {
            var cmd1 = new CreateOrderUseCase.Command("customer-1", SAMPLE_ITEMS);
            var cmd2 = new CreateOrderUseCase.Command("customer-1", SAMPLE_ITEMS);

            String id1 = service.execute(cmd1).id();
            String id2 = service.execute(cmd2).id();

            assertNotEquals(id1, id2, "Cada pedido debe tener un ID único");
        }

        @Test
        @DisplayName("Caso borde: pedido sin ítems lanza IllegalArgumentException")
        void createOrder_emptyItems_throwsException() {
            var cmd = new CreateOrderUseCase.Command("customer-1", List.of());
            assertThrows(IllegalArgumentException.class, () -> service.execute(cmd),
                    "Debe rechazar pedido sin ítems");
        }

        @Test
        @DisplayName("Caso borde: customerId nulo lanza IllegalArgumentException")
        void createOrder_nullCustomerId_throwsException() {
            var cmd = new CreateOrderUseCase.Command(null, SAMPLE_ITEMS);
            assertThrows(IllegalArgumentException.class, () -> service.execute(cmd),
                    "Debe rechazar customerId nulo");
        }

        @Test
        @DisplayName("Caso borde: items nulo lanza IllegalArgumentException")
        void createOrder_nullItems_throwsException() {
            var cmd = new CreateOrderUseCase.Command("customer-1", null);
            assertThrows(IllegalArgumentException.class, () -> service.execute(cmd),
                    "Debe rechazar items nulos");
        }

        @Test
        @DisplayName("El pedido se persiste en el repositorio")
        void createOrder_persistsInRepository() {
            var cmd = new CreateOrderUseCase.Command("customer-1", SAMPLE_ITEMS);
            Order created = service.execute(cmd);

            assertEquals(1, repository.size());
            assertTrue(repository.findById(created.id()).isPresent());
        }
    }

    // =========================================================================
    // Checkpoint 2b: GetOrderUseCase
    // =========================================================================

    @Nested
    @DisplayName("GetOrderUseCase")
    class GetOrderTests {

        @Test
        @DisplayName("Retorna el pedido existente por ID")
        void getOrder_existing_returnsOrder() {
            var cmd  = new CreateOrderUseCase.Command("customer-2", SAMPLE_ITEMS);
            Order created = service.execute(cmd);

            Order found = service.execute(created.id());

            assertEquals(created.id(), found.id());
            assertEquals("customer-2", found.customerId());
        }

        @Test
        @DisplayName("Caso borde: ID inexistente lanza OrderNotFoundException")
        void getOrder_nonExistentId_throwsOrderNotFoundException() {
            assertThrows(OrderNotFoundException.class,
                    () -> service.execute("id-que-no-existe"),
                    "Debe lanzar OrderNotFoundException para ID inexistente");
        }

        @Test
        @DisplayName("El mensaje de la excepción contiene el ID buscado")
        void getOrder_notFound_exceptionContainsId() {
            String missingId = "missing-999";
            OrderNotFoundException ex = assertThrows(OrderNotFoundException.class,
                    () -> service.execute(missingId));
            assertTrue(ex.getMessage().contains(missingId));
        }
    }

    // =========================================================================
    // Checkpoint 2c: UpdateOrderStatusUseCase
    // =========================================================================

    @Nested
    @DisplayName("UpdateOrderStatusUseCase")
    class UpdateStatusTests {

        @Test
        @DisplayName("Transición válida PENDING → CONFIRMED")
        void updateStatus_pendingToConfirmed_succeeds() {
            Order order = createSampleOrder("customer-3");
            var cmd = new UpdateOrderStatusUseCase.Command(order.id(), OrderStatus.CONFIRMED);

            Order updated = service.execute(cmd);

            assertEquals(OrderStatus.CONFIRMED, updated.status());
            assertEquals(order.id(), updated.id());
        }

        @Test
        @DisplayName("Transición válida CONFIRMED → SHIPPED")
        void updateStatus_confirmedToShipped_succeeds() {
            Order order = createSampleOrder("customer-4");
            service.execute(new UpdateOrderStatusUseCase.Command(order.id(), OrderStatus.CONFIRMED));
            Order updated = service.execute(
                    new UpdateOrderStatusUseCase.Command(order.id(), OrderStatus.SHIPPED));

            assertEquals(OrderStatus.SHIPPED, updated.status());
        }

        @Test
        @DisplayName("Transición válida SHIPPED → DELIVERED")
        void updateStatus_shippedToDelivered_succeeds() {
            Order order = createSampleOrder("customer-5");
            service.execute(new UpdateOrderStatusUseCase.Command(order.id(), OrderStatus.CONFIRMED));
            service.execute(new UpdateOrderStatusUseCase.Command(order.id(), OrderStatus.SHIPPED));
            Order updated = service.execute(
                    new UpdateOrderStatusUseCase.Command(order.id(), OrderStatus.DELIVERED));

            assertEquals(OrderStatus.DELIVERED, updated.status());
        }

        @Test
        @DisplayName("Cancelación válida PENDING → CANCELLED")
        void updateStatus_pendingToCancelled_succeeds() {
            Order order = createSampleOrder("customer-6");
            Order updated = service.execute(
                    new UpdateOrderStatusUseCase.Command(order.id(), OrderStatus.CANCELLED));

            assertEquals(OrderStatus.CANCELLED, updated.status());
        }

        @Test
        @DisplayName("Caso borde: transición inválida PENDING → DELIVERED lanza IllegalStateException")
        void updateStatus_invalidTransition_throwsIllegalStateException() {
            Order order = createSampleOrder("customer-7");
            var cmd = new UpdateOrderStatusUseCase.Command(order.id(), OrderStatus.DELIVERED);

            assertThrows(IllegalStateException.class, () -> service.execute(cmd),
                    "Debe rechazar transición inválida PENDING → DELIVERED");
        }

        @Test
        @DisplayName("Caso borde: transición desde DELIVERED (estado terminal) lanza excepción")
        void updateStatus_fromDelivered_throwsIllegalStateException() {
            Order order = createSampleOrder("customer-8");
            service.execute(new UpdateOrderStatusUseCase.Command(order.id(), OrderStatus.CONFIRMED));
            service.execute(new UpdateOrderStatusUseCase.Command(order.id(), OrderStatus.SHIPPED));
            service.execute(new UpdateOrderStatusUseCase.Command(order.id(), OrderStatus.DELIVERED));

            assertThrows(IllegalStateException.class,
                    () -> service.execute(new UpdateOrderStatusUseCase.Command(
                            order.id(), OrderStatus.CANCELLED)));
        }

        @Test
        @DisplayName("Caso borde: ID inexistente al actualizar lanza OrderNotFoundException")
        void updateStatus_nonExistentId_throwsOrderNotFoundException() {
            var cmd = new UpdateOrderStatusUseCase.Command("no-existe", OrderStatus.CONFIRMED);
            assertThrows(OrderNotFoundException.class, () -> service.execute(cmd));
        }

        @Test
        @DisplayName("La actualización se persiste en el repositorio")
        void updateStatus_persistsUpdatedOrder() {
            Order order = createSampleOrder("customer-9");
            service.execute(new UpdateOrderStatusUseCase.Command(order.id(), OrderStatus.CONFIRMED));

            Order stored = repository.findById(order.id()).orElseThrow();
            assertEquals(OrderStatus.CONFIRMED, stored.status());
        }
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private Order createSampleOrder(String customerId) {
        return service.execute(new CreateOrderUseCase.Command(customerId, SAMPLE_ITEMS));
    }
}
