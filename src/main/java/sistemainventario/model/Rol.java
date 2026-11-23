package sistemainventario.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Rol de una persona dentro de un negocio.
 *
 * <ul>
 *   <li><b>Dueño:</b> todo, en todos sus negocios.</li>
 *   <li><b>Administrador</b> (el encargado): maneja la tienda día a día, pero no
 *       toca copias de seguridad, la base de datos ni los negocios, y solo crea cajeros.</li>
 *   <li><b>Cajero:</b> vende y consulta; puede anular sus propias ventas en los
 *       primeros {@value #MINUTOS_ANULAR_PROPIA} minutos.</li>
 * </ul>
 */
public enum Rol {
    DUENO("Dueño", EnumSet.allOf(Permiso.class)),
    ADMINISTRADOR("Administrador", EnumSet.of(Permiso.VENDER, Permiso.VER_GANANCIA, Permiso.EDITAR_CATALOGO,
            Permiso.MOVIMIENTOS, Permiso.ANULAR, Permiso.DESHABILITAR, Permiso.INFORMES, Permiso.COSTOS, Permiso.USUARIOS,
            Permiso.HISTORIAL)),
    CAJERO("Cajero", EnumSet.of(Permiso.VENDER));

    /** El cajero puede anular su propia venta durante estos minutos. */
    public static final int MINUTOS_ANULAR_PROPIA = 10;

    private final String etiqueta;
    private final Set<Permiso> permisos;

    Rol(String etiqueta, Set<Permiso> permisos) {
        this.etiqueta = etiqueta;
        this.permisos = permisos;
    }

    /** @return nombre para mostrar ("Dueño", "Administrador", "Cajero") */
    public String getEtiqueta() {
        return etiqueta;
    }

    /** @return permisos del rol */
    public Set<Permiso> getPermisos() {
        return EnumSet.copyOf(permisos);
    }

    /**
     * @param permiso acción a revisar
     * @return {@code true} si el rol la puede hacer
     */
    public boolean puede(Permiso permiso) {
        return permisos.contains(permiso);
    }

    /**
     * @param otro rol que se quiere asignar
     * @return {@code true} si este rol puede crear o administrar usuarios con {@code otro}
     */
    public boolean puedeAsignar(Rol otro) {
        return this == DUENO || (this == ADMINISTRADOR && otro == CAJERO);
    }

    /**
     * @param texto nombre del rol ({@code DUENO}, {@code ADMINISTRADOR}, {@code CAJERO})
     * @return el rol
     * @throws sistemainventario.exception.ValidationException si no existe
     */
    public static Rol desde(String texto) {
        try {
            return Rol.valueOf(String.valueOf(texto).trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new sistemainventario.exception.ValidationException("Rol desconocido: " + texto);
        }
    }
}
