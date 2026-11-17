package sistemainventario.view;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Rectangle2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Transform;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import sistemainventario.bridge.ScreenTransition;

/**
 * Hace las transiciones grandes de la interfaz con animaciones de JavaFX
 * (aceleradas por la tarjeta gráfica) en lugar de animaciones CSS dentro del
 * {@link WebView}.
 *
 * <p>Toma una imagen de la página, recorta la zona pedida, la coloca encima en
 * el mismo lugar y la desvanece con un desenfoque creciente. Mientras tanto la
 * página ya muestra el contenido nuevo debajo, así que se ve un fundido suave
 * entre lo anterior y lo nuevo.</p>
 */
public class SnapshotTransition implements ScreenTransition {

    /** Desenfoque final de la imagen que se va, en píxeles. */
    private static final double DESENFOQUE_FINAL = 14;
    /** La imagen que se va sube un poco mientras desaparece. */
    private static final double DESPLAZAMIENTO_FINAL = -6;
    private static final int DURACION_MAXIMA = 1200;

    private final StackPane raiz;
    private final WebView vista;

    /**
     * @param raiz  contenedor de la ventana donde se coloca la imagen temporal
     * @param vista visor web del que se toma la imagen
     */
    public SnapshotTransition(StackPane raiz, WebView vista) {
        this.raiz = raiz;
        this.vista = vista;
    }

    @Override
    public void desvanecerZona(double x, double y, double ancho, double alto, int milisegundos) {
        if (ancho <= 0 || alto <= 0 || vista.getScene() == null || vista.getScene().getWindow() == null) {
            return;
        }
        int duracion = Math.max(80, Math.min(milisegundos, DURACION_MAXIMA));

        // La imagen se toma a la escala real de la pantalla (125 %, 150 %...) para que no se vea borrosa.
        double escala = vista.getScene().getWindow().getRenderScaleX();
        SnapshotParameters parametros = new SnapshotParameters();
        parametros.setTransform(Transform.scale(escala, escala));
        WritableImage foto = vista.snapshot(parametros, null);

        ImageView capa = new ImageView(foto);
        capa.setViewport(new Rectangle2D(x * escala, y * escala, ancho * escala, alto * escala));
        capa.setFitWidth(ancho);
        capa.setFitHeight(alto);
        capa.setSmooth(true);
        capa.setManaged(false);
        capa.setMouseTransparent(true);
        capa.relocate(x, y);
        GaussianBlur desenfoque = new GaussianBlur(0);
        capa.setEffect(desenfoque);
        raiz.getChildren().add(capa);

        Interpolator suave = Interpolator.SPLINE(0.4, 0.0, 0.2, 1.0);
        Timeline animacion = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(capa.opacityProperty(), 1.0),
                        new KeyValue(desenfoque.radiusProperty(), 0.0),
                        new KeyValue(capa.translateYProperty(), 0.0)),
                new KeyFrame(Duration.millis(duracion),
                        new KeyValue(capa.opacityProperty(), 0.0, Interpolator.EASE_IN),
                        new KeyValue(desenfoque.radiusProperty(), DESENFOQUE_FINAL, suave),
                        new KeyValue(capa.translateYProperty(), DESPLAZAMIENTO_FINAL, suave)));
        animacion.setOnFinished(evento -> raiz.getChildren().remove(capa));
        animacion.play();
    }
}
