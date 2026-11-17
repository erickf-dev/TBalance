package sistemainventario.bridge;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementación de {@link ViewNotifier} para cuando la interfaz se muestra en
 * Edge: envía cada script a la página por un flujo de eventos del servidor
 * (Server-Sent Events) y la página lo ejecuta al recibirlo.
 *
 * <p>Cumple el mismo contrato que {@link WebViewNotifier}: los controladores no
 * saben cuál de las dos vistas está activa (<b>DIP</b>, <b>OCP</b>).</p>
 */
public class SseNotifier implements ViewNotifier {

    /** Conexiones abiertas de la página (normalmente una; dos un instante si se recarga). */
    private final List<OutputStream> conexiones = new CopyOnWriteArrayList<>();

    /**
     * Registra una conexión nueva de la página.
     *
     * @param salida flujo de respuesta HTTP que queda abierto
     */
    public void agregar(OutputStream salida) {
        conexiones.add(salida);
    }

    /** @param salida conexión que se cerró */
    public void quitar(OutputStream salida) {
        conexiones.remove(salida);
    }

    /** @return cuántas páginas están conectadas en este momento */
    public int conectadas() {
        return conexiones.size();
    }

    @Override
    public void ejecutarScript(String script) {
        enviar(formatear(script));
    }

    /** Envía un comentario vacío para mantener viva la conexión y detectar si se cerró. */
    public void latido() {
        enviar(": latido\n\n");
    }

    /**
     * Da formato de evento al script: cada línea va precedida de {@code data: }.
     *
     * @param script código JavaScript
     * @return texto del evento listo para enviar
     */
    static String formatear(String script) {
        StringBuilder evento = new StringBuilder();
        for (String linea : script.split("\n", -1)) {
            evento.append("data: ").append(linea.replace("\r", "")).append('\n');
        }
        return evento.append('\n').toString();
    }

    private void enviar(String texto) {
        byte[] datos = texto.getBytes(StandardCharsets.UTF_8);
        for (OutputStream salida : conexiones) {
            synchronized (salida) {
                try {
                    salida.write(datos);
                    salida.flush();
                } catch (IOException e) {
                    conexiones.remove(salida);
                }
            }
        }
    }
}
