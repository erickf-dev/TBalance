package sistemainventario.model;

/**
 * Acciones de TBalance que dependen del rol. La interfaz oculta lo que el rol
 * no puede usar y {@link sistemainventario.bridge.AccessPolicy} lo vuelve a
 * revisar en Java antes de cada llamada.
 */
public enum Permiso {
    /** Vender, vender con QR y registrar fiado. */
    VENDER,
    /** Ver la ganancia y los precios de compra. */
    VER_GANANCIA,
    /** Crear o editar productos, categorías y proveedores, e imprimir etiquetas. */
    EDITAR_CATALOGO,
    /** Registrar y consultar entradas y salidas de mercancía. */
    MOVIMIENTOS,
    /** Anular cualquier venta, entrada o salida (con motivo). */
    ANULAR,
    /** Deshabilitar y volver a habilitar productos, categorías y proveedores. */
    DESHABILITAR,
    /** Ver Informes y exportarlos. */
    INFORMES,
    /** Ver la sección Costos. */
    COSTOS,
    /** Crear usuarios del negocio, deshabilitarlos y restablecer su contraseña. */
    USUARIOS,
    /** Crear, editar, deshabilitar y habilitar negocios. */
    NEGOCIOS,
    /** Crear, restaurar y eliminar copias de seguridad. */
    RESPALDOS,
    /** Elegir la base de datos y configurar la clave del asistente. */
    CONFIGURACION,
    /** Ver el historial de acciones (quién hizo qué). */
    HISTORIAL
}
