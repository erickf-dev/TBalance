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

    /** @return entradas vigentes (sin las anuladas) */
    public List<StockEntry> listarEntradas() {
        return entradas.filtrar(e -> !e.isAnulada());
    }

    /** @return salidas vigentes (sin las anuladas) */
    public List<StockExit> listarSalidas() {
        return salidas.filtrar(s -> !s.isAnulada());
    }

    /** @return entradas anuladas */
    public List<StockEntry> listarEntradasAnuladas() {
        return entradas.filtrar(StockEntry::isAnulada);
    }

    /** @return salidas anuladas */
    public List<StockExit> listarSalidasAnuladas() {
        return salidas.filtrar(StockExit::isAnulada);
    }

    /**
     * Anula una entrada y quita del stock las unidades que había sumado.
     *
     * @param id     ID de la entrada
     * @param motivo por qué se anula
     * @return la entrada anulada
     * @throws ValidationException si ya se vendieron o sacaron esas unidades (el stock quedaría negativo)
     */
    public StockEntry anularEntrada(String id, String motivo) {
        return anularEntrada(id, motivo, null);
    }

    /**
     * Igual que la anulación sin nombre, dejando anotado quién anuló.
     *
     * @param id     ID
     * @param motivo por qué se anula
     * @param quien  nombre de quien anula (puede ser {@code null})
     * @return el movimiento anulado
     */
    public StockEntry anularEntrada(String id, String motivo, String quien) {
        StockEntry entrada = entradas.buscarPorId(id).orElseThrow(() -> new ValidationException("Entrada no encontrada"));
        Product producto = inventario.obtener(entrada.getProductoId());
        if (!entrada.isAnulada() && producto.getStock() < entrada.getCantidad()) {
            throw new ValidationException("No se puede anular: ya salieron unidades de esa entrada (stock actual "
                    + producto.getStock() + ", la entrada sumó " + entrada.getCantidad() + ")");
        }
        entrada.anular(DateUtil.ahora(), motivo);
        entrada.setAnuladaPor(quien);
        producto.disminuirStock(entrada.getCantidad());
        inventario.registrarCambioDeStock(producto);
        entradas.actualizar(entrada);
        return entrada;
    }

    /**
     * Anula una salida y devuelve al stock las unidades que había quitado.
     *
     * @param id     ID de la salida
     * @param motivo por qué se anula
     * @return la salida anulada
     */
    public StockExit anularSalida(String id, String motivo) {
        return anularSalida(id, motivo, null);
    }

    /**
     * Igual que la anulación sin nombre, dejando anotado quién anuló.
     *
     * @param id     ID
     * @param motivo por qué se anula
     * @param quien  nombre de quien anula (puede ser {@code null})
     * @return el movimiento anulado
     */
    public StockExit anularSalida(String id, String motivo, String quien) {
        StockExit salida = salidas.buscarPorId(id).orElseThrow(() -> new ValidationException("Salida no encontrada"));
        salida.anular(DateUtil.ahora(), motivo);
        salida.setAnuladaPor(quien);
        Product producto = inventario.obtener(salida.getProductoId());
        producto.aumentarStock(salida.getCantidad());
        inventario.registrarCambioDeStock(producto);
        salidas.actualizar(salida);
        return salida;
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
        inventario.refrescar();
        movimiento.validar();
        Product producto = inventario.obtenerHabilitado(movimiento.getProductoId());
        movimiento.aplicarA(producto);
        inventario.registrarCambioDeStock(producto);
        movimiento.registrar(IdGenerator.conPrefijo(movimiento.getPrefijoId()), DateUtil.ahora());
        repositorio.agregar(movimiento);
        return movimiento;
    }
}
