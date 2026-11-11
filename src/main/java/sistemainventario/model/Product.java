package sistemainventario.model;

import sistemainventario.exception.ValidationException;
import sistemainventario.util.DateUtil;
import sistemainventario.util.TextUtil;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

/**
 * Artículo del inventario de un perfil.
 *
 * <p><b>GRASP Experto en Información:</b> el producto es quien posee el stock,
 * el stock mínimo y la fecha de vencimiento; por eso es él quien decide su
 * estado, si tiene existencias suficientes, si está vencido y cómo se
 * incrementa o descuenta su stock. Los servicios ya no manipulan
 * {@code setStock(...)} directamente.</p>
 *
 * <p>Los nombres de los atributos no cambian respecto a P2 para mantener la
 * compatibilidad con {@code productos.json}.</p>
 */
public class Product extends IdentifiedEntity {

    public static final String ESTADO_AGOTADO = "Agotado";
    public static final String ESTADO_BAJO_STOCK = "Bajo Stock";
    public static final String ESTADO_DISPONIBLE = "Disponible";

    private String nombre;
    private String codigo;
    private String descripcion;
    private String categoria;
    private String categoriaId;
    private String proveedorId;
    private double precioCompra;
    private double precioVenta;
    private int stock;
    private int stockMin;
    private String tipo;
    private boolean requiereVencimiento;
    private String fechaVencimiento;

    /** Constructor vacío requerido por Gson. */
    public Product() {
        super();
    }

    public Product(String id, String nombre, String descripcion,
                    String categoria, String categoriaId, String proveedorId,
                    double precioCompra, double precioVenta,
                    int stock, int stockMin, String tipo) {
        super(id);
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.categoriaId = categoriaId;
        this.proveedorId = proveedorId;
        this.precioCompra = precioCompra;
        this.precioVenta = precioVenta;
        this.stock = stock;
        this.stockMin = stockMin;
        this.tipo = tipo;
    }

    // ── Validación ──────────────────────────────────────────────────────

    /**
     * Verifica las reglas de negocio del producto antes de guardarlo.
     *
     * @param hoy fecha de referencia para validar el vencimiento
     * @throws ValidationException si algún dato es inválido
     */
    public void validar(LocalDate hoy) {
        if (TextUtil.esVacio(nombre)) {
            throw new ValidationException("El nombre del producto es obligatorio");
        }
        if (precioCompra < 0 || precioVenta < 0) {
            throw new ValidationException("Los precios no pueden ser negativos");
        }
        if (stock < 0 || stockMin < 0) {
            throw new ValidationException("El stock no puede ser negativo");
        }
        validarFechaVencimiento(hoy);
    }

    private void validarFechaVencimiento(LocalDate hoy) {
        if (!requiereVencimiento) {
            return;
        }
        if (TextUtil.esVacio(fechaVencimiento)) {
            throw new ValidationException("La fecha de vencimiento es obligatoria");
        }
        LocalDate fecha = getFechaVencimientoComoFecha()
                .orElseThrow(() -> new ValidationException("Formato de fecha invalido (use yyyy-MM-dd)"));
        if (fecha.isBefore(hoy)) {
            throw new ValidationException("La fecha de vencimiento no puede ser anterior a hoy");
        }
    }

    // ── Stock (Experto) ─────────────────────────────────────────────────

    /** @return el estado legible según el stock actual y el mínimo */
    public String getEstado() {
        if (estaAgotado()) {
            return ESTADO_AGOTADO;
        }
        return estaBajoStock() ? ESTADO_BAJO_STOCK : ESTADO_DISPONIBLE;
    }

    /** @return {@code true} si no quedan unidades */
    public boolean estaAgotado() {
        return stock <= 0;
    }

    /** @return {@code true} si quedan unidades pero en el mínimo o por debajo */
    public boolean estaBajoStock() {
        return stock > 0 && stock <= stockMin;
    }

    /**
     * @param cantidad unidades requeridas
     * @return {@code true} si el stock cubre la cantidad
     */
    public boolean tieneStockPara(int cantidad) {
        return stock >= cantidad;
    }

    /**
     * Incrementa el stock (entrada de mercancía).
     *
     * @param cantidad unidades a sumar, debe ser mayor que cero
     * @throws ValidationException si la cantidad no es positiva
     */
    public void aumentarStock(int cantidad) {
        validarCantidadPositiva(cantidad);
        stock += cantidad;
    }

    /**
     * Descuenta stock (salida o venta).
     *
     * @param cantidad unidades a descontar, debe ser mayor que cero
     * @throws ValidationException si la cantidad no es positiva o el stock es insuficiente
     */
    public void disminuirStock(int cantidad) {
        validarCantidadPositiva(cantidad);
        verificarStockDisponible(cantidad);
        stock -= cantidad;
    }

    /**
     * @param cantidad unidades requeridas
     * @throws ValidationException si el stock no alcanza
     */
    public void verificarStockDisponible(int cantidad) {
        if (!tieneStockPara(cantidad)) {
            throw new ValidationException("Stock insuficiente para " + getEtiqueta()
                    + ". Disponible: " + stock);
        }
    }

    private static void validarCantidadPositiva(int cantidad) {
        if (cantidad <= 0) {
            throw new ValidationException("La cantidad debe ser mayor que cero");
        }
    }

    // ── Vencimiento (Experto) ───────────────────────────────────────────

    /** @return la fecha de vencimiento si está controlada y es válida */
    public Optional<LocalDate> getFechaVencimientoComoFecha() {
        return requiereVencimiento ? DateUtil.parsear(fechaVencimiento) : Optional.empty();
    }

    /**
     * @param hoy fecha de referencia
     * @return {@code true} si el producto controla vencimiento y la fecha ya pasó
     */
    public boolean estaVencido(LocalDate hoy) {
        return getFechaVencimientoComoFecha().map(f -> f.isBefore(hoy)).orElse(false);
    }

    /**
     * @param dias ventana de días a evaluar
     * @param hoy  fecha de referencia
     * @return {@code true} si vence desde hoy y antes de {@code hoy + dias}
     */
    public boolean venceDentroDe(int dias, LocalDate hoy) {
        LocalDate limite = hoy.plusDays(dias);
        return getFechaVencimientoComoFecha()
                .map(f -> !f.isBefore(hoy) && f.isBefore(limite))
                .orElse(false);
    }

    // ── Búsqueda (Experto) ──────────────────────────────────────────────

    /**
     * @param texto texto buscado en nombre, descripción, código o ID
     * @return {@code true} si hay coincidencia (ignorando mayúsculas)
     */
    public boolean coincideCon(String texto) {
        return TextUtil.contieneIgnorandoMayusculas(nombre, texto)
                || TextUtil.contieneIgnorandoMayusculas(descripcion, texto)
                || TextUtil.contieneIgnorandoMayusculas(codigo, texto)
                || TextUtil.contieneIgnorandoMayusculas(getId(), texto);
    }

    /**
     * @param nombreCategoria categoría a comparar
     * @return {@code true} si el producto pertenece a esa categoría
     */
    public boolean perteneceACategoria(String nombreCategoria) {
        return nombreCategoria != null && nombreCategoria.equals(categoria);
    }

    // ── Etiqueta QR (Experto) ───────────────────────────────────────────

    /** Primera línea de toda etiqueta QR de TBalance. */
    public static final String ENCABEZADO_QR = "TBalance";
    /** Prefijo de la línea que lleva el ID dentro de la etiqueta QR. */
    private static final String PREFIJO_ID_QR = "ID: ";

    /**
     * <b>GRASP Experto:</b> el producto conoce sus datos, por eso arma el texto
     * que se codifica en su etiqueta QR. El texto es corto a propósito: un QR
     * con pocos datos tiene módulos más grandes y la webcam lo lee mejor. El
     * nombre no va en el código porque ya está impreso al lado.
     *
     * @return texto de tres líneas: {@code TBalance}, el ID y el precio de venta
     */
    public String contenidoEtiquetaQR() {
        return ENCABEZADO_QR + '\n'
                + PREFIJO_ID_QR + getId() + '\n'
                + getPrecioVentaFormateado();
    }

    /**
     * Operación inversa de {@link #contenidoEtiquetaQR()}: obtiene el ID del
     * producto a partir del texto leído de una etiqueta. Como el formato lo
     * define esta clase, también es la <b>Experta</b> en interpretarlo.
     *
     * @param contenido texto leído del código QR
     * @return el ID, o vacío si el texto no es una etiqueta de TBalance
     */
    public static Optional<String> idDesdeEtiquetaQR(String contenido) {
        if (contenido == null || !contenido.strip().startsWith(ENCABEZADO_QR)) {
            return Optional.empty();
        }
        return contenido.lines()
                .map(String::strip)
                .filter(linea -> linea.startsWith(PREFIJO_ID_QR))
                .map(linea -> linea.substring(PREFIJO_ID_QR.length()).strip())
                .filter(id -> !id.isEmpty())
                .findFirst();
    }

    /** @return precio de venta con separador de miles, por ejemplo {@code $ 2.500} */
    public String getPrecioVentaFormateado() {
        return "$ " + String.format(Locale.ROOT, "%,.0f", precioVenta).replace(',', '.');
    }

    @Override
    public String getEtiqueta() {
        return nombre != null ? nombre : super.getEtiqueta();
    }

    // ── Getters y setters (encapsulamiento) ─────────────────────────────

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(String categoriaId) {
        this.categoriaId = categoriaId;
    }

    public String getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(String proveedorId) {
        this.proveedorId = proveedorId;
    }

    public double getPrecioCompra() {
        return precioCompra;
    }

    public void setPrecioCompra(double precioCompra) {
        this.precioCompra = precioCompra;
    }

    public double getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(double precioVenta) {
        this.precioVenta = precioVenta;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getStockMin() {
        return stockMin;
    }

    public void setStockMin(int stockMin) {
        this.stockMin = stockMin;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public boolean isRequiereVencimiento() {
        return requiereVencimiento;
    }

    public void setRequiereVencimiento(boolean requiereVencimiento) {
        this.requiereVencimiento = requiereVencimiento;
    }

    public String getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(String fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }
}
