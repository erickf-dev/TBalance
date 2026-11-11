package sistemainventario.service;

import sistemainventario.exception.ValidationException;
import sistemainventario.model.Product;
import sistemainventario.model.Sale;
import sistemainventario.repository.Repository;
import sistemainventario.util.DateUtil;
import sistemainventario.util.IdGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Registro y consulta de ventas.
 *
 * <p>Coordina el caso de uso "registrar venta": valida la venta, comprueba
 * el stock de todos los productos <i>antes</i> de modificar alguno (para no
 * dejar el inventario a medias) y luego descuenta y guarda.</p>
 */
public class SaleService {

    private final InventoryService inventario;
    private final Repository<Sale> ventas;

    public SaleService(InventoryService inventario, Repository<Sale> ventas) {
        this.inventario = Objects.requireNonNull(inventario);
        this.ventas = Objects.requireNonNull(ventas);
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
        Map<String, Integer> cantidades = venta.cantidadesPorProducto();
        verificarStock(cantidades);
        descontarStock(cantidades);
        venta.completar(IdGenerator.conPrefijo(Sale.PREFIJO_ID), DateUtil.hoy());
        ventas.agregar(venta);
        return venta;
    }

    private void verificarStock(Map<String, Integer> cantidades) {
        cantidades.forEach((productoId, cantidad) ->
                inventario.obtener(productoId).verificarStockDisponible(cantidad));
    }

    private void descontarStock(Map<String, Integer> cantidades) {
        cantidades.forEach((productoId, cantidad) -> {
            Product producto = inventario.obtener(productoId);
            producto.disminuirStock(cantidad);
            inventario.registrarCambioDeStock(producto);
        });
    }

    /** @return ventas de la más reciente a la más antigua, con el total sincronizado */
    public List<Sale> listarTodas() {
        List<Sale> lista = new ArrayList<>(ventas.listarTodos());
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
