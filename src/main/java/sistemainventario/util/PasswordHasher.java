package sistemainventario.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Resume contraseñas con PBKDF2-HMAC-SHA256, que ya trae Java.
 *
 * <p>Cada contraseña lleva una sal aleatoria distinta y se repite el cálculo
 * muchas veces ({@value #ITERACIONES}), así un resumen robado no sirve para
 * adivinar la contraseña. Se guarda como
 * {@code pbkdf2_sha256$iteraciones$sal$resumen}; las iteraciones van en el texto
 * para poder subirlas en el futuro sin invalidar las contraseñas guardadas.</p>
 */
public final class PasswordHasher {

    private static final String PREFIJO = "pbkdf2_sha256";
    /** Repeticiones del cálculo (recomendación de OWASP para PBKDF2-HMAC-SHA256). */
    public static final int ITERACIONES = 600_000;
    private static final int BYTES_SAL = 16;
    private static final int BITS_RESUMEN = 256;
    private static final SecureRandom AZAR = new SecureRandom();

    private PasswordHasher() {
    }

    /**
     * @param clave contraseña escrita por la persona
     * @return texto para guardar (nunca la contraseña misma)
     */
    public static String resumir(String clave) {
        byte[] sal = new byte[BYTES_SAL];
        AZAR.nextBytes(sal);
        byte[] resumen = calcular(clave, sal, ITERACIONES);
        Base64.Encoder b64 = Base64.getEncoder().withoutPadding();
        return PREFIJO + "$" + ITERACIONES + "$" + b64.encodeToString(sal) + "$" + b64.encodeToString(resumen);
    }

    /**
     * @param clave   contraseña escrita al iniciar sesión
     * @param guardado texto de {@link #resumir(String)}
     * @return {@code true} si coinciden (la comparación tarda lo mismo aunque no coincidan)
     */
    public static boolean coincide(String clave, String guardado) {
        if (clave == null || guardado == null) {
            return false;
        }
        String[] partes = guardado.split("\\$");
        if (partes.length != 4 || !PREFIJO.equals(partes[0])) {
            return false;
        }
        try {
            int iteraciones = Integer.parseInt(partes[1]);
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            return MessageDigest.isEqual(esperado, calcular(clave, sal, iteraciones));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] calcular(String clave, byte[] sal, int iteraciones) {
        PBEKeySpec datos = new PBEKeySpec(clave.toCharArray(), sal, iteraciones, BITS_RESUMEN);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(datos).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Este Java no tiene PBKDF2WithHmacSHA256", e);
        } finally {
            datos.clearPassword();
        }
    }
}
