package sistemainventario.ia;

import sistemainventario.util.AssistantLog;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Prepara el cliente HTTP del asistente para que se conecte como lo hace Edge
 * en el mismo computador.
 *
 * <ul>
 *   <li><b>Proxy:</b> usa el proxy configurado en Windows (redes de
 *       universidades o empresas). Java, por defecto, no lo usa.</li>
 *   <li><b>Certificados:</b> confía en los certificados de Java y también en los
 *       de Windows. Algunos antivirus revisan las conexiones seguras con un
 *       certificado propio que solo está instalado en Windows; sin esto Java
 *       rechaza la conexión aunque Edge funcione.</li>
 * </ul>
 */
final class SecureConnection {

    private SecureConnection() {
    }

    /** @return un cliente HTTP con el proxy del sistema y los certificados de Java y de Windows */
    static HttpClient crearCliente() {
        System.setProperty("java.net.useSystemProxies", "true");
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .proxy(ProxySelector.getDefault());
        SSLContext contexto = contextoConCertificadosDelSistema();
        if (contexto != null) {
            builder.sslContext(contexto);
        }
        return builder.build();
    }

    /** @return un contexto TLS que acepta lo que acepte Java o Windows; {@code null} si no se pudo crear */
    private static SSLContext contextoConCertificadosDelSistema() {
        try {
            List<X509TrustManager> gestores = new ArrayList<>();
            gestores.add(gestorDe(null));
            if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                KeyStore windows = KeyStore.getInstance("Windows-ROOT");
                windows.load(null, null);
                gestores.add(gestorDe(windows));
            }
            SSLContext contexto = SSLContext.getInstance("TLS");
            contexto.init(null, new TrustManager[] {new GestorCompuesto(gestores)}, null);
            return contexto;
        } catch (Exception e) {
            AssistantLog.registrar("No se pudieron cargar los certificados de Windows: " + e);
            return null;
        }
    }

    private static X509TrustManager gestorDe(KeyStore almacen) throws Exception {
        TrustManagerFactory fabrica = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        fabrica.init(almacen);
        for (TrustManager gestor : fabrica.getTrustManagers()) {
            if (gestor instanceof X509TrustManager) {
                return (X509TrustManager) gestor;
            }
        }
        throw new IllegalStateException("No hay gestor de certificados X509");
    }

    /** Acepta un certificado si cualquiera de los gestores lo acepta. */
    private static final class GestorCompuesto implements X509TrustManager {
        private final List<X509TrustManager> gestores;

        private GestorCompuesto(List<X509TrustManager> gestores) {
            this.gestores = gestores;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] cadena, String tipo) throws CertificateException {
            gestores.get(0).checkClientTrusted(cadena, tipo);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] cadena, String tipo) throws CertificateException {
            CertificateException ultimo = null;
            for (X509TrustManager gestor : gestores) {
                try {
                    gestor.checkServerTrusted(cadena, tipo);
                    return;
                } catch (CertificateException e) {
                    ultimo = e;
                }
            }
            throw ultimo != null ? ultimo : new CertificateException("Certificado no confiable");
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            List<X509Certificate> todos = new ArrayList<>();
            for (X509TrustManager gestor : gestores) {
                todos.addAll(List.of(gestor.getAcceptedIssuers()));
            }
            return todos.toArray(new X509Certificate[0]);
        }
    }
}
