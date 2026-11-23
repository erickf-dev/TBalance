package sistemainventario.model;

/**
 * Una acción que alguien hizo en TBalance: quién, cuándo, en qué negocio y qué hizo.
 * El historial solo crece: no se edita ni se borra desde la aplicación.
 */
public class AuditEntry extends IdentifiedEntity {

    private String fecha;
    private String usuarioId;
    private String usuario;
    private String rol;
    private String perfilId;
    private String perfil;
    private String accion;
    private String detalle;

    public AuditEntry() {
    }

    /**
     * @param fecha     cuándo pasó ({@code yyyy-MM-dd'T'HH:mm:ss})
     * @param usuarioId quién lo hizo ({@code null} si fue un intento de ingreso sin usuario válido)
     * @param usuario   nombre de quien lo hizo
     * @param rol       su rol en ese negocio
     * @param perfilId  negocio donde pasó ({@code null} si no fue en un negocio)
     * @param perfil    nombre del negocio
     * @param accion    qué hizo, en pocas palabras
     * @param detalle   datos de la acción
     */
    public AuditEntry(String fecha, String usuarioId, String usuario, String rol, String perfilId, String perfil,
                      String accion, String detalle) {
        this.fecha = fecha;
        this.usuarioId = usuarioId;
        this.usuario = usuario;
        this.rol = rol;
        this.perfilId = perfilId;
        this.perfil = perfil;
        this.accion = accion;
        this.detalle = detalle;
    }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    public String getPerfilId() { return perfilId; }
    public void setPerfilId(String perfilId) { this.perfilId = perfilId; }
    public String getPerfil() { return perfil; }
    public void setPerfil(String perfil) { this.perfil = perfil; }
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
}
