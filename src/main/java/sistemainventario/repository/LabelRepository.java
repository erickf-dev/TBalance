package sistemainventario.repository;

import sistemainventario.exception.PersistenceException;
import sistemainventario.exception.ValidationException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Guarda las etiquetas generadas como imágenes PNG en
 * {@code ~/.inventariopro/etiquetas/} y las convierte a Base64 para
 * mostrarlas en la vista.
 *
 * <p><b>SRP:</b> solo maneja archivos de imagen; el dibujo de las etiquetas lo
 * hace el servicio.</p>
 */
public class LabelRepository {

    private static final Pattern NOMBRE_VALIDO = Pattern.compile("[A-Za-z0-9_\\-]+\\.png");
    private static final String FORMATO = "png";

    private final Path carpeta;

    /**
     * @param carpeta carpeta donde se guardan las etiquetas (se crea si no existe)
     */
    public LabelRepository(Path carpeta) {
        this.carpeta = carpeta;
        JsonFileStore.crearDirectorio(carpeta);
    }

    /**
     * @param nombre nombre del archivo, por ejemplo {@code etiqueta_1001.png}
     * @param imagen imagen a guardar
     * @return ruta del archivo guardado
     * @throws ValidationException   si el nombre no es válido
     * @throws PersistenceException si no se puede escribir
     */
    public Path guardar(String nombre, BufferedImage imagen) {
        if (nombre == null || !NOMBRE_VALIDO.matcher(nombre).matches()) {
            throw new ValidationException("Nombre de etiqueta no valido");
        }
        Path destino = carpeta.resolve(nombre);
        try {
            ImageIO.write(imagen, FORMATO, destino.toFile());
            return destino;
        } catch (IOException e) {
            throw new PersistenceException("No se pudo guardar la etiqueta " + nombre, e);
        }
    }

    /**
     * @param imagen imagen a convertir
     * @return la imagen como {@code data:image/png;base64,...} para usarla en un {@code <img>}
     */
    public String aDataUri(BufferedImage imagen) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            ImageIO.write(imagen, FORMATO, bytes);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException e) {
            throw new PersistenceException("No se pudo convertir la etiqueta a imagen", e);
        }
    }

    public Path getCarpeta() {
        return carpeta;
    }
}
