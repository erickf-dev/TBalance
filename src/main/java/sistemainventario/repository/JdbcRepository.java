package sistemainventario.repository;

import sistemainventario.model.IdentifiedEntity;
import sistemainventario.util.IdGenerator;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Repositorio genérico sobre una tabla de Oracle (DAO con JDBC).
 *
 * <p>No escribe SQL: para leer llama a la función {@code FN_LISTAR_...} de la
 * tabla, que devuelve un cursor, y para guardar llama a los procedimientos
 * {@code SP_GUARDAR_<TABLA>} y {@code SP_BORRAR_<TABLA>}, con los parámetros en
 * el mismo orden de {@link #columnas()}. Cada subclase dice su tabla, sus
 * columnas, su función de lectura y cómo convertir una fila en objeto.</p>
 *
 * <p>Las tablas de un negocio tienen llave (PERFIL_ID, ID). Los datos del perfil
 * se leen una vez y quedan en memoria para las consultas; cada cambio se
 * escribe primero en la base, dentro de una transacción, y solo si la base lo
 * acepta se refleja en memoria.</p>
 *
 * @param <T> entidad con ID
 */
public abstract class JdbcRepository<T extends IdentifiedEntity> implements Repository<T> {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    protected final Database bd;
    protected final String perfilId;
    private List<T> cache;
    /** Generación de cambios de otros equipos con la que se cargó {@link #cache}. */
    private long generacionCache = -1;

    /**
     * @param bd       conexión a la base
     * @param perfilId negocio dueño de los datos ({@code null} para la tabla PERFIL)
     */
    protected JdbcRepository(Database bd, String perfilId) {
        this.bd = Objects.requireNonNull(bd);
        this.perfilId = perfilId;
    }

    /** @return nombre de la tabla */
    protected abstract String tabla();

    /** @return columnas que se escriben, sin PERFIL_ID ni ID (mismo orden que los parámetros de SP_GUARDAR) */
    protected abstract List<String> columnas();

    /** @return función de Oracle que devuelve las filas, por ejemplo {@code FN_LISTAR_PRODUCTOS} */
    protected abstract String funcionListar();

    /**
     * @param entidad objeto a guardar
     * @return valores en el mismo orden de {@link #columnas()}
     */
    protected abstract Object[] valores(T entidad);

    /**
     * @param fila fila del SELECT (tiene la columna ID y todas las de {@link #columnas()})
     * @return la entidad
     * @throws SQLException si falla la lectura
     */
    protected abstract T leer(ResultSet fila) throws SQLException;

    /**
     * Completa las entidades leídas con datos de otras tablas (por ejemplo el
     * detalle de las ventas). Por defecto no hace nada.
     *
     * @param entidades entidades recién leídas
     */
    protected void completar(List<T> entidades) {
    }

    /**
     * Escribe los datos de tablas hijas después del INSERT o del UPDATE.
     * Por defecto no hace nada.
     *
     * @param conexion conexión de la transacción
     * @param entidad  entidad guardada
     * @param nueva    {@code true} si fue un INSERT
     * @throws SQLException si la base rechaza un cambio
     */
    protected void guardarHijos(Connection conexion, T entidad, boolean nueva) throws SQLException {
    }

    /** Pregunta ya a la base si otro equipo cambió algo y, si es así, recarga. */
    @Override
    public void refrescar() {
        bd.revisarCambios(true);
        datos();
    }

    @Override
    public List<T> listarTodos() {
        return new ArrayList<>(datos());
    }

    @Override
    public Optional<T> buscarPorId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return datos().stream().filter(e -> id.equals(e.getId())).findFirst();
    }

    @Override
    public boolean existe(String id) {
        return buscarPorId(id).isPresent();
    }

    @Override
    public List<T> filtrar(Predicate<T> condicion) {
        return datos().stream().filter(condicion).collect(Collectors.toList());
    }

    @Override
    public void agregar(T entidad) {
        Objects.requireNonNull(entidad, "La entidad no puede ser nula");
        // se carga la copia en memoria ANTES de insertar: si se cargara después,
        // ya traería la fila nueva y quedaría repetida al agregarla abajo
        List<T> enMemoria = datos();
        bd.transaccion(c -> {
            Database.procedimiento(c, "SP_GUARDAR_" + tabla(), concatenar(concatenar(new Object[]{1}, llave(entidad.getId())), valores(entidad)));
            guardarHijos(c, entidad, true);
            return null;
        });
        enMemoria.add(entidad);
    }

    @Override
    public boolean actualizar(T entidad) {
        int posicion = indiceDe(entidad.getId());
        if (posicion < 0) {
            return false;
        }
        bd.transaccion(c -> {
            Database.procedimiento(c, "SP_GUARDAR_" + tabla(), concatenar(concatenar(new Object[]{0}, llave(entidad.getId())), valores(entidad)));
            guardarHijos(c, entidad, false);
            return null;
        });
        datos().set(posicion, entidad);
        return true;
    }

    @Override
    public boolean eliminar(String id) {
        if (indiceDe(id) < 0) {
            return false;
        }
        bd.transaccion(c -> {
            Database.procedimiento(c, "SP_BORRAR_" + tabla(), llave(id));
            return null;
        });
        datos().removeIf(e -> Objects.equals(id, e.getId()));
        return true;
    }

    @Override
    public String siguienteIdNumerico() {
        return IdGenerator.siguienteNumerico(datos().stream().map(IdentifiedEntity::getId).collect(Collectors.toList()));
    }

    /** @return {@code true} si la tabla pertenece a un negocio (tiene PERFIL_ID) */
    protected boolean porPerfil() {
        return perfilId != null;
    }

    /**
     * @param funcionExiste función que dice si existe (FN_EXISTE_CATEGORIA o FN_EXISTE_PROVEEDOR)
     * @param id            ID referido
     * @return el ID si existe en ese negocio; si no, un nulo (para no romper la llave foránea)
     */
    protected Object referenciaValida(String funcionExiste, String id) {
        if (id == null || id.isBlank()) {
            return new Database.Nulo(Types.VARCHAR);
        }
        return bd.llamarNumero(funcionExiste, perfilId, id) > 0 ? id : new Database.Nulo(Types.VARCHAR);
    }

    // ---- conversión de valores -------------------------------------------------

    /** @return texto o nulo de tipo VARCHAR */
    protected static Object texto(String valor) {
        return valor == null ? new Database.Nulo(Types.VARCHAR) : valor;
    }

    /** @return fecha (columna DATE) a partir de "yyyy-MM-dd", o nulo */
    protected static Object dia(String iso) {
        return parsearFecha(iso).map(f -> (Object) Date.valueOf(f.toLocalDate()))
                .orElse(new Database.Nulo(Types.DATE));
    }

    /** @return fecha y hora (columna TIMESTAMP) a partir de "yyyy-MM-dd" o "yyyy-MM-ddTHH:mm:ss", o nulo */
    protected static Object momento(String iso) {
        return parsearFecha(iso).map(f -> (Object) Timestamp.valueOf(f))
                .orElse(new Database.Nulo(Types.TIMESTAMP));
    }

    /** @return la columna DATE como "yyyy-MM-dd" o {@code null} */
    protected static String leerDia(ResultSet fila, String columna) throws SQLException {
        Date valor = fila.getDate(columna);
        return valor == null ? null : valor.toLocalDate().toString();
    }

    /**
     * @return la columna TIMESTAMP como "yyyy-MM-ddTHH:mm:ss", o solo "yyyy-MM-dd"
     * si la hora es 00:00:00 (así quedan igual las fechas que se guardaron sin hora)
     */
    protected static String leerMomento(ResultSet fila, String columna) throws SQLException {
        Timestamp valor = fila.getTimestamp(columna);
        if (valor == null) {
            return null;
        }
        LocalDateTime fecha = valor.toLocalDateTime().withNano(0);
        return fecha.toLocalTime().equals(LocalTime.MIDNIGHT) ? fecha.toLocalDate().toString() : fecha.format(FECHA_HORA);
    }

    /** @return la columna numérica como double (0 si es nula) */
    protected static double leerNumero(ResultSet fila, String columna) throws SQLException {
        java.math.BigDecimal valor = fila.getBigDecimal(columna);
        return valor == null ? 0 : valor.doubleValue();
    }

    private static Optional<LocalDateTime> parsearFecha(String iso) {
        if (iso == null || iso.isBlank()) {
            return Optional.empty();
        }
        String texto = iso.trim().replace(' ', 'T');
        try {
            if (texto.length() <= 10) {
                return Optional.of(LocalDate.parse(texto).atStartOfDay());
            }
            return Optional.of(LocalDateTime.parse(texto.length() > 19 ? texto.substring(0, 19) : texto));
        } catch (DateTimeParseException e) {
            throw new sistemainventario.exception.ValidationException("Fecha inválida: " + iso);
        }
    }

    // ---- lectura y llave -------------------------------------------------------

    private List<T> datos() {
        long generacion = bd.revisarCambios(false);
        if (generacion != generacionCache) {
            cache = null;
        }
        if (cache == null) {
            generacionCache = generacion;
            List<T> leidos = porPerfil() ? bd.llamar(funcionListar(), this::leer, perfilId)
                    : bd.llamar(funcionListar(), this::leer);
            completar(leidos);
            cache = Collections.synchronizedList(new ArrayList<>(leidos));
        }
        return cache;
    }

    private Object[] llave(String id) {
        return porPerfil() ? new Object[]{perfilId, id} : new Object[]{id};
    }

    private static Object[] concatenar(Object[] a, Object[] b) {
        Object[] todo = new Object[a.length + b.length];
        System.arraycopy(a, 0, todo, 0, a.length);
        System.arraycopy(b, 0, todo, a.length, b.length);
        return todo;
    }

    private int indiceDe(String id) {
        List<T> lista = datos();
        for (int i = 0; i < lista.size(); i++) {
            if (Objects.equals(id, lista.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }
}
