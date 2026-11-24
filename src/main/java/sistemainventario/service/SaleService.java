package sistemainventario.service;

import sistemainventario.exception.ValidationException;
import sistemainventario.model.AuditEntry;
import sistemainventario.model.Product;
import sistemainventario.model.Sale;
import sistemainventario.model.SaleItem;
import sistemainventario.repository.DatabaseRoutines;
import sistemainventario.repository.Repository;
import sistemainventario.util.DateUtil;
import sistemainventario.util.IdGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Registro y consulta de ventas.
 *
 * <p>Coordina el caso de uso "registrar venta": valida la venta, comprueba
 * el stock de todos los productos <i>antes</i> de modificar alguno (para no
 * dejar el inventario a medias) y luego descuenta y guarda.</p>
 *
 * <p>Con Oracle, registrar y anular las hace la base con los procedimientos
 * SP_REGISTRAR_VENTA y SP_ANULAR_VENTA ({@link DatabaseRoutines}); Java solo
 * valida los datos de la venta y el medio de pago.</p>
 */
public class SaleService {

    private final InventoryService inventario;
    private final Repository<Sale> ventas;

    /** Medios de pago que acepta el negocio; por defecto, todos. */
    private java.util.function.Predicate<String> medioPermitido = metodo -> true;

    /** Procedimientos de la base (solo Oracle); {@code null} si se usa la lógica de Java. */
    private DatabaseRoutines rutinas;

    public SaleService(InventoryService inventario, Repository<Sale> ventas) {
        this.inventario = Objects.requireNonNull(inventario);
        this.ventas = Objects.requireNonNull(ventas);
    }

    /**
     * @param medioPermitido dice si el negocio acepta un medio de pago (por su etiqueta)
     */
    public void usarMediosDePago(java.util.function.Predicate<String> medioPermitido) {
        this.medioPermitido = Objects.requireNonNull(medioPermitido);
    }

    /**
     * @param rutinas procedimientos y funciones de la base (solo Oracle)
     */
    public void usarRutinas(DatabaseRoutines rutinas) {
        this.rutinas = rutinas;
    }

    /** @return los procedimientos y funciones de la base, si se usan */
    public Optional<DatabaseRoutines> rutinas() {
        return Optional.ofNullable(rutinas);
    }

    /** @return {@code true} si la base anota la anulación en el historial (SP_ANULAR_VENTA) */
    public boolean anotaAnulacionEnLaBase() {
        return rutinas != null;
    }

    /**
     * @param metodo etiqueta del medio de pago
     * @return {@code true} si el negocio acepta ese medio
     */
    public boolean aceptaMedioPago(String metodo) {
        return medioPermitido.test(metodo);
    }

    /**
     * @param venta venta recibida desde la vista
     * @return la venta registrada con ID, fecha, estado y total
     * @throws ValidationException si la venta no es válida o falta stock
     */
    public Sale registrar(Sale venta) {
        if (venta == null) {
            throw new ValidationException("No se recibieron datos de la venta");
        }
        venta.validar();
        venta.normalizarPago();
        if (rutinas != null) {
            return registrarEnLaBase(venta);
        }
        // con varios equipos sobre la misma base, el stock se revisa con lo último guardado
        inventario.refrescar();
        if (!medioPermitido.test(venta.getMetodoPago())) {
            throw new ValidationException("Este negocio no tiene activo el pago con " + venta.getMetodoPago()
                    + ". Actívelo en Editar negocio o elija otro medio.");
        }
        Map<String, Integer> cantidades = venta.cantidadesPorProducto();
        verificarStock(cantidades);
        guardarCostos(venta);
        descontarStock(cantidades);
        venta.completar(IdGenerator.conPrefijo(Sale.PREFIJO_ID), DateUtil.hoy());
        ventas.agregar(venta);
        return venta;
    }

    /** Oracle: SP_REGISTRAR_VENTA revisa el stock, guarda y descuenta en una transacción. */
    private Sale registrarEnLaBase(Sale venta) {
        if (!medioPermitido.test(venta.getMetodoPago())) {
            throw new ValidationException("Este negocio no tiene activo el pago con " + venta.getMetodoPago()
                    + ". Actívelo en Editar negocio o elija otro medio.");
        }
        venta.completar(IdGenerator.conPrefijo(Sale.PREFIJO_ID), DateUtil.hoy());
        rutinas.registrarVenta(venta);
        // se devuelve como quedó en la base (con el costo de cada producto)
        return ventas.buscarPorId(venta.getId()).orElse(venta);
    }

    private void verificarStock(Map<String, Integer> cantidades) {
        cantidades.forEach((productoId, cantidad) ->
                inventario.obtenerHabilitado(productoId).verificarStockDisponible(cantidad));
    }

    /** Guarda en cada ítem el precio de compra actual del producto, para la utilidad real. */
    private void guardarCostos(Sale venta) {
        for (SaleItem item : venta.getItems()) {
            item.setCostoUnitario(inventario.obtener(item.getProductoId()).getPrecioCompra());
        }
    }

    private void descontarStock(Map<String, Integer> cantidades) {
        cantidades.forEach((productoId, cantidad) -> {
            Product producto = inventario.obtener(productoId);
            producto.disminuirStock(cantidad);
            inventario.registrarCambioDeStock(producto);
        });
    }

    /** @return ventas vigentes (sin las anuladas), de la más reciente a la más antigua */
    public List<Sale> listarTodas() {
        return ordenar(ventas.filtrar(v -> !v.isAnulada()));
    }

    /** @return ventas anuladas, de la más reciente a la más antigua */
    public List<Sale> listarAnuladas() {
        return ordenar(ventas.filtrar(Sale::isAnulada));
    }

    /**
     * Anula una venta: queda en el historial con el motivo, deja de contar en los
     * totales y el stock de sus productos se devuelve.
     *
     * @param id     ID de la venta
     * @param motivo por qué se anula
     * @return la venta anulada
     * @throws ValidationException si no existe, ya estaba anulada o falta el motivo
     */
    public Sale anular(String id, String motivo) {
        return anular(id, motivo, (String) null);
    }

    /**
     * @param id  ID de la venta
     * @return la venta (vigente o anulada)
     * @throws ValidationException si no existe
     */
    public Sale obtener(String id) {
        return ventas.buscarPorId(id).orElseThrow(() -> new ValidationException("Venta no encontrada"));
    }

    /**
     * Igual que {@link #anular(String, String)}, dejando anotado quién anuló.
     *
     * @param id     ID de la venta
     * @param motivo por qué se anula
     * @param quien  nombre de quien anula (puede ser {@code null})
     * @return la venta anulada
     */
    public Sale anular(String id, String motivo, String quien) {
        if (rutinas != null) {
            AuditEntry historial = new AuditEntry(DateUtil.ahora(), null, quien, null, null, null, null, null);
            rutinas.anularVenta(id, motivo, historial);
            return obtener(id);
        }
        Sale venta = obtener(id);
        venta.anular(DateUtil.ahora(), motivo);
        venta.setAnuladaPor(quien);
        venta.cantidadesPorProducto().forEach((productoId, cantidad) ->
                inventario.buscarPorId(productoId).ifPresent(producto -> {
                    producto.aumentarStock(cantidad);
                    inventario.registrarCambioDeStock(producto);
                }));
        ventas.actualizar(venta);
        return venta;
    }

    /**
     * Anula una venta dejando anotado quién lo hizo. Con Oracle, SP_ANULAR_VENTA
     * también escribe el historial (AUDITORIA) con estos datos.
     *
     * @param id        ID de la venta
     * @param motivo    por qué se anula
     * @param historial fecha, usuario, rol y negocio de quien anula
     * @return la venta anulada
     */
    public Sale anular(String id, String motivo, AuditEntry historial) {
        if (rutinas != null) {
            rutinas.anularVenta(id, motivo, historial);
            return obtener(id);
        }
        return anular(id, motivo, historial.getUsuario());
    }

    private static List<Sale> ordenar(List<Sale> origen) {
        List<Sale> lista = new ArrayList<>(origen);
        lista.forEach(Sale::recalcularTotal);
        Collections.reverse(lista);
        return lista;
    }

    /** @return suma de los totales de las ventas completadas */
    public double getTotalVentas() {
        return ventas.filtrar(Sale::estaCompletada).stream().mapToDouble(Sale::getTotal).sum();
    }

    /** @return cantidad de ventas completadas */
    public int getTotalTransacciones() {
        return ventas.filtrar(Sale::estaCompletada).size();
    }
}
