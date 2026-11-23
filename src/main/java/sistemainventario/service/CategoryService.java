package sistemainventario.service;

import sistemainventario.exception.ValidationException;
import sistemainventario.model.Category;
import sistemainventario.model.Product;
import sistemainventario.model.BusinessType;
import sistemainventario.repository.Repository;
import sistemainventario.util.IdGenerator;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Casos de uso de las categorías de un perfil.
 *
 * <p><b>GRASP Creator:</b> es quien agrega y registra las categorías, por eso
 * crea las categorías predefinidas de un tipo de negocio
 * ({@link #crearPredefinidas(BusinessType)}).</p>
 */
public class CategoryService extends EntityService<Category> {

    /** Prefijo que P2 ya usaba en {@code categorias.json}. */
    public static final String PREFIJO_ID = "CAT";
    /** Categoría para los productos que se crearon sin una (la categoría es obligatoria). */
    public static final String SIN_CATEGORIA_ID = "CAT-SIN";
    /** Nombre de {@link #SIN_CATEGORIA_ID}. */
    public static final String SIN_CATEGORIA = "Sin categoría";

    /** Productos habilitados del negocio, para no deshabilitar una categoría en uso. */
    private Supplier<List<Product>> productos = List::of;

    public CategoryService(Repository<Category> repositorio) {
        super(repositorio);
        asignarIdsFaltantes();
    }

    /** @param productos fuente de los productos habilitados del mismo negocio */
    public void usarProductos(Supplier<List<Product>> productos) {
        this.productos = productos;
    }

    /**
     * Completa la categoría de un producto: con el ID busca el nombre, y con
     * solo el nombre busca el ID.
     *
     * @param producto producto a guardar
     * @throws ValidationException si no tiene categoría o la categoría no existe o está deshabilitada
     */
    public void resolverCategoria(Product producto) {
        Optional<Category> porId = Optional.ofNullable(producto.getCategoriaId()).flatMap(this::buscarPorId);
        Optional<Category> elegida = porId.or(() -> buscarPorNombre(producto.getCategoria()));
        if (elegida.isEmpty()) {
            boolean vacia = esVacio(producto.getCategoriaId()) && esVacio(producto.getCategoria());
            throw new ValidationException(vacia ? "Elija la categoría del producto"
                    : "La categoría " + producto.getCategoria() + " no existe");
        }
        if (!elegida.get().isActivo()) {
            throw new ValidationException("La categoría " + elegida.get().getNombre() + " está deshabilitada");
        }
        producto.setCategoriaId(elegida.get().getId());
        producto.setCategoria(elegida.get().getNombre());
    }

    /** @return la categoría "Sin categoría", creándola si no existe */
    public Category sinCategoria() {
        return buscarPorId(SIN_CATEGORIA_ID).orElseGet(() -> {
            Category nueva = new Category(SIN_CATEGORIA_ID, SIN_CATEGORIA,
                    "Productos que se registraron sin categoría; cámbielos a la que corresponda");
            repositorio.agregar(nueva);
            return nueva;
        });
    }

    /**
     * @param nombre nombre de la categoría (sin importar mayúsculas)
     * @return la categoría con ese nombre, habilitada o no
     */
    public Optional<Category> buscarPorNombre(String nombre) {
        if (esVacio(nombre)) {
            return Optional.empty();
        }
        String buscado = nombre.trim().toLowerCase(Locale.ROOT);
        return repositorio.listarTodos().stream()
                .filter(c -> c.getNombre() != null && c.getNombre().trim().toLowerCase(Locale.ROOT).equals(buscado))
                .findFirst();
    }

    @Override
    protected void validarDeshabilitar(Category categoria) {
        if (SIN_CATEGORIA_ID.equals(categoria.getId())) {
            throw new ValidationException("La categoría " + SIN_CATEGORIA + " no se puede deshabilitar");
        }
        long enUso = productos.get().stream().filter(p -> categoria.getId().equals(p.getCategoriaId())).count();
        if (enUso > 0) {
            throw new ValidationException("Tiene " + enUso + (enUso == 1 ? " producto" : " productos")
                    + " habilitados; cámbielos de categoría primero");
        }
    }

    private static boolean esVacio(String texto) {
        return texto == null || texto.isBlank();
    }

    /**
     * Migración de datos de P2: las categorías predefinidas se guardaban con
     * {@code id = null}, lo que impedía editarlas o eliminarlas. Al cargar el
     * perfil se les asigna un ID único.
     */
    private void asignarIdsFaltantes() {
        List<Category> sinId = repositorio.filtrar(categoria -> !categoria.tieneId());
        sinId.stream().map(Category::getId).distinct().forEach(repositorio::eliminar);
        sinId.forEach(categoria -> {
            categoria.setId(generarId());
            repositorio.agregar(categoria);
        });
    }

    @Override
    protected void validar(Category categoria) {
        categoria.validar();
    }

    @Override
    protected String generarId() {
        return IdGenerator.conPrefijo(PREFIJO_ID);
    }

    @Override
    protected String nombreEntidad() {
        return "Categoria";
    }

    /**
     * <b>Creator:</b> crea las categorías iniciales de un tipo de negocio.
     * En P2 se guardaban sin ID; ahora cada una recibe un ID único.
     *
     * @param tipoNegocio tipo elegido al crear el perfil
     */
    public void crearPredefinidas(BusinessType tipoNegocio) {
        String descripcion = "Categoria predefinida para " + tipoNegocio.getEtiqueta();
        for (String nombre : tipoNegocio.getCategoriasPredefinidas()) {
            guardar(new Category(null, nombre, descripcion));
        }
    }
}
