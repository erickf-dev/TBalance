package sistemainventario.service;

import sistemainventario.exception.ValidationException;
import sistemainventario.model.StockEntry;
import sistemainventario.model.InventoryMovement;
import sistemainventario.model.Product;
import sistemainventario.model.StockExit;
import sistemainventario.repository.Repository;
import sistemainventario.util.DateUtil;
import sistemainventario.util.IdGenerator;

import java.util.List;
import java.util.Objects;

/**
 * Registro de entradas y salidas de inventario.
 *
 * <p><b>Bajo acoplamiento:</b> en P2 esta clase creaba sus propios
 * repositorios con {@code new StockEntryRepository(dir)}, duplicando las cachés
 * que ya existían en {@code AppContext}. Ahora los recibe por constructor
 * (inyección de dependencias) como interfaces {@link Repository}.</p>
 *
 * <p><b>Polimorfismo:</b> {@link #registrar} trabaja con
 * {@link InventoryMovement}; cada subclase decide si suma o resta stock.</p>
 */
public class MovementService {

    private final InventoryService inventario;
    private final Repository<StockEntry> entradas;
    private final Repository<StockExit> salidas;

    public MovementService(InventoryService inventario,
                             Repository<StockEntry> entradas,
                             Repository<StockExit> salidas) {
        this.inventario = Objects.requireNonNull(inventario);
        this.entradas = Objects.requireNonNull(entradas);
        this.salidas = Objects.requireNonNull(salidas);
    }

    public List<StockEntry> listarEntradas() {
        return entradas.listarTodos();
    }

    public List<StockExit> listarSalidas() {
        return salidas.listarTodos();
    }

    /**
     * @param entrada datos de la entrada
     * @return la entrada registrada
     * @throws ValidationException si el producto no existe o la cantidad no es válida
     */
    public StockEntry registrarEntrada(StockEntry entrada) {
        return registrar(entrada, entradas);
    }

    /**
     * @param salida datos de la salida
     * @return la salida registrada
     * @throws ValidationException si el stock es insuficiente o los datos no son válidos
     */
    public StockExit registrarSalida(StockExit salida) {
        return registrar(salida, salidas);
    }

    /**
     * Algoritmo común para cualquier movimiento: validar, aplicar al
     * producto, persistir el producto y guardar el movimiento.
     */
    private <M extends InventoryMovement> M registrar(M movimiento, Repository<M> repositorio) {
        if (movimiento == null) {
            throw new ValidationException("No se recibieron datos del movimiento");
        }
        movimiento.validar();
        Product producto = inventario.obtener(movimiento.getProductoId());
        movimiento.aplicarA(producto);
        inventario.registrarCambioDeStock(producto);
        movimiento.registrar(IdGenerator.conPrefijo(movimiento.getPrefijoId()), DateUtil.hoy());
        repositorio.agregar(movimiento);
        return movimiento;
    }
}
