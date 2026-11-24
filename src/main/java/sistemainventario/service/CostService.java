package sistemainventario.service;

import sistemainventario.model.Product;
import sistemainventario.model.StockEntry;
import sistemainventario.model.Supplier;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Costos del negocio por sus productos: cuánto cuesta cada producto, cuánto
 * costó todo lo que hay en inventario y cuánto se ha comprado (entradas) en un
 * periodo.
 *
 * <p><b>Experto:</b> cada {@link Product} conoce su costo y su stock; este
 * servicio solo suma. Las entradas registradas antes de que existiera el costo
 * por unidad no se suman: se cuentan aparte como "sin costo".</p>
 */
public class CostService {

    private final InventoryService inventario;
    private final MovementService movimientos;
    private final SupplierService proveedores;
    /** FN_VALOR_INVENTARIO (solo Oracle); {@code null} para sumar en Java. */
    private sistemainventario.repository.DatabaseRoutines rutinas;

    /**
     * Costo de un producto.
     *
     * @param id               ID del producto
     * @param nombre           nombre
     * @param categoria        categoría
     * @param costoUnidad      precio de compra por unidad
     * @param stock            unidades disponibles
     * @param costoStock       costo de las unidades disponibles (stock × costo)
     * @param gananciaEsperada ganancia si se venden todas a su precio actual
     */
    public record ProductCost(String id, String nombre, String categoria, double costoUnidad, int stock,
                              double costoStock, double gananciaEsperada) {
    }

    /**
     * Una compra (entrada con costo) del periodo.
     *
     * @param fecha       día en que llegó
     * @param producto    nombre del producto
     * @param proveedor   proveedor, o vacío
     * @param cantidad    unidades
     * @param costoUnidad costo por unidad (0 si no se registró)
     * @param total       cantidad × costo
     */
    public record Purchase(String fecha, String producto, String proveedor, int cantidad, double costoUnidad,
                           double total) {
    }

    /**
     * Resumen de costos.
     *
     * @param valorInventario     cuánto costó lo que hay en inventario
     * @param gananciaEsperada    ganancia si se vende todo el inventario
     * @param productosSinCosto   productos sin precio de compra
     * @param productos           costo por producto, del más costoso en inventario al menor
     * @param totalCompras        total comprado en el periodo
     * @param entradasSinCosto    entradas del periodo sin costo registrado
     * @param compras             compras del periodo, de la más reciente a la más antigua
     * @param comprasPorProveedor total comprado a cada proveedor en el periodo
     */
    public record Summary(double valorInventario, double gananciaEsperada, int productosSinCosto,
                          List<ProductCost> productos, double totalCompras, int entradasSinCosto,
                          List<Purchase> compras, Map<String, Double> comprasPorProveedor) {
    }

    /**
     * @param inventario  productos del negocio
     * @param movimientos entradas del negocio
     * @param proveedores proveedores del negocio
     */
    public CostService(InventoryService inventario, MovementService movimientos, SupplierService proveedores) {
        this.inventario = Objects.requireNonNull(inventario);
        this.movimientos = Objects.requireNonNull(movimientos);
        this.proveedores = Objects.requireNonNull(proveedores);
    }

    /**
     * @param rutinas funciones de la base (solo Oracle)
     * @return este servicio
     */
    public CostService usarRutinas(sistemainventario.repository.DatabaseRoutines rutinas) {
        this.rutinas = rutinas;
        return this;
    }

    /**
     * @param desde primer día del periodo de compras (incluido), o {@code null} sin límite
     * @param hasta último día del periodo de compras (incluido), o {@code null} sin límite
     * @return costos del inventario y compras del periodo
     */
    public Summary resumen(LocalDate desde, LocalDate hasta) {
        List<ProductCost> productos = new ArrayList<>();
        double valor = 0;
        double ganancia = 0;
        int sinCosto = 0;
        for (Product p : inventario.listarTodos()) {
            double costoStock = p.getStock() * p.getPrecioCompra();
            double esperada = p.getPrecioCompra() > 0 ? p.getStock() * (p.getPrecioVenta() - p.getPrecioCompra()) : 0;
            productos.add(new ProductCost(p.getId(), p.getNombre(), p.getCategoria(), p.getPrecioCompra(),
                    p.getStock(), costoStock, esperada));
            valor += costoStock;
            ganancia += esperada;
            if (p.getPrecioCompra() <= 0) {
                sinCosto++;
            }
        }
        productos.sort(Comparator.comparingDouble(ProductCost::costoStock).reversed());
        if (rutinas != null) {
            // Oracle: el valor del inventario lo calcula FN_VALOR_INVENTARIO
            valor = rutinas.valorInventario();
        }

        List<Purchase> compras = new ArrayList<>();
        Map<String, Double> porProveedor = new LinkedHashMap<>();
        double totalCompras = 0;
        int entradasSinCosto = 0;
        for (StockEntry e : movimientos.listarEntradas()) {
            LocalDate dia = dia(e.getFecha());
            if (dia == null || (desde != null && dia.isBefore(desde)) || (hasta != null && dia.isAfter(hasta))) {
                continue;
            }
            String producto = inventario.buscarPorId(e.getProductoId()).map(Product::getNombre).orElse(e.getProductoId());
            String proveedor = e.getProveedorId() == null ? ""
                    : proveedores.buscarPorId(e.getProveedorId()).map(Supplier::getNombre).orElse("");
            double total = e.getCantidad() * e.getPrecioUnitario();
            compras.add(new Purchase(dia.toString(), producto, proveedor, e.getCantidad(), e.getPrecioUnitario(), total));
            if (e.getPrecioUnitario() <= 0) {
                entradasSinCosto++;
                continue;
            }
            totalCompras += total;
            porProveedor.merge(proveedor.isEmpty() ? "Sin proveedor" : proveedor, total, Double::sum);
        }
        compras.sort(Comparator.comparing(Purchase::fecha).reversed());
        return new Summary(valor, ganancia, sinCosto, productos, totalCompras, entradasSinCosto, compras, porProveedor);
    }

    private static LocalDate dia(String fecha) {
        if (fecha == null || fecha.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(fecha.substring(0, 10));
        } catch (RuntimeException e) {
            return null;
        }
    }
}
