package sistemainventario.model;

import sistemainventario.exception.ValidationException;
import sistemainventario.util.DateUtil;
import sistemainventario.util.TextUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Espacio de trabajo independiente (un negocio). Cada perfil tiene su propio
 * directorio con productos, categorías, proveedores, movimientos y ventas.
 *
 * <p><b>SRP:</b> solo modela los datos del perfil; su ciclo de vida lo
 * administra {@link sistemainventario.service.ProfileService}.</p>
 */
public class Profile extends IdentifiedEntity {

    public static final String COLOR_POR_DEFECTO = "#d4821b";

    private String nombre;
    private String descripcion;
    private String color;
    private String tipoNegocio;
    private String fechaCreacion;
    private Map<String, String> configuracion = new LinkedHashMap<>();

    /** Constructor vacío requerido por Gson. */
    public Profile() {
        super();
    }

    public Profile(String id, String nombre, String descripcion, String color, String tipoNegocio) {
        super(id);
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.color = TextUtil.esVacio(color) ? COLOR_POR_DEFECTO : color;
        this.tipoNegocio = tipoNegocio;
        this.fechaCreacion = DateUtil.hoy();
        inicializarConfiguracionPorDefecto();
    }

    private void inicializarConfiguracionPorDefecto() {
        configuracion.put("moneda", "COP");
        configuracion.put("stockMin", "5");
        configuracion.put("notificaciones", "true");
    }

    /**
     * @throws ValidationException si el nombre está vacío
     */
    public void validar() {
        if (TextUtil.esVacio(nombre)) {
            throw new ValidationException("El nombre del perfil es obligatorio");
        }
    }

    /**
     * <b>GRASP Experto:</b> actualiza solo los datos editables y conserva la
     * fecha de creación, la configuración y el tipo de negocio (que no puede
     * cambiarse después de crear el perfil).
     *
     * @param nuevoNombre      nombre nuevo (obligatorio)
     * @param nuevaDescripcion descripción nueva (opcional)
     * @param nuevoColor       color nuevo; si está vacío se conserva el actual
     */
    public void actualizarDatos(String nuevoNombre, String nuevaDescripcion, String nuevoColor) {
        if (TextUtil.esVacio(nuevoNombre)) {
            throw new ValidationException("El nombre del perfil es obligatorio");
        }
        this.nombre = nuevoNombre.trim();
        this.descripcion = TextUtil.recortar(nuevaDescripcion);
        if (!TextUtil.esVacio(nuevoColor)) {
            this.color = nuevoColor;
        }
    }

    /** @return el tipo de negocio como enumeración ({@code NINGUNO} si no tiene) */
    public BusinessType getPerfilNegocio() {
        return BusinessType.desdeClave(tipoNegocio);
    }

    /** @return {@code true} si el tipo de negocio exige controlar vencimientos */
    public boolean requiereControlVencimiento() {
        return getPerfilNegocio().requiereControlVencimiento();
    }

    /**
     * @param clave   clave de configuración
     * @param defecto valor devuelto si la clave no existe
     * @return el valor configurado o {@code defecto}
     */
    public String getConfig(String clave, String defecto) {
        return configuracion == null ? defecto : configuracion.getOrDefault(clave, defecto);
    }

    public void setConfig(String clave, String valor) {
        if (configuracion == null) {
            configuracion = new LinkedHashMap<>();
        }
        configuracion.put(clave, valor);
    }

    @Override
    public String getEtiqueta() {
        return nombre != null ? nombre : super.getEtiqueta();
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getTipoNegocio() {
        return tipoNegocio;
    }

    public void setTipoNegocio(String tipoNegocio) {
        this.tipoNegocio = tipoNegocio;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(String fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Map<String, String> getConfiguracion() {
        return configuracion;
    }

    public void setConfiguracion(Map<String, String> configuracion) {
        this.configuracion = configuracion;
    }
}
