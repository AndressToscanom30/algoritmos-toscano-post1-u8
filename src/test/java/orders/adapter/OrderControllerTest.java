package orders.adapter;

import orders.adapter.in.http.OrderController;
import orders.adapter.in.http.OrderController.CreateOrderRequest;
import orders.adapter.in.http.OrderController.ItemRequest;
import orders.adapter.in.http.OrderController.OrderResponse;
import orders.adapter.out.persistence.InMemoryOrderRepository;
import orders.application.service.OrderService;
import orders.domain.model.OrderNotFoundException;
import orders.domain.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests del adaptador HTTP (controlador ligero).
 *
 * <p>Checkpoint 3a: {@code handleCreate} retorna la respuesta correcta.
 * <p>Checkpoint 3b: {@code handleGet} retorna excepción para ID inexistente.
 *
 * <p>Sin frameworks de mocking ni servidores HTTP; se prueba el controlador
 * directamente como objeto Java.
 */
@DisplayName("OrderController — adaptador HTTP")
class OrderControllerTest {

    private OrderController controller;

    private static final List<ItemRequest> SAMPLE_ITEMS = List.of(
            new ItemRequest("prod-X", 3, 15.0),
            new ItemRequest("prod-Y", 1, 5.0)
    );

    @BeforeEach
    void setUp() {
        var repository = new InMemoryOrderRepository();
        var service    = new OrderService(repository);
        controller     = new OrderController(service, service, service);
    }

    // =========================================================================
    // Checkpoint 3a: handleCreate
    // =========================================================================

    @Nested
    @DisplayName("handleCreate — POST /orders")
    class HandleCreateTests {

        @Test
        @DisplayName("Retorna respuesta con ID, customerId, status PENDING y total correcto")
        void handleCreate_returnsCorrectResponse() {
            var req    = new CreateOrderRequest("cust-001", SAMPLE_ITEMS);
            OrderResponse resp = controller.handleCreate(req);

            assertNotNull(resp.id(),               "ID no debe ser nulo");
            assertEquals("cust-001", resp.customerId());
            assertEquals("PENDING", resp.status(), "Estado inicial debe ser PENDING");
            assertEquals(50.0, resp.total(), 0.001, "Total: 3*15 + 1*5 = 50");
            assertEquals(2, resp.itemCount());
        }

        @Test
        @DisplayName("Cada llamada genera un ID único")
        void handleCreate_generatesUniqueIds() {
            var req1 = new CreateOrderRequest("cust-A", SAMPLE_ITEMS);
            var req2 = new CreateOrderRequest("cust-B", SAMPLE_ITEMS);

            assertNotEquals(controller.handleCreate(req1).id(),
                            controller.handleCreate(req2).id());
        }

        @Test
        @DisplayName("Caso borde: ítems vacíos lanza excepción")
        void handleCreate_emptyItems_throwsException() {
            var req = new CreateOrderRequest("cust-002", List.of());
            assertThrows(IllegalArgumentException.class, () -> controller.handleCreate(req));
        }

        @Test
        @DisplayName("Caso borde: customerId nulo lanza excepción")
        void handleCreate_nullCustomerId_throwsException() {
            var req = new CreateOrderRequest(null, SAMPLE_ITEMS);
            assertThrows(IllegalArgumentException.class, () -> controller.handleCreate(req));
        }
    }

    // =========================================================================
    // Checkpoint 3b: handleGet
    // =========================================================================

    @Nested
    @DisplayName("handleGet — GET /orders/{id}")
    class HandleGetTests {

        @Test
        @DisplayName("Retorna el pedido existente por ID")
        void handleGet_existingId_returnsOrder() {
            var created = controller.handleCreate(
                    new CreateOrderRequest("cust-003", SAMPLE_ITEMS));

            OrderResponse found = controller.handleGet(created.id());

            assertEquals(created.id(), found.id());
            assertEquals("cust-003", found.customerId());
        }

        @Test
        @DisplayName("Checkpoint 3b: ID inexistente lanza OrderNotFoundException (→ HTTP 404)")
        void handleGet_nonExistentId_throwsOrderNotFoundException() {
            assertThrows(OrderNotFoundException.class,
                    () -> controller.handleGet("id-inexistente"),
                    "Debe lanzar OrderNotFoundException para ID inexistente");
        }
    }

    // =========================================================================
    // handleUpdateStatus
    // =========================================================================

    @Nested
    @DisplayName("handleUpdateStatus — PATCH /orders/{id}/status")
    class HandleUpdateStatusTests {

        @Test
        @DisplayName("Transición válida PENDING → CONFIRMED actualiza estado")
        void handleUpdateStatus_validTransition_updatesStatus() {
            var created = controller.handleCreate(
                    new CreateOrderRequest("cust-004", SAMPLE_ITEMS));

            OrderResponse updated = controller.handleUpdateStatus(
                    created.id(), "CONFIRMED");

            assertEquals("CONFIRMED", updated.status());
        }

        @Test
        @DisplayName("Caso borde: status inválido lanza IllegalArgumentException")
        void handleUpdateStatus_invalidStatus_throwsIllegalArgumentException() {
            var created = controller.handleCreate(
                    new CreateOrderRequest("cust-005", SAMPLE_ITEMS));

            assertThrows(IllegalArgumentException.class,
                    () -> controller.handleUpdateStatus(created.id(), "INVALID_STATE"));
        }

        @Test
        @DisplayName("Caso borde: transición inválida lanza IllegalStateException")
        void handleUpdateStatus_invalidTransition_throwsIllegalStateException() {
            var created = controller.handleCreate(
                    new CreateOrderRequest("cust-006", SAMPLE_ITEMS));

            // PENDING → DELIVERED no es una transición válida
            assertThrows(IllegalStateException.class,
                    () -> controller.handleUpdateStatus(created.id(), "DELIVERED"));
        }

        @Test
        @DisplayName("Caso borde: ID inexistente lanza OrderNotFoundException")
        void handleUpdateStatus_nonExistentId_throwsOrderNotFoundException() {
            assertThrows(OrderNotFoundException.class,
                    () -> controller.handleUpdateStatus("no-existe", "CONFIRMED"));
        }
    }
}
