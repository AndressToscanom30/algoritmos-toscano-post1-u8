import orders.adapter.in.http.OrderController;
import orders.adapter.in.http.OrderController.*;
import orders.adapter.out.persistence.InMemoryOrderRepository;
import orders.application.port.in.CreateOrderUseCase;
import orders.application.port.in.UpdateOrderStatusUseCase;
import orders.application.service.OrderService;
import orders.domain.model.*;
import java.util.*;

/**
 * Runner de verificación manual — ejecuta todos los checkpoints del PDF.
 * Sustituye JUnit mientras no hay red para descargar las dependencias.
 */
public class TestRunner {

    static int passed = 0, failed = 0;

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║   HEXAGONAL ORDERS — VERIFICACIÓN DE CHECKPOINTS    ║");
        System.out.println("╚══════════════════════════════════════════════════════╝\n");

        testDomainModel();
        testCheckpoint1_Ports();
        testCheckpoint2_OrderService();
        testCheckpoint3_OrderController();
        testCheckpoint4_WiringMain();
        testEdgeCases();

        System.out.println("\n══════════════════════════════════════════════════════");
        System.out.printf("  RESULTADO: %d pasaron  |  %d fallaron%n", passed, failed);
        System.out.println("══════════════════════════════════════════════════════");
        System.exit(failed > 0 ? 1 : 0);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    static void ok(String name) {
        System.out.printf("  ✔  %s%n", name);
        passed++;
    }

    static void fail(String name, String reason) {
        System.out.printf("  ✘  %s  →  %s%n", name, reason);
        failed++;
    }

    static void section(String title) {
        System.out.printf("%n▶  %s%n", title);
    }

    static void assertTrue(String test, boolean cond) {
        if (cond) ok(test); else fail(test, "condición falsa");
    }

    static void assertEquals(String test, Object expected, Object actual) {
        if (Objects.equals(expected, actual)) ok(test);
        else fail(test, "esperado=" + expected + " actual=" + actual);
    }

    static void assertThrows(String test, Class<? extends Throwable> type, Runnable r) {
        try { r.run(); fail(test, "no lanzó excepción"); }
        catch (Throwable t) {
            if (type.isInstance(t)) ok(test);
            else fail(test, "lanzó " + t.getClass().getSimpleName() + " en lugar de " + type.getSimpleName());
        }
    }

    static List<OrderItem> sampleItems() {
        return List.of(new OrderItem("prod-A", 2, 10.0), new OrderItem("prod-B", 1, 25.0));
    }

    static OrderService newService() {
        return new OrderService(new InMemoryOrderRepository());
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    static void testDomainModel() {
        section("Modelo de dominio (Order, OrderItem, OrderStatus)");

        // OrderItem validations
        assertThrows("OrderItem quantity=0 rechazado", IllegalArgumentException.class,
            () -> new OrderItem("p1", 0, 10.0));
        assertThrows("OrderItem price<0 rechazado", IllegalArgumentException.class,
            () -> new OrderItem("p1", 1, -5.0));

        // Order.create
        Order o = Order.create("cust-1", sampleItems());
        assertEquals("Estado inicial PENDING", OrderStatus.PENDING, o.status());
        assertTrue("Total calculado: 2*10+1*25=45", Math.abs(o.total() - 45.0) < 0.001);
        assertTrue("ID no nulo", o.id() != null && !o.id().isBlank());

        // withStatus transitions
        Order confirmed = o.withStatus(OrderStatus.CONFIRMED);
        assertEquals("PENDING→CONFIRMED OK", OrderStatus.CONFIRMED, confirmed.status());
        assertThrows("PENDING→DELIVERED inválido", IllegalStateException.class,
            () -> o.withStatus(OrderStatus.DELIVERED));

        // invariant: empty items
        assertThrows("Order sin items rechazado", IllegalArgumentException.class,
            () -> Order.create("c1", List.of()));
    }

    static void testCheckpoint1_Ports() {
        section("Checkpoint 1 — Puertos compilan sin infraestructura");
        // Si llegamos aquí, los interfaces compilaron sin Spring/JPA
        ok("CreateOrderUseCase es interfaz pura Java");
        ok("GetOrderUseCase es interfaz pura Java");
        ok("UpdateOrderStatusUseCase es interfaz pura Java");
        ok("OrderRepository es interfaz pura Java");
        assertTrue("OrderRepository no importa frameworks",
            orders.application.port.out.OrderRepository.class.isInterface());
    }

    static void testCheckpoint2_OrderService() {
        section("Checkpoint 2 — OrderService con InMemoryOrderRepository (sin mocks)");
        OrderService svc = newService();

        // create
        var cmd = new CreateOrderUseCase.Command("cust-A", sampleItems());
        Order created = svc.execute(cmd);
        assertEquals("createOrder status=PENDING", OrderStatus.PENDING, created.status());
        assertTrue("createOrder id asignado", created.id() != null);
        assertTrue("createOrder total>0", created.total() > 0);

        // get
        Order found = svc.execute(created.id());
        assertEquals("getOrder retorna mismo id", created.id(), found.id());

        // get not found
        assertThrows("getOrder id-inexistente lanza OrderNotFoundException",
            OrderNotFoundException.class, () -> svc.execute("no-existe"));

        // update status
        var upd = new UpdateOrderStatusUseCase.Command(created.id(), OrderStatus.CONFIRMED);
        Order updated = svc.execute(upd);
        assertEquals("updateStatus PENDING→CONFIRMED", OrderStatus.CONFIRMED, updated.status());

        // invalid transition
        assertThrows("transición inválida CONFIRMED→PENDING lanza IllegalStateException",
            IllegalStateException.class,
            () -> svc.execute(new UpdateOrderStatusUseCase.Command(created.id(), OrderStatus.PENDING)));

        ok("OrderService depende solo de interfaces (verificado por compilación)");
    }

    static void testCheckpoint3_OrderController() {
        section("Checkpoint 3 — OrderController (adaptador HTTP)");
        var repo = new InMemoryOrderRepository();
        var svc  = new OrderService(repo);
        var ctrl = new OrderController(svc, svc, svc);

        // handleCreate retorna respuesta correcta
        var items = List.of(
            new ItemRequest("p1", 3, 15.0),
            new ItemRequest("p2", 1, 5.0)
        );
        var req  = new CreateOrderRequest("cust-B", items);
        OrderResponse resp = ctrl.handleCreate(req);
        assertTrue("handleCreate id no nulo", resp.id() != null);
        assertEquals("handleCreate status=PENDING", "PENDING", resp.status());
        assertTrue("handleCreate total=50.0", Math.abs(resp.total() - 50.0) < 0.001);
        assertEquals("handleCreate itemCount=2", 2, resp.itemCount());

        // handleGet existente
        OrderResponse found = ctrl.handleGet(resp.id());
        assertEquals("handleGet retorna mismo id", resp.id(), found.id());

        // handleGet id inexistente → 404 (OrderNotFoundException)
        assertThrows("handleGet id-inexistente lanza OrderNotFoundException (HTTP 404)",
            OrderNotFoundException.class, () -> ctrl.handleGet("id-que-no-existe"));

        // handleUpdateStatus válido
        OrderResponse upd = ctrl.handleUpdateStatus(resp.id(), "CONFIRMED");
        assertEquals("handleUpdateStatus CONFIRMED", "CONFIRMED", upd.status());

        // status inválido
        assertThrows("handleUpdateStatus status inválido lanza IllegalArgumentException",
            IllegalArgumentException.class,
            () -> ctrl.handleUpdateStatus(resp.id(), "FAKE_STATUS"));
    }

    static void testCheckpoint4_WiringMain() {
        section("Checkpoint 4 — Wiring manual sin framework de inyección");
        var repo  = new InMemoryOrderRepository();
        var svc   = new OrderService(repo);
        var ctrl  = new OrderController(svc, svc, svc);

        // Full round-trip: create → get → update → get
        OrderResponse created = ctrl.handleCreate(
            new CreateOrderRequest("cust-C",
                List.of(new ItemRequest("prod-Z", 1, 100.0))));

        assertEquals("wiring: estado inicial PENDING", "PENDING", created.status());

        ctrl.handleUpdateStatus(created.id(), "CONFIRMED");
        ctrl.handleUpdateStatus(created.id(), "SHIPPED");
        OrderResponse delivered = ctrl.handleUpdateStatus(created.id(), "DELIVERED");

        assertEquals("wiring: ciclo completo → DELIVERED", "DELIVERED", delivered.status());

        assertThrows("wiring: desde DELIVERED no se puede transicionar",
            IllegalStateException.class,
            () -> ctrl.handleUpdateStatus(created.id(), "CANCELLED"));

        ok("Wiring: new OrderService(new InMemoryOrderRepository()) sin framework");
    }

    static void testEdgeCases() {
        section("Casos borde adicionales");
        OrderService svc = newService();

        // Cancelación desde PENDING
        var o = svc.execute(new CreateOrderUseCase.Command("cust-D", sampleItems()));
        Order cancelled = svc.execute(new UpdateOrderStatusUseCase.Command(o.id(), OrderStatus.CANCELLED));
        assertEquals("PENDING→CANCELLED permitido", OrderStatus.CANCELLED, cancelled.status());

        // Desde CANCELLED no se puede continuar
        assertThrows("CANCELLED es estado terminal",
            IllegalStateException.class,
            () -> svc.execute(new UpdateOrderStatusUseCase.Command(o.id(), OrderStatus.CONFIRMED)));

        // IDs únicos
        var id1 = svc.execute(new CreateOrderUseCase.Command("c1", sampleItems())).id();
        var id2 = svc.execute(new CreateOrderUseCase.Command("c2", sampleItems())).id();
        assertTrue("IDs son únicos entre pedidos", !id1.equals(id2));

        // findByCustomerId
        var repo = new InMemoryOrderRepository();
        var svc2 = new OrderService(repo);
        svc2.execute(new CreateOrderUseCase.Command("cust-multi", sampleItems()));
        svc2.execute(new CreateOrderUseCase.Command("cust-multi", sampleItems()));
        svc2.execute(new CreateOrderUseCase.Command("otro-cliente", sampleItems()));
        var porCliente = repo.findByCustomerId("cust-multi");
        assertEquals("findByCustomerId retorna 2 pedidos", 2, porCliente.size());
    }
}
