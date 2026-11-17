package sistemainventario;

import javafx.application.Application;
import javafx.stage.Stage;
import sistemainventario.view.DesktopBrowserApp;
import sistemainventario.view.WebViewMain;

/**
 * Aplicación del Sistema de Inventario TBalance.
 *
 * <p>La interfaz se abre en Edge en modo aplicación ({@link DesktopBrowserApp});
 * si no está disponible, en el visor de JavaFX ({@link WebViewMain}).</p>
 *
 * <p>Para ejecutar desde IntelliJ IDEA use {@link Launcher} (evita el error
 * "JavaFX runtime components are missing"). Con Maven: {@code mvn javafx:run}.</p>
 */
public class Main extends Application {

    @Override
    public void start(Stage ventanaPrincipal) {
        new WebViewMain(AppContext.get()).show(ventanaPrincipal);
    }

    /**
     * Abre la interfaz en una ventana de Edge (fluida, dibujada con la tarjeta
     * gráfica); si no hay Edge ni Chrome, usa el visor de JavaFX.
     */
    public static void main(String[] args) {
        if (DesktopBrowserApp.iniciar(AppContext.get())) {
            return;
        }
        launch(args);
    }
}
