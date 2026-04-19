package orders;

import orders.adapter.in.http.OrderController;
import orders.adapter.out.persistence.InMemoryOrderRepository;
import orders.application.service.OrderService;

/**
 * Punto de entrada y composición del sistema (wiring manual).
 *
 * <p>Aquí se ensamblan todos los componentes de la arquitectura hexagonal
 * sin ningún framework de inyección de dependencias:
 *
 * <pre>
 *   InMemoryOrderRepository  ←  OrderService  ←  OrderController
 *        (adaptador out)          (aplicación)      (adaptador in)
 * </pre>
 *
 * <p>Para conectar una base de datos real, basta con reemplazar
 * {@code new InMemoryOrderRepository()} por otra implementación de
 * {@code OrderRepository} sin tocar ninguna otra clase.
 */
public class Main {

    public static void main(String[] args) {
        // 1. Adaptador de persistencia (puerto de salida)
        var repository = new InMemoryOrderRepository();

        // 2. Servicio de aplicación (implementa los 3 casos de uso)
        var orderService = new OrderService(repository);

        // 3. Adaptador HTTP (puerto de entrada)
        var controller = new OrderController(orderService, orderService, orderService);

        System.out.println("=== Hexagonal Orders — demostración de wiring ===");
        System.out.println("Componentes ensamblados correctamente.");
        System.out.println("  Repository : " + repository.getClass().getSimpleName());
        System.out.println("  Service    : " + orderService.getClass().getSimpleName());
        System.out.println("  Controller : " + controller.getClass().getSimpleName());
        System.out.println();
        System.out.println("Ejecuta los tests con: mvn test");
    }
}
