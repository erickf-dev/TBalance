package sistemainventario.dispositivo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import sistemainventario.exception.ValidationException;

import javax.imageio.ImageIO;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Conexión con el celular por el Wi-Fi del local, para usarlo como lector de
 * etiquetas sin instalar aplicaciones ni usar internet.
 *
 * <p>Mientras la cámara del celular está encendida, TBalance abre un pequeño
 * servidor HTTPS en la red local. El celular abre la página {@code celular.html}
 * en su navegador (escaneando el QR que muestra el PC), toma fotos de la cámara
 * y las envía; el PC las lee con el mismo escáner de la webcam. La respuesta de
 * cada foto lleva los avisos para el celular (producto agregado, etiqueta de
 * otro negocio...).</p>
 *
 * <p><b>Seguridad:</b> el servidor existe solo mientras se usa (se cierra al
 * apagar la cámara), exige una clave aleatoria que cambia cada vez y solo va en
 * el QR, y no da acceso a los datos: solo recibe fotos. Usa HTTPS porque los
 * navegadores solo permiten la cámara en páginas seguras; el certificado lo
 * crea TBalance en este computador (por eso el celular avisa la primera vez).</p>
 */
public class PhoneLink {

    /** Puerto preferido; si está ocupado se usa otro libre. */
    private static final int PUERTO_PREFERIDO = 8765;
    /** Tamaño máximo de una foto (las del celular pesan unos 60 KB). */
    private static final int MAXIMO_BYTES_FOTO = 3 * 1024 * 1024;
    /** Sin fotos durante este tiempo, el celular se considera desconectado. */
    private static final long DESCONECTADO_MS = 4000;
    private static final String ALIAS = "tbalance-celular";

    private final Path carpeta;
    private HttpsServer servidor;
    private String clave;
    private String direccion;
    private byte[] ultimaFoto;
    private boolean fotoNueva;
    private long ultimaFotoMs;
    private final List<String> avisos = new ArrayList<>();

    /** @param carpeta donde se guarda el certificado ({@code ~/.inventariopro/celular}) */
    public PhoneLink(Path carpeta) {
        this.carpeta = carpeta;
    }

    /**
     * Abre el servidor si no estaba abierto.
     *
     * @return dirección que debe abrir el celular (va en el QR)
     * @throws ValidationException si no hay red local o no se pudo abrir el servidor
     */
    public synchronized String iniciar() {
        if (servidor != null) {
            return direccion;
        }
        String ip = direccionLocal().orElseThrow(() -> new ValidationException(
                "El computador no está conectado a una red Wi-Fi o de cable. Conéctelo a la misma red del celular"));
        try {
            SSLContext tls = contextoTls(ip);
            servidor = abrirPuerto();
            servidor.setHttpsConfigurator(new HttpsConfigurator(tls));
            servidor.createContext("/", this::atender);
            byte[] azar = new byte[10];
            new SecureRandom().nextBytes(azar);
            synchronized (this) {
                // 80 bits al azar: suficiente para una conexión que dura minutos y deja el QR
                // poco denso, así hasta un celular sencillo lo lee rápido de la pantalla.
                clave = HexFormat.of().formatHex(azar);
            }
            servidor.setExecutor(Executors.newSingleThreadExecutor(r -> {
                Thread hilo = new Thread(r, "celular-camara");
                hilo.setDaemon(true);
                return hilo;
            }));
            servidor.start();
        } catch (IOException | GeneralSecurityException e) {
            detener();
            throw new ValidationException("No se pudo preparar la conexión con el celular: " + e.getMessage());
        }
        direccion = "https://" + ip + ":" + servidor.getAddress().getPort() + "/c?t=" + clave;
        ultimaFoto = null;
        ultimaFotoMs = 0;
        avisos.clear();
        return direccion;
    }

    /** Cierra el servidor; la dirección anterior deja de servir. */
    public synchronized void detener() {
        if (servidor != null) {
            servidor.stop(0);
            if (servidor.getExecutor() instanceof java.util.concurrent.ExecutorService hilos) {
                hilos.shutdownNow();
            }
        }
        servidor = null;
        clave = null;
        direccion = null;
        ultimaFoto = null;
    }

    /**
     * Crea el certificado en segundo plano si todavía no existe, para que la
     * primera conexión con el celular no tenga que esperarlo.
     */
    public void prepararCertificado() {
        Thread hilo = new Thread(() -> {
            try {
                synchronized (this) {
                    Path almacen = carpeta.resolve("certificado.p12");
                    Path archivoClave = carpeta.resolve("certificado.clave");
                    if (!Files.exists(almacen) || !Files.exists(archivoClave)) {
                        crearCertificado(almacen, archivoClave, direccionLocal().orElse("127.0.0.1"));
                    }
                }
            } catch (IOException | RuntimeException e) {
                // se vuelve a intentar al conectar el celular, donde el error sí se muestra
            }
        }, "certificado-celular");
        hilo.setDaemon(true);
        hilo.start();
    }

    /** @return {@code true} si el servidor está abierto */
    public synchronized boolean estaActivo() {
        return servidor != null;
    }

    /** @return {@code true} si el celular envió una foto hace poco */
    public synchronized boolean celularConectado() {
        return ultimaFotoMs > 0 && System.currentTimeMillis() - ultimaFotoMs < DESCONECTADO_MS;
    }

    /**
     * @return la foto más reciente si no se había entregado antes, o {@code null}
     */
    public BufferedImage tomarFotoNueva() {
        byte[] datos;
        synchronized (this) {
            if (!fotoNueva || ultimaFoto == null) {
                return null;
            }
            fotoNueva = false;
            datos = ultimaFoto;
        }
        try {
            return ImageIO.read(new ByteArrayInputStream(datos));
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Deja un aviso para el celular; se entrega con la respuesta a la próxima foto.
     *
     * @param mensaje  texto
     * @param correcto {@code true} si fue un producto agregado
     */
    public synchronized void avisar(String mensaje, boolean correcto) {
        if (avisos.size() < 10) {
            avisos.add("{\"texto\":" + textoJson(mensaje) + ",\"ok\":" + correcto + "}");
        }
    }

    private void atender(HttpExchange intercambio) throws IOException {
        try (intercambio) {
            String ruta = intercambio.getRequestURI().getPath();
            String consulta = intercambio.getRequestURI().getRawQuery();
            boolean claveCorrecta;
            synchronized (this) {
                claveCorrecta = clave != null && consulta != null && consulta.equals("t=" + clave);
            }
            if (!claveCorrecta) {
                responder(intercambio, 403, "text/plain; charset=utf-8",
                        "Este enlace ya no sirve. Vuelva a escanear el código que muestra TBalance."
                                .getBytes(StandardCharsets.UTF_8));
            } else if ("GET".equals(intercambio.getRequestMethod()) && ("/c".equals(ruta) || "/celular.html".equals(ruta))) {
                responder(intercambio, 200, "text/html; charset=utf-8", recurso("/html/celular.html"));
            } else if ("POST".equals(intercambio.getRequestMethod()) && "/foto".equals(ruta)) {
                recibirFoto(intercambio);
            } else {
                responder(intercambio, 404, "text/plain; charset=utf-8", new byte[0]);
            }
        }
    }

    private void recibirFoto(HttpExchange intercambio) throws IOException {
        byte[] datos;
        try (InputStream entrada = intercambio.getRequestBody()) {
            datos = entrada.readNBytes(MAXIMO_BYTES_FOTO + 1);
        }
        if (datos.length == 0 || datos.length > MAXIMO_BYTES_FOTO) {
            responder(intercambio, 413, "text/plain; charset=utf-8", new byte[0]);
            return;
        }
        String respuesta;
        synchronized (this) {
            ultimaFoto = datos;
            fotoNueva = true;
            ultimaFotoMs = System.currentTimeMillis();
            respuesta = "{\"avisos\":[" + String.join(",", avisos) + "]}";
            avisos.clear();
        }
        responder(intercambio, 200, "application/json; charset=utf-8", respuesta.getBytes(StandardCharsets.UTF_8));
    }

    private static HttpsServer abrirPuerto() throws IOException {
        try {
            return HttpsServer.create(new InetSocketAddress(PUERTO_PREFERIDO), 0);
        } catch (BindException ocupado) {
            return HttpsServer.create(new InetSocketAddress(0), 0);
        }
    }

    /**
     * Certificado propio de este computador, creado una sola vez con la
     * herramienta {@code keytool} que trae Java.
     */
    private SSLContext contextoTls(String ip) throws IOException, GeneralSecurityException {
        Path almacen = carpeta.resolve("certificado.p12");
        Path archivoClave = carpeta.resolve("certificado.clave");
        if (!Files.exists(almacen) || !Files.exists(archivoClave)) {
            crearCertificado(almacen, archivoClave, ip);
        }
        char[] claveAlmacen = Files.readString(archivoClave, StandardCharsets.UTF_8).trim().toCharArray();
        KeyStore llaves = KeyStore.getInstance("PKCS12");
        try (InputStream entrada = Files.newInputStream(almacen)) {
            llaves.load(entrada, claveAlmacen);
        }
        KeyManagerFactory fabrica = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        fabrica.init(llaves, claveAlmacen);
        SSLContext tls = SSLContext.getInstance("TLS");
        tls.init(fabrica.getKeyManagers(), null, new SecureRandom());
        return tls;
    }

    private void crearCertificado(Path almacen, Path archivoClave, String ip) throws IOException {
        Files.createDirectories(carpeta);
        Files.deleteIfExists(almacen);
        byte[] azar = new byte[18];
        new SecureRandom().nextBytes(azar);
        String claveNueva = HexFormat.of().formatHex(azar);
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        Path keytool = Path.of(System.getProperty("java.home"), "bin", windows ? "keytool.exe" : "keytool");
        if (!Files.exists(keytool)) {
            throw new IOException("falta la herramienta keytool de Java (" + keytool + ")");
        }
        List<String> orden = List.of(keytool.toString(), "-genkeypair", "-alias", ALIAS,
                // Curva elíptica P-256: se crea al instante y la conexión segura con el celular es más rápida que con RSA
                "-keyalg", "EC", "-groupname", "secp256r1", "-validity", "3650",
                "-dname", "CN=TBalance", "-ext", "SAN=ip:" + ip + ",dns:localhost",
                "-storetype", "PKCS12", "-keystore", almacen.toString(),
                "-storepass", claveNueva, "-keypass", claveNueva);
        Process proceso = new ProcessBuilder(orden).redirectErrorStream(true).start();
        String salida = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        try {
            if (!proceso.waitFor(60, TimeUnit.SECONDS) || proceso.exitValue() != 0 || !Files.exists(almacen)) {
                throw new IOException("keytool no pudo crear el certificado: " + salida.trim());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("se interrumpió la creación del certificado");
        }
        Files.writeString(archivoClave, claveNueva, StandardCharsets.UTF_8);
    }

    /**
     * Dirección IPv4 del computador en la red local (Wi-Fi o cable), sin contar
     * adaptadores virtuales (VirtualBox, VMware, Hyper-V, WSL, Docker).
     *
     * @return la dirección, si el computador está en una red
     */
    public static java.util.Optional<String> direccionLocal() {
        List<String> candidatas = new ArrayList<>();
        List<String> otras = new ArrayList<>();
        try {
            for (NetworkInterface red : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                String nombre = (red.getName() + " " + red.getDisplayName()).toLowerCase(Locale.ROOT);
                if (!red.isUp() || red.isLoopback() || red.isVirtual() || red.isPointToPoint()
                        || nombre.matches(".*(virtual|vmware|vbox|hyper-v|vethernet|wsl|docker|tap|tun|bluetooth).*")) {
                    continue;
                }
                for (InetAddress direccion : Collections.list(red.getInetAddresses())) {
                    if (direccion instanceof Inet4Address && !direccion.isLinkLocalAddress()) {
                        otras.add(direccion.getHostAddress());
                        if (direccion.isSiteLocalAddress()) {
                            String ip = direccion.getHostAddress();
                            // las redes de casa y de negocio suelen ser 192.168.x.x: van primero
                            if (ip.startsWith("192.168.")) {
                                candidatas.add(0, ip);
                            } else {
                                candidatas.add(ip);
                            }
                        }
                    }
                }
            }
        } catch (SocketException e) {
            return java.util.Optional.empty();
        }
        // Si no hay una dirección de red privada (raro), se usa la que haya.
        return candidatas.stream().findFirst().or(() -> otras.stream().findFirst());
    }

    private static byte[] recurso(String ruta) throws IOException {
        try (InputStream entrada = PhoneLink.class.getResourceAsStream(ruta)) {
            if (entrada == null) {
                throw new IOException("No se encontró " + ruta);
            }
            return entrada.readAllBytes();
        }
    }

    private static void responder(HttpExchange intercambio, int codigo, String tipo, byte[] cuerpo) throws IOException {
        intercambio.getResponseHeaders().set("Content-Type", tipo);
        intercambio.getResponseHeaders().set("Cache-Control", "no-store");
        intercambio.sendResponseHeaders(codigo, cuerpo.length == 0 ? -1 : cuerpo.length);
        if (cuerpo.length > 0) {
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(cuerpo);
            }
        }
    }

    private static String textoJson(String texto) {
        StringBuilder json = new StringBuilder("\"");
        for (char c : String.valueOf(texto).toCharArray()) {
            switch (c) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (c < 0x20) {
                        json.append(String.format("\\u%04x", (int) c));
                    } else {
                        json.append(c);
                    }
                }
            }
        }
        return json.append('"').toString();
    }
}
