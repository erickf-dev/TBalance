package sistemainventario.service;

import sistemainventario.model.Product;
import sistemainventario.repository.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Casos de uso del catálogo de productos: CRUD, búsqueda, filtros y alertas
 * de stock y vencimiento.
 *
 * <p><b>Alta cohesión:</b> todas sus operaciones giran alrededor de los
 * productos. Las preguntas sobre un producto concreto (¿está vencido?, ¿está
 * en bajo stock?) se delegan al propio {@link Product} (<b>Experto</b>).</p>
 */
public class InventoryService extends EntityService<Product> {

    public InventoryService(Repository<Product> repositorio) {
        super(repositorio);
    }

    @Override
    protected void validar(Product producto) {
        producto.validar(LocalDate.now());
    }

    @Override
    protected String generarId() {
        return repositorio.siguienteIdNumerico();
    }

    @Override
    protected String nombreEntidad() {
        return "Producto";
    }

    /**
     * Persiste un cambio de stock sin volver a validar la ficha completa;
     * así un producto ya vencido puede seguir recibiendo movimientos.
     *
     * @param producto producto con el stock modificado
     */
    public void registrarCambioDeStock(Product producto) {
        repositorio.actualizar(producto);
    }

    /**
     * @param texto texto a buscar; si está vacío se devuelven todos
     * @return productos que coinciden por nombre, descripción, código o ID
     */
    public List<Product> buscarPorTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return listarTodos();
        }
        return repositorio.filtrar(producto -> producto.coincideCon(texto));
    }

    /**
     * @param categoria nombre de la categoría; {@code "Todas"} o vacío devuelve todo
     * @return productos de la categoría
     */
    public List<Product> filtrarPorCategoria(String categoria) {
        if (categoria == null || categoria.isBlank() || "Todas".equals(categoria)) {
            return listarTodos();
        }
        return repositorio.filtrar(producto -> producto.perteneceACategoria(categoria));
    }

    /**
     * Utilidad estimada del inventario: lo que se ganaría vendiendo todo el
     * stock de los productos que tienen precio de compra.
     *
     * @return suma de {@link Product#getUtilidadEstimadaStock()}
     */
    public double getUtilidadEstimadaInventario() {
        return repositorio.listarTodos().stream()
                .filter(Product::tienePrecioCompra)
                .mapToDouble(Product::getUtilidadEstimadaStock)
                .sum();
    }

    /** @return productos sin precio de compra, cuya utilidad no se puede estimar */
    public List<Product> getSinPrecioCompra() {
        return repositorio.filtrar(producto -> !producto.tienePrecioCompra());
    }

    /** @return productos con stock en el mínimo o por debajo (sin estar agotados) */
    public List<Product> getBajoStock() {
        return repositorio.filtrar(Product::estaBajoStock);
    }

    /** @return productos sin unidades */
    public List<Product> getAgotados() {
        return repositorio.filtrar(Product::estaAgotado);
    }

    /** @return productos cuya fecha de vencimiento ya pasó */
    public List<Product> getVencidos() {
        LocalDate hoy = LocalDate.now();
        return repositorio.filtrar(producto -> producto.estaVencido(hoy));
    }

    /**
     * @param dias ventana de días
     * @return productos que vencen dentro de los próximos {@code dias} días
     */
    public List<Product> getPorVencer(int dias) {
        LocalDate hoy = LocalDate.now();
        return repositorio.filtrar(producto -> producto.venceDentroDe(dias, hoy));
    }

    /** @return nombres de categorías usadas por los productos, ordenados */
    public Set<String> getCategorias() {
        return listarTodos().stream()
                .map(Product::getCategoria)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
