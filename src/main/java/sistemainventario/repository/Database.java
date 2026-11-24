package sistemainventario.repository;

import sistemainventario.exception.PersistenceException;
import sistemainventario.exception.ValidationException;
import sistemainventario.model.DatabaseSettings;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Conexión JDBC con Oracle.
 *
 * <p>Java no escribe consultas: para leer llama a funciones de Oracle que
 * devuelven un cursor ({@link #llamar(String, LectorDeFila, Object...)}) o un
 * número ({@link #llamarNumero(String, Object...)}), y para guardar llama a
 * procedimientos ({@link #procedimiento(Connection, String, Object...)}). Todo
 * eso está en los scripts de {@code src/main/resources/sql/oracle}.</p>
 *
 * <p>Las únicas sentencias SQL que quedan en Java son las que se usan antes de
 * que existan las funciones: ver si una tabla existe, en qué versión está la
 * base, anotar una versión nueva y revisar si alguna función quedó con
 * errores.</p>
 *
 * <p>Usa una sola conexión que se reabre si se cae. Todas las escrituras pasan
 * por {@link #transaccion(Trabajo)}: si algo falla (por ejemplo, un disparador
 * o un procedimiento rechaza el cambio) se hace ROLLBACK de todo.</p>
 */
public class Database implements AutoCloseable {

    /** Scripts que se ejecutan la primera vez, en orden (dejan el esquema en la versión 1). */
    private static final String[] SCRIPTS_INICIALES = {"01_tablas.sql", "02_disparadores.sql"};
    /** Cambios del esquema: la posición 0 lleva a la versión 2, la 1 a la 3, etc. */
    private static final List<List<String>> CAMBIOS = List.of(
            List.of("cambios/v2_borrado_logico.sql"),
            List.of("cambios/v3_vistas_informes.sql"),
            List.of("cambios/v4_usuarios.sql"),
            List.of("cambios/v5_historial.sql"),
            List.of("cambios/v6_abonos.sql"),
            List.of("cambios/v7_varios_equipos.sql"),
            // v8: procedimientos y funciones para vender, anular, abonar e informes
            List.of("04_funciones.sql", "05_procedimientos.sql"),
            // v9: todas las consultas y todos los guardados en Oracle
            List.of("07_funciones_consulta.sql", "08_procedimientos_guardar.sql"));
    /** Desde esta versión existen las funciones de consulta y los procedimientos para guardar. */
    private static final int VERSION_TODO_EN_ORACLE = 9;
    private static final String CARPETA = "sql/oracle/";
    private static final int SEGUNDOS_VALIDACION = 3;
    /** Si la conexión lleva más de esto sin usarse, se revisa antes de usarla. */
    private static final long VALIDAR_TRAS_MS = 20_000;
    /** Cada cuánto, como mínimo, se pregunta a la base si otro equipo cambió algo. */
    private static final long REVISAR_CAMBIOS_CADA_MS = 1_500;
    /** Fila de CAMBIO_VERSION que cuenta todos los cambios. */
    private static final String CLAVE_CAMBIOS = "TODO";
    /** Tipo JDBC de un cursor de Oracle (oracle.jdbc.OracleTypes.CURSOR). */
    private static final int CURSOR = -10;

    private final DatabaseSettings config;
    private final OracleDialect oracle = new OracleDialect();
    private Connection conexion;
    private long ultimoUso;
    /** {@code null} mientras no se sepa si la base ya tiene todo en Oracle (cambio v9). */
    private Boolean seguimiento;
    private long versionConocida = -1;
    private long generacion;
    private long ultimaRevision;

    /**
     * Valor nulo con su tipo SQL (algunos drivers lo necesitan).
     *
     * @param tipo constante de {@link Types}
     */
    public record Nulo(int tipo) {
    }

    /**
     * Contenido binario (por ejemplo, el .zip de una copia de seguridad).
     *
     * @param datos bytes
     */
    public record Binario(byte[] datos) {
    }

    /**
     * Trabajo que se ejecuta dentro de una transacción.
     *
     * @param <T> resultado
     */
    @FunctionalInterface
    public interface Trabajo<T> {
        /**
         * @param conexion conexión con la transacción abierta
         * @return resultado
         * @throws SQLException si falla Oracle
         */
        T ejecutar(Connection conexion) throws SQLException;
    }

    /**
     * Convierte una fila en un objeto.
     *
     * @param <T> objeto
     */
    @FunctionalInterface
    public interface LectorDeFila<T> {
        /**
         * @param fila fila actual
         * @return objeto leído
         * @throws SQLException si falla la lectura
         */
        T leer(ResultSet fila) throws SQLException;
    }

    /** @param config URL, usuario y clave de Oracle */
    public Database(DatabaseSettings config) {
        this.config = Objects.requireNonNull(config);
    }

    /** @return "Oracle" */
    public String nombreMotor() {
        return "Oracle";
    }

    /** Abre la conexión (lanza una excepción con un mensaje claro si no se puede). */
    public void probar() {
        conexion();
    }

    /** @return {@code true} si ya existen las tablas de TBalance */
    public boolean tieneTablas() {
        try {
            return oracle.tablaExiste(conexion(), "PERFIL");
        } catch (SQLException e) {
            throw traducir(e, "revisar las tablas");
        }
    }

    /**
     * @return versión del esquema: 0 sin tablas, 1 con las tablas iniciales y
     *         después la última anotada en ESQUEMA_VERSION
     */
    public synchronized int versionEsquema() {
        try {
            Connection c = conexion();
            if (!oracle.tablaExiste(c, "ESQUEMA_VERSION")) {
                return oracle.tablaExiste(c, "PERFIL") ? 1 : 0;
            }
            // lectura directa: puede que todavía no existan las funciones de consulta
            try (Statement consulta = c.createStatement();
                 ResultSet fila = consulta.executeQuery("SELECT MAX(VERSION) FROM ESQUEMA_VERSION")) {
                return fila.next() ? fila.getInt(1) : 1;
            }
        } catch (SQLException e) {
            throw traducir(e, "revisar la versión de las tablas");
        }
    }

    /** @return versión a la que este programa deja el esquema */
    public static int versionActual() {
        return 1 + CAMBIOS.size();
    }

    /**
     * Crea las tablas si no existen y aplica, en orden y una sola vez, los
     * cambios que falten (tablas nuevas, funciones y procedimientos).
     *
     * @return cuántos cambios se aplicaron
     */
    public int actualizarEsquema() {
        try {
            int version = versionEsquema();
            if (version == 0) {
                for (String archivo : SCRIPTS_INICIALES) {
                    ejecutarScript(archivo);
                }
                version = 1;
            }
            int aplicados = 0;
            for (int siguiente = version + 1; siguiente <= versionActual(); siguiente++) {
                for (String script : CAMBIOS.get(siguiente - 2)) {
                    ejecutarScript(script);
                }
                revisarRutinas();
                anotarVersion(siguiente);
                aplicados++;
            }
            seguimiento = null;
            return aplicados;
        } catch (PersistenceException e) {
            if (String.valueOf(e.getMessage()).contains("ORA-01031")) {
                throw new PersistenceException("Al usuario de Oracle le falta un permiso para crear las funciones y los "
                        + "procedimientos. En SQL Developer, conectado como SYSTEM, ejecute una vez: "
                        + "GRANT CREATE PROCEDURE TO " + config.usuario().toUpperCase(java.util.Locale.ROOT)
                        + ";  y vuelva a abrir TBalance.", e);
            }
            throw e;
        }
    }

    /**
     * Oracle crea una función o un procedimiento aunque tenga errores (queda
     * INVALID). Si pasa, se avisa con el error exacto en vez de fallar después.
     */
    private void revisarRutinas() {
        try {
            java.util.Optional<String> error = oracle.erroresDeRutinas(conexion());
            if (error.isPresent()) {
                throw new PersistenceException(error.get(), null);
            }
        } catch (SQLException e) {
            throw traducir(e, "revisar las funciones y los procedimientos");
        }
    }

    private void anotarVersion(int numero) {
        transaccion(c -> {
            try (PreparedStatement sentencia = c.prepareStatement("INSERT INTO ESQUEMA_VERSION (VERSION) VALUES (?)")) {
                sentencia.setInt(1, numero);
                return sentencia.executeUpdate();
            }
        });
    }

    /** @return {@code true} si no hay ningún negocio (FN_CONTAR_NEGOCIOS) */
    public boolean estaVacia() {
        return llamarNumero("FN_CONTAR_NEGOCIOS") == 0;
    }

    /** @return cuántos negocios hay, o -1 si la base todavía no tiene las funciones de consulta */
    public int contarNegocios() {
        return versionEsquema() < VERSION_TODO_EN_ORACLE ? -1 : (int) llamarNumero("FN_CONTAR_NEGOCIOS");
    }

    /**
     * Ejecuta un script de {@code sql/oracle} dentro de una transacción.
     *
     * @param archivo nombre del script, por ejemplo {@code 01_tablas.sql}
     */
    public void ejecutarScript(String archivo) {
        String script = leerRecurso(CARPETA + archivo);
        transaccion(c -> {
            try (Statement sentencia = c.createStatement()) {
                for (String sql : oracle.separarSentencias(script)) {
                    sentencia.execute(sql);
                }
            }
            return null;
        });
    }

    // ---- llamadas a funciones y procedimientos ---------------------------------

    /**
     * Llama a una función de Oracle que devuelve un cursor y lee sus filas.
     *
     * @param funcion    nombre, por ejemplo {@code FN_LISTAR_PRODUCTOS}
     * @param lector     cómo convertir cada fila
     * @param parametros parámetros de la función
     * @param <T>        tipo de cada fila
     * @return las filas leídas
     */
    public synchronized <T> List<T> llamar(String funcion, LectorDeFila<T> lector, Object... parametros) {
        return conReintento(() -> {
            try {
                return llamar(conexion(), funcion, lector, parametros);
            } catch (SQLException e) {
                throw traducir(e, "leer los datos");
            }
        });
    }

    /**
     * Igual que {@link #llamar(String, LectorDeFila, Object...)}, dentro de una transacción.
     *
     * @param conexion   conexión de {@link #transaccion(Trabajo)}
     * @param funcion    nombre de la función
     * @param lector     cómo convertir cada fila
     * @param parametros parámetros de la función
     * @param <T>        tipo de cada fila
     * @return las filas leídas
     * @throws SQLException si falla Oracle
     */
    public <T> List<T> llamar(Connection conexion, String funcion, LectorDeFila<T> lector, Object... parametros)
            throws SQLException {
        try (CallableStatement llamada = conexion.prepareCall("{? = call " + funcion + "(" + marcas(parametros.length) + ")}")) {
            llamada.registerOutParameter(1, CURSOR);
            asignar(llamada, 2, parametros);
            llamada.execute();
            List<T> resultado = new ArrayList<>();
            try (ResultSet fila = (ResultSet) llamada.getObject(1)) {
                while (fila.next()) {
                    resultado.add(lector.leer(fila));
                }
            }
            return resultado;
        }
    }

    /**
     * Llama a una función de Oracle que devuelve un número entero.
     *
     * @param funcion    nombre, por ejemplo {@code FN_CONTAR_NEGOCIOS}
     * @param parametros parámetros de la función
     * @return el número
     */
    public synchronized long llamarNumero(String funcion, Object... parametros) {
        return Math.round(llamarDecimal(funcion, parametros));
    }

    /**
     * Llama a una función de Oracle que devuelve un número con decimales.
     *
     * @param funcion    nombre, por ejemplo {@code FN_SALDO_CLIENTE}
     * @param parametros parámetros de la función
     * @return el número (0 si la función devolvió NULL)
     */
    public synchronized double llamarDecimal(String funcion, Object... parametros) {
        return conReintento(() -> {
            try (CallableStatement llamada = conexion().prepareCall(
                    "{? = call " + funcion + "(" + marcas(parametros.length) + ")}")) {
                llamada.registerOutParameter(1, Types.NUMERIC);
                asignar(llamada, 2, parametros);
                llamada.execute();
                BigDecimal valor = llamada.getBigDecimal(1);
                return valor == null ? 0 : valor.doubleValue();
            } catch (SQLException e) {
                throw traducir(e, "calcular en la base");
            }
        });
    }

    /**
     * Llama a un procedimiento almacenado dentro de una transacción.
     *
     * @param conexion      conexión de {@link #transaccion(Trabajo)}
     * @param procedimiento nombre, por ejemplo {@code SP_GUARDAR_PRODUCTO}
     * @param parametros    parámetros del procedimiento, en orden
     * @throws SQLException si Oracle rechaza la llamada
     */
    public static void procedimiento(Connection conexion, String procedimiento, Object... parametros)
            throws SQLException {
        try (CallableStatement llamada = conexion.prepareCall("{call " + procedimiento + "(" + marcas(parametros.length) + ")}")) {
            asignar(llamada, 1, parametros);
            llamada.execute();
        }
    }

    /**
     * Llama a procedimientos que cambian datos por su cuenta (por ejemplo,
     * SP_REGISTRAR_VENTA). Después se vuelven a leer los datos que TBalance
     * tiene en memoria.
     *
     * @param trabajo llamadas con {@link CallableStatement}
     * @param <T>     resultado
     * @return lo que devuelva el trabajo
     */
    public synchronized <T> T llamar(Trabajo<T> trabajo) {
        try {
            return transaccion(trabajo);
        } finally {
            generacion++;
        }
    }

    private static String marcas(int cantidad) {
        return String.join(", ", Collections.nCopies(cantidad, "?"));
    }

    private <R> R conReintento(java.util.function.Supplier<R> lectura) {
        try {
            return lectura.get();
        } catch (PersistenceException e) {
            // Oracle pudo cerrar la conexión: se abre otra y se intenta una vez más
            if (e.getCause() instanceof SQLException sql && conexionPerdida(sql)) {
                close();
                return lectura.get();
            }
            throw e;
        }
    }

    private static boolean conexionPerdida(SQLException e) {
        String estado = e.getSQLState();
        return e instanceof java.sql.SQLRecoverableException || e instanceof java.sql.SQLNonTransientConnectionException
                || (estado != null && estado.startsWith("08"));
    }

    // ---- transacciones ---------------------------------------------------------

    /**
     * Ejecuta un trabajo en una transacción: COMMIT si termina bien, ROLLBACK si falla.
     *
     * @param trabajo lo que hay que hacer
     * @param <T>     resultado
     * @return lo que devuelva el trabajo
     */
    public synchronized <T> T transaccion(Trabajo<T> trabajo) {
        Connection c = conexion();
        try {
            try {
                c.setAutoCommit(false);
            } catch (SQLException e) {
                if (!conexionPerdida(e)) {
                    throw e;
                }
                // todavía no se había hecho nada: se reconecta y se sigue
                close();
                c = conexion();
                c.setAutoCommit(false);
            }
            T resultado = trabajo.ejecutar(c);
            long version = tieneSeguimiento() ? anotarCambio(c) : -1;
            c.commit();
            if (version >= 0) {
                // si entre medio otro equipo cambió algo, las copias en memoria se recargan
                if (versionConocida >= 0 && version != versionConocida + 1) {
                    generacion++;
                }
                versionConocida = version;
            }
            return resultado;
        } catch (SQLException e) {
            deshacer(c);
            throw traducir(e, "guardar los cambios");
        } catch (RuntimeException e) {
            deshacer(c);
            throw e;
        } finally {
            try {
                c.setAutoCommit(true);
            } catch (SQLException ignorada) {
                // la conexión se revisa de nuevo en el próximo uso
            }
        }
    }

    /**
     * Convierte un error de Oracle en un mensaje para el usuario. Los errores de
     * los disparadores y procedimientos (RAISE_APPLICATION_ERROR) se muestran tal cual.
     *
     * @param error  error de JDBC
     * @param accion qué se estaba haciendo ("guardar los cambios")
     * @return excepción lista para lanzar
     */
    public RuntimeException traducir(SQLException error, String accion) {
        return oracle.mensajeDeDisparador(error)
                .<RuntimeException>map(ValidationException::new)
                .orElseGet(() -> new PersistenceException("No se pudo " + accion + " en Oracle: "
                        + primeraLinea(error.getMessage()), error));
    }

    // ---- varios computadores ---------------------------------------------------

    /**
     * Pregunta a Oracle si otro computador guardó algo (FN_VERSION_CAMBIOS).
     *
     * @param ahora {@code true} para preguntar ya; {@code false} respeta un mínimo de tiempo entre preguntas
     * @return número que cambia cada vez que hay que volver a leer los datos
     */
    public synchronized long revisarCambios(boolean ahora) {
        long t = System.currentTimeMillis();
        if (!ahora && t - ultimaRevision < REVISAR_CAMBIOS_CADA_MS) {
            return generacion;
        }
        ultimaRevision = t;
        try {
            if (!tieneSeguimiento()) {
                return generacion;
            }
            long version = leerVersion(conexion());
            if (version != versionConocida) {
                if (versionConocida >= 0) {
                    generacion++;
                }
                versionConocida = version;
            }
        } catch (SQLException | PersistenceException e) {
            // sin conexión por un momento: se sigue con lo que hay en memoria
        }
        return generacion;
    }

    /** @return el número actual de {@link #revisarCambios(boolean)} sin preguntar a la base */
    public synchronized long generacion() {
        return generacion;
    }

    private boolean tieneSeguimiento() {
        if (seguimiento == null) {
            seguimiento = versionEsquema() >= VERSION_TODO_EN_ORACLE;
        }
        return seguimiento;
    }

    private long anotarCambio(Connection c) throws SQLException {
        procedimiento(c, "SP_ANOTAR_CAMBIO", CLAVE_CAMBIOS);
        return leerVersion(c);
    }

    private long leerVersion(Connection c) throws SQLException {
        try (CallableStatement llamada = c.prepareCall("{? = call FN_VERSION_CAMBIOS(?)}")) {
            llamada.registerOutParameter(1, Types.NUMERIC);
            llamada.setString(2, CLAVE_CAMBIOS);
            llamada.execute();
            return llamada.getLong(1);
        }
    }

    // ---- conexión --------------------------------------------------------------

    /** Cierra la conexión (se vuelve a abrir sola si se necesita). */
    @Override
    public synchronized void close() {
        if (conexion != null) {
            try {
                conexion.close();
            } catch (SQLException ignorada) {
                // ya no se usa
            }
            conexion = null;
        }
    }

    private synchronized Connection conexion() {
        try {
            long ahora = System.currentTimeMillis();
            boolean revisar = ahora - ultimoUso > VALIDAR_TRAS_MS;
            if (conexion == null || conexion.isClosed() || (revisar && !conexion.isValid(SEGUNDOS_VALIDACION))) {
                close();
                DriverManager.setLoginTimeout(15);
                conexion = DriverManager.getConnection(config.url(), config.usuario(), config.clave());
            }
            ultimoUso = ahora;
            return conexion;
        } catch (SQLException e) {
            throw new PersistenceException(mensajeDeConexion(e), e);
        }
    }

    private String mensajeDeConexion(SQLException e) {
        String detalle = primeraLinea(e.getMessage());
        if ("08001".equals(e.getSQLState()) && detalle.toLowerCase().contains("suitable driver")) {
            return "Falta el driver JDBC de Oracle (ojdbc11). Recargue el proyecto de Maven en IntelliJ.";
        }
        return "No se pudo conectar a Oracle en " + config.url() + ": " + detalle;
    }

    // ---- parámetros ------------------------------------------------------------

    private static void asignar(PreparedStatement sentencia, int desde, Object... parametros) throws SQLException {
        for (int i = 0; i < parametros.length; i++) {
            asignarUno(sentencia, i + desde, parametros[i]);
        }
    }

    /**
     * Asigna un parámetro de una llamada.
     *
     * @param sentencia llamada preparada
     * @param posicion  posición del "?" (desde 1)
     * @param valor     valor; {@code null}, {@link Nulo} o {@link Binario} incluidos
     * @throws SQLException si el driver rechaza el valor
     */
    public static void asignarUno(PreparedStatement sentencia, int posicion, Object valor) throws SQLException {
        if (valor instanceof Nulo nulo) {
            sentencia.setNull(posicion, nulo.tipo());
        } else if (valor instanceof Binario binario) {
            // un BLOB temporal: así Oracle recibe archivos de cualquier tamaño en un procedimiento
            java.sql.Blob blob = sentencia.getConnection().createBlob();
            blob.setBytes(1, binario.datos());
            sentencia.setBlob(posicion, blob);
        } else if (valor == null) {
            sentencia.setNull(posicion, Types.VARCHAR);
        } else if (valor instanceof Double numero) {
            sentencia.setBigDecimal(posicion, BigDecimal.valueOf(numero));
        } else if (valor instanceof Boolean logico) {
            sentencia.setInt(posicion, logico ? 1 : 0);
        } else {
            sentencia.setObject(posicion, valor);
        }
    }

    private static void deshacer(Connection c) {
        try {
            c.rollback();
        } catch (SQLException ignorada) {
            // si no se pudo deshacer, la conexión se reabre en el próximo uso
        }
    }

    private static String leerRecurso(String recurso) {
        try (InputStream entrada = Database.class.getClassLoader().getResourceAsStream(recurso)) {
            if (entrada == null) {
                throw new PersistenceException("No se encontró el script " + recurso, null);
            }
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistenceException("No se pudo leer el script " + recurso, e);
        }
    }

    private static String primeraLinea(String texto) {
        return texto == null ? "" : texto.split("\\R")[0].trim();
    }
}
