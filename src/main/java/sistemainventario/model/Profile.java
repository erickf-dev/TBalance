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
public class Profile extends IdentifiedEntity implements Deshabilitable {

    public static final String COLOR_POR_DEFECTO = "#315D53";

    private String nombre;
    private String descripcion;
    private String color;
    private String tipoNegocio;
    private String fechaCreacion;
    private Map<String, String> configuracion = new LinkedHashMap<>();

    /** {@code null} en los datos de versiones anteriores, que cuentan como habilitados. */
    private Boolean activo;

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

    /**
     * Cuentas donde el negocio recibe pagos digitales (se muestran al cobrar).
     * TBalance solo las muestra: no se conecta con los bancos.
     */
    public static final java.util.List<String> CLAVES_CUENTAS = java.util.List.of(
            "cuentaNequi", "cuentaDaviplata", "cuentaBancolombia", "llaveBreB");

    /**
     * Copia de otro perfil los medios de pago y las cuentas que llegaron al
     * crear o editar el negocio (solo esas claves, recortadas a 60 caracteres).
     *
     * @param datos perfil recibido de la pantalla
     */
    public void copiarAjustesDePago(Profile datos) {
        if (datos == null || datos.getConfiguracion() == null) {
            return;
        }
        String medios = datos.getConfiguracion().get(CLAVE_MEDIOS_PAGO);
        if (medios != null) {
            setMediosPago(java.util.Arrays.asList(medios.split(",")));
        }
        for (String clave : CLAVES_CUENTAS) {
            String valor = datos.getConfiguracion().get(clave);
            if (valor != null) {
                String limpio = valor.replaceAll("[\\r\\n\\t]", " ").trim();
                setConfig(clave, limpio.length() > 60 ? limpio.substring(0, 60) : limpio);
            }
        }
    }

    /** Clave de configuración con los medios de pago que acepta el negocio. */
    public static final String CLAVE_MEDIOS_PAGO = "mediosPago";

    /**
     * Medios de pago que el negocio acepta. Si nunca se configuró, acepta
     * todos; el efectivo siempre queda activo.
     *
     * @return etiquetas oficiales de los medios activos, en el orden del catálogo
     */
    public java.util.Set<String> getMediosPago() {
        String guardado = getConfig(CLAVE_MEDIOS_PAGO, "");
        java.util.Set<String> activos = new java.util.LinkedHashSet<>();
        for (PaymentMethod m : PaymentMethod.values()) {
            if (guardado.isBlank() || m == PaymentMethod.EFECTIVO
                    || java.util.Arrays.asList(guardado.split(",")).contains(m.getEtiqueta())) {
                activos.add(m.getEtiqueta());
            }
        }
        return activos;
    }

    /**
     * Guarda los medios de pago activos. Ignora los que no reconoce y siempre
     * deja el efectivo.
     *
     * @param medios etiquetas o nombres de los medios activos
     */
    public void setMediosPago(java.util.Collection<String> medios) {
        java.util.Set<PaymentMethod> elegidos = java.util.EnumSet.of(PaymentMethod.EFECTIVO);
        if (medios != null) {
            medios.forEach(t -> PaymentMethod.desdeTexto(t).ifPresent(elegidos::add));
        }
        setConfig(CLAVE_MEDIOS_PAGO, String.join(",", elegidos.stream().map(PaymentMethod::getEtiqueta).toList()));
    }

    /**
     * @param metodo etiqueta del medio de pago
     * @return {@code true} si el negocio acepta ese medio
     */
    public boolean aceptaMedioPago(String metodo) {
        return getMediosPago().contains(metodo);
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

    @Override
    public boolean isActivo() {
        return !Boolean.FALSE.equals(activo);
    }

    @Override
    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
