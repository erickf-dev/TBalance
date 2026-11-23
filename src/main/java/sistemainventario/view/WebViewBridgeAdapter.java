package sistemainventario.view;

import sistemainventario.bridge.BridgeInvoker;

/**
 * Puente que ve la página en el visor de JavaFX.
 *
 * <p>Antes la página llamaba los métodos de {@link sistemainventario.bridge.JavaBridge}
 * directamente. Ahora pasa por {@link BridgeInvoker}, igual que en Edge, para que
 * la sesión y los permisos se revisen en los dos modos de ventana.</p>
 */
public class WebViewBridgeAdapter {

    /** Crea {@code window.javaBridge} con la misma forma que en Edge. */
    static final String SCRIPT_PUENTE = """
            window.javaBridge = new Proxy({}, {
              get: function (_, nombre) {
                if (typeof nombre !== 'string' || nombre === 'then' || nombre === 'toJSON') return undefined;
                return function () {
                  var r = JSON.parse(window.__tbPuente.llamar(nombre, JSON.stringify(Array.prototype.slice.call(arguments))));
                  if (!r.ok) throw new Error(r.error);
                  return r.v === null ? undefined : r.v;
                };
              }
            });
            """;

    private final BridgeInvoker invocador;

    /** @param invocador ejecuta los métodos del puente revisando permisos */
    public WebViewBridgeAdapter(BridgeInvoker invocador) {
        this.invocador = invocador;
    }

    /**
     * @param metodo        nombre del método
     * @param argumentosJson argumentos en un arreglo JSON
     * @return respuesta JSON {@code {ok, v}} o {@code {ok:false, error}}
     */
    public String llamar(String metodo, String argumentosJson) {
        return invocador.invocar(metodo, argumentosJson);
    }
}
