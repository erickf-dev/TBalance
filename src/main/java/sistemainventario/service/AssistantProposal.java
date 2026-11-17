package sistemainventario.service;

import java.util.List;

/**
 * Cambio que el asistente propone y que solo se hace si el usuario lo confirma
 * (registrar una venta, una entrada de mercancía, cambiar un precio...).
 *
 * <p>La acción se guarda como {@link Runnable} y no se envía a la vista; la
 * vista recibe el título, las líneas y el total para mostrar la tarjeta.</p>
 */
public final class AssistantProposal {

    private final String id;
    private final String tipo;
    private final String titulo;
    private final List<String> lineas;
    private final Double total;
    private final transient Runnable accion;

    /**
     * @param id     identificador para confirmarla o descartarla
     * @param tipo   venta, entrada, salida, precio o producto
     * @param titulo resumen corto
     * @param lineas detalle (una línea por producto)
     * @param total  valor total, o {@code null} si no aplica
     * @param accion lo que se hace al confirmar
     */
    public AssistantProposal(String id, String tipo, String titulo, List<String> lineas, Double total, Runnable accion) {
        this.id = id;
        this.tipo = tipo;
        this.titulo = titulo;
        this.lineas = List.copyOf(lineas);
        this.total = total;
        this.accion = accion;
    }

    /** Hace el cambio propuesto. */
    public void ejecutar() {
        accion.run();
    }

    public String getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public List<String> getLineas() {
        return lineas;
    }

    public Double getTotal() {
        return total;
    }
}
