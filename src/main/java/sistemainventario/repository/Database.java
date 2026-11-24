package sistemainventario.repository;

import sistemainventario.exception.PersistenceException;
import sistemainventario.exception.ValidationException;
import sistemainventario.model.DatabaseSettings;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Conexión JDBC a PostgreSQL u Oracle y utilidades para ejecutar SQL.
 *
 * <p>TBalance es una aplicación de escritorio de un solo usuario, así que usa una
 * sola conexión que se reabre si se cae. Todas las escrituras de los
 * repositorios pasan por {@link #transaccion(Trabajo)}: si algo falla (por
 * ejemplo, un disparador rechaza el cambio) se deshace todo.</p>
 */
public class Database implements AutoCloseable {

    /** Scripts que se ejecutan la primera vez, en orden (dejan el esquema en la versión 1). */
    private static final String[] SCRIPTS_INICIALES = {"01_tablas.sql", "02_disparadores.sql"};
    /**
     * Cambio v8. En Oracle crea las funciones y los procedimientos en PL/SQL
     * ({@code 04_funciones.sql} y {@code 05_procedimientos.sql}); en PostgreSQL
     * no crea nada (ese motor sigue con la lógica de Java).
     */
    private static final String CAMBIO_PROCEDIMIENTOS = "cambios/v8_procedimientos.sql";
    private static final List<String> SCRIPTS_PLSQL = List.of("04_funciones.sql", "05_procedimientos.sql");
    /** Cambios del esquema: la posición 0 lleva a la versión 2, la 1 a la 3, etc. */
    private static final String[] CAMBIOS = {"cambios/v2_borrado_logico.sql", "cambios/v3_vistas_informes.sql",
            "cambios/v4_usuarios.sql", "cambios/v5_historial.sql", "cambios/v6_abonos.sql", "cambios/v7_varios_equipos.sql",
            CAMBIO_PROCEDIMIENTOS};
    private static final int SEGUNDOS_VALIDACION = 3;
    /** Si la conexión lleva más de esto sin usarse, se revisa antes de usarla (la nube corta las inactivas). */
    private static final long VALIDAR_TRAS_MS = 20_000;
    /** Cada cuánto, como mínimo, se pregunta a la base si otro equipo cambió algo. */
    private static final long REVISAR_CAMBIOS_CADA_MS = 1_500;
    /** Fila de CAMBIO_VERSION que cuenta todos los cambios. */
    private static final String CLAVE_CAMBIOS = "TODO";

    private final DatabaseSettings config;
    private final SqlDialect dialecto;
    private Connection conexion;
    private long ultimoUso;
    /** {@code null} mientras no se sepa si la base tiene la tabla CAMBIO_VERSION (cambio v7). */
    private Boolean seguimiento;
    private long versionConocida = -1;
    private long generacion;
    private long ultimaRevision;

    /**
     * Valor nulo con su tipo SQL (PostgreSQL exige el tipo correcto en {@code setNull}).
     *
     * @param tipo constante de {@link Types}
     */
    public record Nulo(int tipo) {
    }

    /**
     * Valor binario grande (el .zip de una copia): se envía como flujo para que
     * funcione igual con BYTEA de PostgreSQL y BLOB de Oracle.
     *
     * @param datos bytes a guardar
     */
    public record Binario(byte[] datos) {
    }

    /**
     * Trabajo que se hace con la conexión dentro de una transacción.
     *
     * @param <T> resultado
     */
    @FunctionalInterface
    public interface Trabajo<T> {
        /**
         * @param conexion conexión con la transacción abierta
         * @return resultado del trabajo
         * @throws SQLException si falla una sentencia
         */
        T ejecutar(Connection conexion) throws SQLException;
    }

    /**
     * Convierte una fila del resultado en un objeto.
     *
     * @param <T> tipo del objeto
     */
    @FunctionalInterface
    public interface LectorDeFila<T> {
        /**
         * @param fila fila actual
         * @return objeto leído
         * @throws SQLException si falla la lectura de una columna
         */
        T leer(ResultSet fila) throws SQLException;
    }

    /** @param config motor, URL, usuario y clave */
    public Database(DatabaseSettings config) {
        this.config = Objects.requireNonNull(config);
        this.dialecto = SqlDialect.para(config.motor());
    }

    /** @return el dialecto del motor configurado */
    public SqlDialect getDialecto() {
        return dialecto;
    }

    /** @return nombre del motor para los mensajes */
    public String nombreMotor() {
        return config.motor().getEtiqueta();
    }

    /**
     * Abre la conexión y comprueba que responde.
     *
     * @throws PersistenceException con un mensaje claro si no se pudo conectar
     */
    public void probar() {
        conexion();
    }

    /** @return {@code true} si ya existen las tablas de TBalance */
    public boolean tieneTablas() {
        try {
            return dialecto.tablaExiste(conexion(), "PERFIL");
        } catch (SQLException e) {
            throw traducir(e, "revisar las tablas");
        }
    }

    /** Crea las tablas y los disparadores con los scripts del motor. */
    public void crearTablas() {
        for (String archivo : SCRIPTS_INICIALES) {
            ejecutarScript(dialecto.carpetaScripts() + "/" + archivo);
        }
    }

    /** @return la versión del esquema que tiene la base (0 si no tiene tablas de TBalance) */
    public int versionEsquema() {
        try {
            Connection c = conexion();
            if (!dialecto.tablaExiste(c, "ESQUEMA_VERSION")) {
                return dialecto.tablaExiste(c, "PERFIL") ? 1 : 0;
            }
        } catch (SQLException e) {
            throw traducir(e, "revisar la versión de las tablas");
        }
        return consultar("SELECT MAX(VERSION) FROM ESQUEMA_VERSION", fila -> fila.getInt(1)).get(0);
    }

    /** @return la versión que dejan todos los cambios aplicados */
    public static int versionActual() {
        return 1 + CAMBIOS.length;
    }

    /**
     * Deja la base en la versión actual: si no hay tablas las crea y después
     * aplica, en orden, los cambios que falten. Cada cambio se aplica una vez
     * y queda anotado en ESQUEMA_VERSION, así los datos que ya había se conservan.
     *
     * @return cuántos cambios se aplicaron
     */
    public int actualizarEsquema() {
        try {
            return aplicarCambios();
        } catch (PersistenceException e) {
            String texto = String.valueOf(e.getMessage());
            if (texto.contains("ORA-01031")) {
                throw new PersistenceException("Al usuario de Oracle le falta un permiso para crear las funciones y los "
                        + "procedimientos. En SQL Developer, conectado como SYSTEM, ejecute una vez: "
                        + "GRANT CREATE PROCEDURE TO TBALANCE;  y vuelva a abrir TBalance.", e);
            }
            throw e;
        }
    }

    private int aplicarCambios() {
        int version = versionEsquema();
        if (version == 0) {
            crearTablas();
            version = 1;
        }
        int aplicados = 0;
        for (int siguiente = version + 1; siguiente <= versionActual(); siguiente++) {
            int numero = siguiente;
            for (String script : scriptsDelCambio(CAMBIOS[numero - 2])) {
                ejecutarScript(dialecto.carpetaScripts() + "/" + script);
            }
            revisarRutinas();
            transaccion(c -> actualizar(c, "INSERT INTO ESQUEMA_VERSION (VERSION) VALUES (?)", numero));
            aplicados++;
        }
        seguimiento = null;
        return aplicados;
    }

    private List<String> scriptsDelCambio(String cambio) {
        if (cambio.equals(CAMBIO_PROCEDIMIENTOS) && dialecto.motor() == sistemainventario.model.DatabaseEngine.ORACLE) {
            return SCRIPTS_PLSQL;
        }
        return List.of(cambio);
    }

    /**
     * Oracle crea una función o un procedimiento aunque tenga errores (queda
     * INVALID). Si pasa, se avisa con el error exacto en vez de fallar después
     * al vender.
     */
    private void revisarRutinas() {
        try {
            java.util.Optional<String> error = dialecto.erroresDeRutinas(conexion());
            if (error.isPresent()) {
                throw new PersistenceException(error.get(), null);
            }
        } catch (SQLException e) {
            throw traducir(e, "revisar las funciones y los procedimientos");
        }
    }

    /**
     * Llama a funciones o procedimientos almacenados dentro de una transacción.
     * Como el procedimiento cambia filas por su cuenta, después se vuelven a
     * leer los datos que TBalance tiene en memoria.
     *
     * @param trabajo llamadas con {@link java.sql.CallableStatement}
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

    /**
     * Llama a una función almacenada que devuelve un número, sin abrir transacción.
     *
     * @param llamada    por ejemplo {@code "{? = call FN_SALDO_CLIENTE(?, ?)}"}
     * @param parametros valores de los "?" después del resultado
     * @return el número que devolvió la función (0 si fue nulo)
     */
    public synchronized double funcionNumerica(String llamada, Object... parametros) {
        try (java.sql.CallableStatement funcion = conexion().prepareCall(llamada)) {
            funcion.registerOutParameter(1, Types.NUMERIC);
            for (int i = 0; i < parametros.length; i++) {
                asignarUno(funcion, i + 2, parametros[i]);
            }
            funcion.execute();
            BigDecimal valor = funcion.getBigDecimal(1);
            return valor == null ? 0 : valor.doubleValue();
        } catch (SQLException e) {
            throw traducir(e, "calcular en la base");
        }
    }

    /** @return {@code true} si la tabla PERFIL no tiene filas */
    public boolean estaVacia() {
        return consultar("SELECT COUNT(*) FROM PERFIL", fila -> fila.getInt(1)).get(0) == 0;
    }

    /**
     * Ejecuta todas las sentencias de un script de los recursos.
     *
     * @param recurso ruta dentro de los recursos, por ejemplo {@code sql/oracle/01_tablas.sql}
     */
    public void ejecutarScript(String recurso) {
        String script = leerRecurso(recurso);
        transaccion(c -> {
            try (Statement sentencia = c.createStatement()) {
                for (String sql : dialecto.separarSentencias(script)) {
                    sentencia.execute(sql);
                }
            }
            return null;
        });
    }

    /**
     * Ejecuta una consulta y lee cada fila.
     *
     * @param sql        consulta con parámetros "?"
     * @param lector     convierte cada fila
     * @param parametros valores de los parámetros
     * @param <T>        tipo de cada elemento
     * @return elementos leídos en el orden de la consulta
     */
    public synchronized <T> List<T> consultar(String sql, LectorDeFila<T> lector, Object... parametros) {
        try {
            return consultarUnaVez(sql, lector, parametros);
        } catch (PersistenceException e) {
            // la nube pudo cerrar la conexión: se abre otra y se intenta una vez más
            if (e.getCause() instanceof SQLException sql2 && conexionPerdida(sql2)) {
                close();
                return consultarUnaVez(sql, lector, parametros);
            }
            throw e;
        }
    }

    private static boolean conexionPerdida(SQLException e) {
        String estado = e.getSQLState();
        return e instanceof java.sql.SQLRecoverableException || e instanceof java.sql.SQLNonTransientConnectionException
                || (estado != null && estado.startsWith("08"));
    }

    private <T> List<T> consultarUnaVez(String sql, LectorDeFila<T> lector, Object... parametros) {
        try (PreparedStatement consulta = conexion().prepareStatement(sql)) {
            asignar(consulta, parametros);
            List<T> resultado = new ArrayList<>();
            try (ResultSet fila = consulta.executeQuery()) {
                while (fila.next()) {
                    resultado.add(lector.leer(fila));
                }
            }
            return resultado;
        } catch (SQLException e) {
            throw traducir(e, "leer los datos");
        }
    }

    /**
     * Ejecuta un trabajo en una transacción: confirma si todo salió bien y
     * deshace todo si hubo un error.
     *
     * @param trabajo sentencias a ejecutar
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
     * Ejecuta INSERT, UPDATE o DELETE con la conexión de una transacción.
     *
     * @param conexion   conexión de {@link #transaccion(Trabajo)}
     * @param sql        sentencia con parámetros "?"
     * @param parametros valores
     * @return filas afectadas
     * @throws SQLException si la base rechaza la sentencia
     */
    public static int actualizar(Connection conexion, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            asignar(sentencia, parametros);
            return sentencia.executeUpdate();
        }
    }

    /**
     * Convierte un error de la base en una excepción de TBalance: los mensajes de
     * los disparadores llegan como {@link ValidationException} (el usuario los ve
     * tal cual); el resto como {@link PersistenceException}.
     *
     * @param error  error de JDBC
     * @param accion qué se estaba haciendo, para el mensaje
     * @return la excepción para lanzar
     */
    public RuntimeException traducir(SQLException error, String accion) {
        return dialecto.mensajeDeDisparador(error)
                .<RuntimeException>map(ValidationException::new)
                .orElseGet(() -> new PersistenceException("No se pudo " + accion + " en "
                        + nombreMotor() + ": " + primeraLinea(error.getMessage()), error));
    }

    /**
     * Pregunta a la base si otro equipo guardó algo desde la última vez. Se
     * hace una sola consulta corta; si nadie cambió nada, no se recarga nada.
     *
     * @param ahora {@code true} para preguntar ya (por ejemplo, antes de vender);
     *              {@code false} para preguntar como mucho cada 1,5 segundos
     * @return número que aumenta cada vez que otro equipo hizo cambios
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

    /** @return el número de cambios de otros equipos que ya se conocen (sin consultar la base) */
    public synchronized long generacion() {
        return generacion;
    }

    private boolean tieneSeguimiento() throws SQLException {
        if (seguimiento == null) {
            seguimiento = dialecto.tablaExiste(conexion(), "CAMBIO_VERSION");
        }
        return seguimiento;
    }

    private long anotarCambio(Connection c) throws SQLException {
        actualizar(c, "UPDATE CAMBIO_VERSION SET VERSION = VERSION + 1 WHERE CLAVE = ?", CLAVE_CAMBIOS);
        return leerVersion(c);
    }

    private static long leerVersion(Connection c) throws SQLException {
        try (PreparedStatement consulta = c.prepareStatement("SELECT VERSION FROM CAMBIO_VERSION WHERE CLAVE = ?")) {
            consulta.setString(1, CLAVE_CAMBIOS);
            try (ResultSet fila = consulta.executeQuery()) {
                return fila.next() ? fila.getLong(1) : 0;
            }
        }
    }

    /** Cierra la conexión. */
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
            return "Falta el driver JDBC de " + nombreMotor() + " (" + dialecto.nombreDriver()
                    + "). Recargue el proyecto de Maven en IntelliJ.";
        }
        return "No se pudo conectar a " + nombreMotor() + " en " + config.url() + ": " + detalle;
    }

    private static void asignar(PreparedStatement sentencia, Object... parametros) throws SQLException {
        for (int i = 0; i < parametros.length; i++) {
            asignarUno(sentencia, i + 1, parametros[i]);
        }
    }

    /**
     * Asigna un parámetro con las mismas reglas que las consultas normales.
     *
     * @param sentencia sentencia o llamada preparada
     * @param posicion  posición del "?" (desde 1)
     * @param valor     valor; {@code null}, {@link Nulo} o {@link Binario} incluidos
     * @throws SQLException si el driver rechaza el valor
     */
    public static void asignarUno(PreparedStatement sentencia, int posicion, Object valor) throws SQLException {
        if (valor instanceof Nulo nulo) {
            sentencia.setNull(posicion, nulo.tipo());
        } else if (valor instanceof Binario binario) {
            sentencia.setBinaryStream(posicion, new java.io.ByteArrayInputStream(binario.datos()), binario.datos().length);
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
