package sistemainventario.repository;

import sistemainventario.exception.PersistenceException;
import sistemainventario.exception.ValidationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Imagen del QR de cobro (Bre-B) de cada negocio, en
 * {@code ~/.inventariopro/qr-cobro/<negocio>.img}. El QR lo genera el banco del
 * negocio; TBalance solo guarda la imagen para mostrársela al cliente al cobrar.
 */
public class PaymentQrRepository {

    /** Tamaño máximo de la imagen (2 MB). */
    public static final int TAMANO_MAXIMO = 2 * 1024 * 1024;

    private final Path carpeta;

    /** @param carpeta carpeta donde se guardan las imágenes */
    public PaymentQrRepository(Path carpeta) {
        this.carpeta = carpeta;
    }

    /**
     * @param perfilId negocio
     * @param imagen   bytes de una imagen PNG o JPEG
     * @throws ValidationException si no es PNG ni JPEG o pasa de 2 MB
     */
    public void guardar(String perfilId, byte[] imagen) {
        if (imagen == null || imagen.length == 0 || imagen.length > TAMANO_MAXIMO) {
            throw new ValidationException("La imagen del QR debe pesar menos de 2 MB");
        }
        if (tipo(imagen).isEmpty()) {
            throw new ValidationException("Suba la imagen del QR en PNG o JPG");
        }
        try {
            Files.createDirectories(carpeta);
            Files.write(archivo(perfilId), imagen);
        } catch (IOException e) {
            throw new PersistenceException("No se pudo guardar la imagen del QR", e);
        }
    }

    /**
     * @param perfilId negocio
     * @return la imagen como "data:image/...;base64,...", si el negocio subió una
     */
    public Optional<String> leerComoDataUrl(String perfilId) {
        Path ruta = archivo(perfilId);
        if (!Files.isRegularFile(ruta)) {
            return Optional.empty();
        }
        try {
            byte[] bytes = Files.readAllBytes(ruta);
            return tipo(bytes).map(t -> "data:" + t + ";base64," + java.util.Base64.getEncoder().encodeToString(bytes));
        } catch (IOException e) {
            throw new PersistenceException("No se pudo leer la imagen del QR", e);
        }
    }

    /** @param perfilId negocio cuyo QR se quita */
    public void eliminar(String perfilId) {
        try {
            Files.deleteIfExists(archivo(perfilId));
        } catch (IOException e) {
            throw new PersistenceException("No se pudo quitar la imagen del QR", e);
        }
    }

    private Path archivo(String perfilId) {
        if (perfilId == null || !perfilId.matches("[A-Za-z0-9_-]{1,30}")) {
            throw new ValidationException("Negocio no válido");
        }
        return carpeta.resolve(perfilId + ".img");
    }

    private static Optional<String> tipo(byte[] b) {
        if (b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return Optional.of("image/png");
        }
        if (b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8) {
            return Optional.of("image/jpeg");
        }
        return Optional.empty();
    }
}
