package sistemainventario.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tipos de negocio que puede tener un {@link Profile}.
 *
 * <p>Cada tipo declara sus categorías predefinidas, si exige control de
 * vencimiento y textos de ejemplo para los formularios.</p>
 *
 * <p><b>OCP:</b> agregar un tipo de negocio nuevo consiste en declarar una
 * constante más; ni los servicios ni la vista necesitan cambiar porque
 * consumen la información a través de {@link #describir()}.</p>
 */
public enum BusinessType {

    BOUTIQUE("Boutique",
            List.of("Camisas", "Vestidos", "Pantalones", "Zapatos", "Accesorios"),
            true,
            new Suggestions("Blusa Casual Mujer", "Ropa", "Moda Fashion")),

    FARMACIA("Farmacia",
            List.of("Medicamentos", "Vitaminas", "Higiene", "Cosmeticos", "Primeros Auxilios"),
            true,
            new Suggestions("Acetaminofen 500 mg", "Medicamentos", "Laboratorios XYZ")),

    FERRETERIA("Ferreteria",
            List.of("Herramientas", "Pinturas", "Electricidad", "Plomeria", "Tornilleria"),
            true,
            new Suggestions("Martillo de acero", "Herramientas", "FerreDistribuciones")),

    SUPERMERCADO("Supermercado",
            List.of("Frutas", "Verduras", "Carnes", "Lacteos", "Bebidas", "Aseo"),
            true,
            new Suggestions("Leche Entera 1L", "Lacteos", "Alimentos Nacionales")),

    TIENDA_BARRIO("Tienda de barrio",
            List.of("Bebidas", "Lacteos", "Snacks", "Dulces", "Aseo"),
            true,
            new Suggestions("Arroz Diana 500g", "Abarrotes", "Distribuidora Local")),

    TECNOLOGIA("Tecnologia",
            List.of("Computadores", "Celulares", "Accesorios", "Audio", "Gaming"),
            true,
            new Suggestions("Portatil HP 15", "Laptops", "Distribuidora Tecnologica")),

    NINGUNO("Personalizado (sin categorías predefinidas)",
            List.of(),
            true,
            new Suggestions("", "", ""));

    private final String etiqueta;
    private final List<String> categoriasPredefinidas;
    private final boolean requiereVencimiento;
    private final Suggestions sugerencias;

    BusinessType(String etiqueta, List<String> categorias, boolean requiereVencimiento, Suggestions sugerencias) {
        this.etiqueta = etiqueta;
        this.categoriasPredefinidas = Collections.unmodifiableList(categorias);
        this.requiereVencimiento = requiereVencimiento;
        this.sugerencias = sugerencias;
    }

    /**
     * Busca un tipo por su nombre de constante o por su etiqueta.
     *
     * @param clave por ejemplo {@code "FARMACIA"} o {@code "Farmacia"}
     * @return el tipo encontrado, o {@link #NINGUNO} si no existe
     */
    public static BusinessType desdeClave(String clave) {
        if (clave == null || clave.isBlank()) {
            return NINGUNO;
        }
        return Arrays.stream(values())
                .filter(tipo -> tipo.name().equalsIgnoreCase(clave) || tipo.etiqueta.equalsIgnoreCase(clave))
                .findFirst()
                .orElse(NINGUNO);
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public List<String> getCategoriasPredefinidas() {
        return categoriasPredefinidas;
    }

    public boolean tieneCategorias() {
        return !categoriasPredefinidas.isEmpty();
    }

    /** @return {@code true} si los productos de este negocio manejan fecha de vencimiento */
    public boolean requiereControlVencimiento() {
        return requiereVencimiento;
    }

    /** @return mapa con las claves {@code producto}, {@code categoria} y {@code proveedor} */
    public Map<String, String> getSugerencias() {
        return sugerencias.comoMapa();
    }

    /**
     * <b>GRASP Experto:</b> el tipo de negocio sabe describirse para la vista.
     *
     * @return mapa con clave, etiqueta, categorías y si exige vencimiento
     */
    public Map<String, Object> describir() {
        Map<String, Object> descripcion = new LinkedHashMap<>();
        descripcion.put("clave", name());
        descripcion.put("etiqueta", etiqueta);
        descripcion.put("categorias", categoriasPredefinidas);
        descripcion.put("vencimiento", requiereVencimiento);
        return descripcion;
    }

    /** Textos de ejemplo (placeholders) para los formularios de la vista. */
    private record Suggestions(String producto, String categoria, String proveedor) {

        Map<String, String> comoMapa() {
            Map<String, String> mapa = new LinkedHashMap<>();
            mapa.put("producto", producto);
            mapa.put("categoria", categoria);
            mapa.put("proveedor", proveedor);
            return mapa;
        }
    }
}
