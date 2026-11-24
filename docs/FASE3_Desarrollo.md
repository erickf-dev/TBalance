# Fase 3 — Desarrollo e implementación

Universidad Popular del Cesar · Programación de Computadores III (SS462) · Docente: Ing. Esp. Alfredo Bautista<br>
Integrantes: Kevin Jhosep Mercado · Reinaldo Padilla Pedroza · Dair José Quintero

[README](../README.md) · [Fase 1](FASE1_Especificacion.md) · [Fase 2](FASE2_Arquitectura_y_Diseno.md) · [Fase 3](FASE3_Desarrollo.md) · [Fase 4](FASE4_Documentacion_Tecnica.md) · [Trazabilidad](TRAZABILIDAD.md)

---

## 1. Clases básicas

### 1.1 Encapsulamiento

Todos los atributos son `private` (o `protected` cuando una subclase los necesita, como el repositorio de `EntityService`). Los datos se cambian con métodos que revisan las reglas; por ejemplo, el stock de un producto se cambia con `aumentarStock(int)` y `disminuirStock(int)`, que no dejan cantidades negativas, y no con `setStock(getStock() - n)`.

### 1.2 Constructores

Las clases del modelo tienen un constructor vacío, que usa la librería Gson para leer los datos, y otro con los datos principales. Los servicios y controladores reciben en el constructor los objetos que necesitan. Ejemplos:

| Clase | Constructores |
|---|---|
| `Product` | `Product()`<br>`Product(String id, String nombre, String descripcion, String categoria, String categoriaId, String proveedorId, double precioCompra, double precioVenta, int stock, int stockMin, String tipo)` |
| `Sale` | `Sale()`<br>`Sale(String id, String cliente, String metodoPago, String observacion, String fecha)` |
| `CreditPayment` | `CreditPayment()`<br>`CreditPayment(String clienteDocumento, double monto, String metodoPago)` |
| `SaleService` | `SaleService(InventoryService inventario, Repository<Sale> ventas)` |
| `CreditService` | `CreditService(SaleService ventas, Repository<CreditPayment> abonos)` |
| `ProductJdbcRepository` | `ProductJdbcRepository(Database bd, String perfilId)` |

El constructor con datos de `SaleItem` solo se puede usar dentro del paquete `model`, para que solo la venta cree sus renglones. El de `AppContext` es privado: hay una sola instancia y se obtiene con `AppContext.get()`.

### 1.3 Getters y setters

Las clases del modelo tienen getters y setters para sus atributos (están en la [descripción de cada clase](FASE2_Arquitectura_y_Diseno.md#descripción-de-cada-clase)). Los que reciben listas guardan una copia, para que dos objetos no compartan la misma lista.

## 2. Herencia y polimorfismo

### 2.1 Jerarquías

```
IdentifiedEntity (abstracta)
├── Product, Category, Supplier, Profile, User      (implementan Deshabilitable)
├── Sale, CreditPayment                              (implementan Anulable)
├── AuditEntry
└── InventoryMovement (abstracta, implementa Anulable)
    ├── StockEntry
    └── StockExit

Repository<T> (interfaz)
├── JsonRepository<T> (abstracta) → 10 repositorios para el contenido de las copias de seguridad
└── JdbcRepository<T> (abstracta) → 10 repositorios de base de datos

EntityService<T> (abstracta)
├── InventoryService
├── CategoryService
└── SupplierService
```

### 2.2 Métodos sobrescritos

El proyecto tiene **213 métodos con `@Override`** en 46 clases. Los principales:

| Clase | Métodos sobrescritos |
|---|---|
| `StockEntry` | `aplicarA()`, `getPrefijoId()` |
| `StockExit` | `aplicarA()`, `getPrefijoId()` |
| `IdentifiedEntity` | `equals()`, `hashCode()` |
| `JsonRepository` | `listarTodos()`, `buscarPorId()`, `existe()`, `filtrar()`, `agregar()`, `actualizar()`, `eliminar()`, `siguienteIdNumerico()` |
| `JdbcRepository` | `refrescar()`, `listarTodos()`, `buscarPorId()`, `existe()`, `filtrar()`, `agregar()`, `actualizar()`, `eliminar()` … |
| `ProductJdbcRepository` | `tabla()`, `funcionListar()`, `columnas()`, `valores()`, `leer()` |
| `SaleJdbcRepository` | `tabla()`, `funcionListar()`, `columnas()`, `valores()`, `leer()`, `completar()`, `guardarHijos()` |
| `CreditPaymentJdbcRepository` | `tabla()`, `funcionListar()`, `columnas()`, `valores()`, `leer()` |
| `CategoryService` | `validarDeshabilitar()`, `validar()`, `generarId()`, `nombreEntidad()` |
| `InventoryService` | `validar()`, `generarId()`, `nombreEntidad()` |
| `SupplierService` | `validar()`, `generarId()`, `nombreEntidad()` |
| `Sale` | `isAnulada()`, `getFechaAnulacion()`, `getMotivoAnulacion()`, `anular()`, `getAnuladaPor()`, `setAnuladaPor()` |
| `CreditPayment` | `isAnulada()`, `anular()`, `getFechaAnulacion()`, `getMotivoAnulacion()`, `getAnuladaPor()`, `setAnuladaPor()` |
| `Product` | `getEtiqueta()`, `isActivo()`, `setActivo()` |
| `WebcamCamera` | `abrir()`, `capturar()`, `cerrar()` |
| `PhoneCamera` | `abrir()`, `capturar()`, `cerrar()`, `avisar()` |
| `GeminiClient` | `generar()` |
| `SseNotifier` | `ejecutarScript()` |
| `WebViewNotifier` | `ejecutarScript()` |
| `DatabaseStorageProvider` | `nombre()`, `perfiles()`, `usuarios()`, `auditoria()`, `productos()`, `categorias()`, `proveedores()`, `entradas()` … |
| `SinConexionStorageProvider` | `nombre()`, `perfiles()`, `usuarios()`, `auditoria()`, `productos()`, `categorias()`, `proveedores()`, `entradas()` … |
| `OracleRoutines` | `registrarVenta()`, `anularVenta()`, `registrarAbono()`, `saldoCliente()`, `valorInventario()`, `utilidadPeriodo()` |
| `JsonStorageProvider` | `nombre()`, `perfiles()`, `usuarios()`, `auditoria()`, `productos()`, `categorias()`, `proveedores()`, `entradas()` … |
| `DatabaseSnapshot` | `prepararCarpeta()`, `liberarCarpeta()`, `restaurarDesde()` |
| `Main` | `start()` |

Ejemplo de polimorfismo: `MovementService` registra cualquier `InventoryMovement` llamando a `aplicarA(producto)`. Si es una entrada, el stock sube; si es una salida, baja. El servicio no pregunta de qué tipo es. Lo mismo pasa con `Repository<T>`: `SaleService` llama a `agregar(venta)` y, según el almacenamiento elegido, la venta se guarda en un archivo o en la base de datos.

### 2.3 Interfaces

El proyecto tiene 13 interfaces: `Repository<T>`, `StorageProvider`, `DatabaseRoutines`, `BackupStore`, `DataSnapshot`, `Deshabilitable`, `Anulable`, `Camera`, `CameraProvider`, `ViewNotifier`, `FileOpener`, `LanguageModel` y `ScreenTransition`. Están explicadas en la [Fase 2](FASE2_Arquitectura_y_Diseno.md#42-interfaces).

### 2.4 Colecciones

| Colección | Veces que se crea | Para qué se usa |
|---|---|---|
| `ArrayList` | 73 | Listas: la copia en memoria de cada repositorio, los renglones de una venta, los abonos y ventas de un cliente, filas de informes. |
| `LinkedHashMap` | 23 | Mapas que conservan el orden: la configuración de cada negocio, las cuentas de Fiados, los datos que se envían a la pantalla. |
| `HashMap` | 2 | Búsquedas por nombre donde el orden no importa (métodos del puente, acumulados del informe). |
| `EnumSet` | 11 | Conjuntos de permisos de cada rol y medios de pago activos. |
| `EnumMap` | 2 | Opciones de la librería de códigos QR. |
| `ConcurrentHashMap` | 1 | Propuestas del asistente por confirmar (las usan dos hilos). |
| `CopyOnWriteArrayList` | 2 | Conexiones abiertas de la ventana de Edge (las usan varios hilos). |

Las listas se declaran como `List<T>` y los mapas como `Map<K, V>`. Se usa `LinkedHashMap` cuando importa el orden (por ejemplo, las cuentas de los clientes en Fiados), `HashMap` cuando no, `EnumSet` para los permisos de cada rol y colecciones seguras para varios hilos donde el asistente y la pantalla usan los mismos datos.

## 3. Persistencia con base de datos relacional y DAO

TBalance guarda todos los datos en **Oracle** (18c Express Edition o superior). La conexión se escribe una sola vez: la primera vez que se abre TBalance (o si Oracle no responde) aparece la pantalla **Conectar con Oracle**, con la dirección, el usuario y la contraseña; después el dueño la puede cambiar en **Copias de seguridad → Conexión con Oracle**. Estos datos quedan en `~/.inventariopro/basedatos.json`, solo en ese computador. Sin conexión no se puede entrar ni guardar nada.

Se usó el patrón **DAO** (objeto de acceso a datos): cada tabla tiene una clase que sabe leerla y guardarla, y el resto del programa solo conoce la interfaz `Repository<T>`.

| Pieza | Clase | Qué hace |
|---|---|---|
| Interfaz del DAO | `Repository<T>` | Listar, buscar por ID, agregar, actualizar y eliminar. Los servicios solo conocen esta interfaz. |
| DAO genérico | `JdbcRepository<T>` | Lee llamando a la función de consulta de la tabla (por ejemplo `FN_LISTAR_PRODUCTOS`) y guarda llamando a `SP_GUARDAR_<TABLA>` y `SP_BORRAR_<TABLA>` con `CallableStatement`. Cada cambio va en una transacción: si falla, no queda nada a medias. |
| DAO por tabla | `ProductJdbcRepository`, `SaleJdbcRepository`, `CreditPaymentJdbcRepository` y 7 más | Cada una dice su tabla, sus columnas, su función de consulta y cómo convertir una fila en objeto. `SaleJdbcRepository` guarda la venta y sus productos juntos. |
| Ventas, abonos e informes | `OracleRoutines` | Llama a los procedimientos `SP_REGISTRAR_VENTA`, `SP_ANULAR_VENTA` y `SP_REGISTRAR_ABONO` y a las funciones `FN_SALDO_CLIENTE`, `FN_VALOR_INVENTARIO` y `FN_UTILIDAD_PERIODO`. |
| Conexión | `Database`, `OracleDialect` | Abre la conexión, ejecuta los scripts, llama a las funciones y procedimientos y convierte los errores de Oracle en mensajes para el usuario. |
| Abrir la base | `StorageService`, `DatabaseStorageProvider`, `SinConexionStorageProvider` | Se conectan con Oracle, crean o actualizan las tablas y entregan los repositorios; si Oracle no responde, TBalance pide los datos de conexión. |
| Copias de seguridad | `DatabaseSnapshot`, `DataTransferService` | Pasan los datos de Oracle a un .zip (y del .zip a Oracle al restaurar). El .zip se guarda en la tabla `RESPALDO`. |

Los scripts están en `src/main/resources/sql/oracle`. TBalance ejecuta `01` y `02` la primera vez, y después cada cambio una sola vez, en orden, anotándolo en la tabla `ESQUEMA_VERSION`. Así una base que ya tenía datos se actualiza sin perderlos.

| Script | Qué hace |
|---|---|
| `00_crear_usuario.sql` | Crea el usuario `TBALANCE` con sus permisos (también `CREATE PROCEDURE`, para las funciones y procedimientos). |
| `01_tablas.sql` | Crea las tablas con sus llaves primarias, foráneas y restricciones. |
| `02_disparadores.sql` | Dos disparadores: uno valida el producto (no deja stock ni precios negativos) y otro guarda el historial de precios. |
| `03_probar_disparadores.sql` | Prueba los disparadores y deshace los cambios. |
| `cambios/v2_borrado_logico.sql` | Columnas para deshabilitar y anular, y tablas `PROVEEDOR_CATEGORIA`, `RESPALDO` y `ESQUEMA_VERSION`. |
| `cambios/v3_vistas_informes.sql` | Cuatro vistas para informes (ventas por día, ganancia por producto, valor del inventario y compras por mes). |
| `cambios/v4_usuarios.sql` | Tablas `USUARIO` y `NEGOCIO_USUARIO`, y quién vendió o anuló cada movimiento. |
| `cambios/v5_historial.sql` | Tabla `AUDITORIA` (historial) y la cédula del cliente en las ventas fiadas. |
| `cambios/v6_abonos.sql` | Tabla `ABONO` (abonos de los fiados). |
| `cambios/v7_varios_equipos.sql` | Tabla `CAMBIO_VERSION`: un contador que sube con cada cambio, para que los demás computadores sepan que deben actualizarse. |
| `04_funciones.sql` (cambio v8) | `FN_SALDO_CLIENTE`, `FN_VALOR_INVENTARIO`, `FN_UTILIDAD_PERIODO` y `FN_CLAVE_CLIENTE`. |
| `05_procedimientos.sql` (cambio v8) | `SP_REGISTRAR_VENTA`, `SP_ANULAR_VENTA` y `SP_REGISTRAR_ABONO`. |
| `06_probar_procedimientos.sql` | Crea un negocio de prueba, vende, abona y anula, muestra los resultados y deshace todo. |
| `07_funciones_consulta.sql` (cambio v9) | Las 21 funciones de consulta (`FN_LISTAR_...`, `FN_EXISTE_...`, `FN_CONTAR_NEGOCIOS`, `FN_VERSION_CAMBIOS`, `FN_LEER_COPIA`). |
| `08_procedimientos_guardar.sql` (cambio v9) | Los 31 procedimientos para guardar: `SP_GUARDAR_` y `SP_BORRAR_` de cada tabla, los de las filas hijas (productos de una venta, tipos de un proveedor, accesos de un usuario, ajustes de un negocio), las copias de seguridad y `SP_ANOTAR_CAMBIO`. |
| `00_crear_usuario_nube.sql` | Crea el usuario `TBALANCE` en Oracle Autonomous Database (nube). |
| `99_borrar_todo.sql` | Borra todas las tablas, funciones y procedimientos (solo para empezar de cero). |

Los cambios v2 a v9 **solo agregan** tablas, columnas, vistas, funciones o procedimientos: no cambian ni borran lo anterior.

**Todo el acceso a datos está en Oracle.** El código Java no escribe `SELECT`, `INSERT`, `UPDATE` ni `DELETE`: solo llama a funciones y procedimientos con `CallableStatement`.

| Para | En Oracle | En Java |
|---|---|---|
| Leer una tabla | `FN_LISTAR_PRODUCTOS(negocio)` devuelve un cursor (`SYS_REFCURSOR`) con las filas | `Database.llamar("FN_LISTAR_PRODUCTOS", ...)` |
| Saber si algo existe o contar | `FN_EXISTE_CATEGORIA`, `FN_CONTAR_NEGOCIOS` devuelven un número | `Database.llamarNumero(...)` |
| Agregar o cambiar una fila | `SP_GUARDAR_PRODUCTO(P_NUEVO, ...)`: con `P_NUEVO = 1` hace el `INSERT`, con `0` el `UPDATE` | `Database.procedimiento(c, "SP_GUARDAR_PRODUCTO", ...)` |
| Vender, anular, abonar | `SP_REGISTRAR_VENTA`, `SP_ANULAR_VENTA`, `SP_REGISTRAR_ABONO`: revisan las reglas y tocan varias tablas en una sola transacción | `OracleRoutines` |
| Informes | `FN_UTILIDAD_PERIODO`, `FN_VALOR_INVENTARIO`, `FN_SALDO_CLIENTE` | `ReportService`, `CostService`, `CreditService` |

Los parámetros de los procedimientos usan `%TYPE` (por ejemplo `P_NOMBRE IN PRODUCTO.NOMBRE%TYPE`): tienen el mismo tipo que la columna. Los procedimientos no hacen `COMMIT`: TBalance los llama dentro de una transacción y hace `COMMIT` si todo salió bien o `ROLLBACK` si Oracle rechazó algo; el mensaje de `RAISE_APPLICATION_ERROR` (por ejemplo, "Stock insuficiente para Arroz. Disponible: 4") se le muestra tal cual al usuario. Java sigue revisando lo que es de la pantalla (campos obligatorios, permisos de cada rol, el medio de pago).

Solo quedan cuatro sentencias directas, porque se usan antes de que existan las funciones: ver si una tabla existe, ver en qué versión está la base, anotar una versión nueva y revisar si alguna función o procedimiento quedó con errores (`USER_ERRORS`). Si al crear las funciones falta el permiso `CREATE PROCEDURE`, TBalance lo dice al conectarse con la instrucción exacta.

**Borrado lógico.** Desde el programa no se borra nada que tenga historial. Negocios, categorías, proveedores, productos y usuarios se *deshabilitan* y se pueden volver a habilitar. Ventas, entradas, salidas y abonos se *anulan* con un motivo, el stock o el saldo se corrige y quedan en la lista de anulados.

**Historial.** Cada acción que pasa por el puente (`BridgeInvoker`) se revisa en `AccessPolicy` (¿tiene sesión y permiso?) y luego `AuditTrail` la anota en `AUDITORIA` con la persona, el rol, el negocio y la hora. Nunca se anota una contraseña. La anulación de una venta la anota `SP_ANULAR_VENTA` dentro de Oracle y `AuditTrail` no la repite.

**Fiados.** Lo que debe un cliente no se guarda: se calcula sumando sus ventas fiadas vigentes y restando sus abonos vigentes (`FN_SALDO_CLIENTE` en Oracle; `CreditService` arma la lista de Fiados). Así, si se anula una venta o un abono, la deuda se corrige sola.

**Varios computadores.** Varios computadores pueden usar la misma base de Oracle a la vez (en una red o en la nube, ver la [guía](GUIA_NUBE.md)). Cada repositorio JDBC guarda una copia en memoria para responder rápido; para que no quede vieja, cada vez que un computador guarda algo, `Database` llama a `SP_ANOTAR_CAMBIO`, que sube el contador de `CAMBIO_VERSION` en la misma transacción. Los demás preguntan ese contador con `FN_VERSION_CAMBIOS` (como mucho cada 1,5 segundos) y, si cambió, `JdbcRepository` vuelve a leer sus datos. La pantalla hace la misma pregunta cada 5 segundos y se actualiza sola. Antes de vender o sacar mercancía, `SaleService` y `MovementService` piden el dato fresco (`refrescar()`), y el disparador de la tabla `PRODUCTO` no deja el stock en negativo.

El driver de Oracle está en el `pom.xml`: `com.oracle.database.jdbc:ojdbc11`.

## 4. Interfaz gráfica

### 4.1 Tecnología

La interfaz no se hizo con formularios de NetBeans: es una página web dentro del proyecto (`src/main/resources/html`, con HTML, CSS y JavaScript), desarrollada en IntelliJ IDEA. Al arrancar, TBalance abre esa página en una ventana de **Microsoft Edge** (sin barra de direcciones, como un programa normal); si no hay Edge, la muestra en una ventana de **JavaFX 21**. La página habla con Java por medio del objeto `javaBridge`.

### 4.2 Componentes

| Componente | Cantidad |
|---|---|
| Secciones | 15: Inicio de sesión, Hoy, Productos, Categorías, Proveedores, Entradas, Salidas, Ventas, Fiados, Informes, Costos, Mis negocios, Usuarios, Historial y Copias de seguridad |
| Ventanas emergentes (modales) | 14 (producto, categoría, proveedor, entrada, salida, venta, venta por QR, etiqueta QR, cuenta de un cliente, negocio, confirmaciones…) |
| Botones | 157 |
| Campos de texto | 83 |
| Listas desplegables | 16 |
| Tablas | 10 |
| Panel del asistente | 1 (se abre con el botón "Pregúntale a TBalance" o `Ctrl+K`) |

### 4.3 Manejo de eventos

| Sentido | Cómo funciona | Ejemplo |
|---|---|---|
| Pantalla → Java | Los botones y campos usan `onclick` (147), `onchange` (17) y `oninput` (7). La función de JavaScript llama a `javaBridge.<método>()`, que llega al `JavaBridge` en Java. | Registrar abono → `javaBridge.registrarAbono(json)` → `CreditController` → `CreditService.registrarAbono` |
| Java → Pantalla | Java ejecuta una función de la página para avisar un cambio. | `cargarEstadoDesdeJava()` después de restaurar una copia; `onAsistenteRespuesta(...)` cuando el asistente contesta. |

Cada método del puente devuelve `OK`, los datos en JSON o `ERROR: mensaje`, y la página muestra el error como un aviso.

### 4.4 Separación en tres capas

| Capa | Qué hace cuando el usuario registra un abono |
|---|---|
| Presentación (`html`, `bridge`) | La página lee el valor y el medio de pago; `CreditController` pone quién lo recibió (tomado de la sesión) y llama al servicio. |
| Lógica de negocio (`service`, `model`) | `CreditPayment.validar()` revisa los datos y `CreditService` revisa que no pase de lo que el cliente debe. |
| Acceso a datos (`repository`) | `OracleRoutines` llama a `SP_REGISTRAR_ABONO`, que revisa la deuda con `FN_SALDO_CLIENTE` y guarda el abono en la tabla `ABONO`. |

## 5. Buenas prácticas

| Práctica | Cómo se aplicó |
|---|---|
| Código limpio | Métodos cortos con nombres que dicen lo que hacen (`disminuirStock`, `registrarAbono`, `aceptaMedioPago`); se quitó el código que no se usaba. |
| Javadoc | Todas las clases y los métodos públicos tienen su comentario `/** ... */`. |
| Convenciones de nombres | `PascalCase` en clases, `camelCase` en métodos y atributos y `MAYUSCULAS_CON_GUION` en constantes (`Sale.PREFIJO_ID`, `CreditPayment.PREFIJO_ID`). |
| Validación de datos | Cada clase del modelo tiene `validar()` (`Product`, `Sale`, `CreditPayment`…) y los servicios revisan las reglas que dependen de otros datos (stock, deuda, medios de pago del negocio, permisos del rol). |
| Excepciones | `ValidationException` para reglas del negocio, `PersistenceException` para errores al guardar y `AssistantException` para fallas del asistente. `BridgeResponse` las convierte en un mensaje para el usuario en un solo lugar. |
| Datos privados | Las contraseñas se guardan cifradas; la clave del asistente y la de la base de datos no están en el código ni en GitHub. |

## 6. Tabla de trazabilidad GRASP y SOLID

| Patrón o principio | Dónde se aplica |
|---|---|
| Creador | [`Sale.agregarItem`](../src/main/java/sistemainventario/model/Sale.java), [`ProfileServicesFactory`](../src/main/java/sistemainventario/service/ProfileServicesFactory.java), [`CreditService.listarCuentas`](../src/main/java/sistemainventario/service/CreditService.java) |
| Controlador | [`JavaBridge`](../src/main/java/sistemainventario/bridge/JavaBridge.java) y los controladores del paquete `bridge` |
| Experto en información | [`Product`](../src/main/java/sistemainventario/model/Product.java), [`Sale`](../src/main/java/sistemainventario/model/Sale.java), [`CustomerDebt`](../src/main/java/sistemainventario/model/CustomerDebt.java), [`Rol`](../src/main/java/sistemainventario/model/Rol.java) |
| Bajo acoplamiento | [`Repository<T>`](../src/main/java/sistemainventario/repository/Repository.java), [`Camera`](../src/main/java/sistemainventario/dispositivo/Camera.java), [`LanguageModel`](../src/main/java/sistemainventario/ia/LanguageModel.java) |
| Alta cohesión | Controladores del `bridge`, [`AccessPolicy`](../src/main/java/sistemainventario/bridge/AccessPolicy.java), [`AuditTrail`](../src/main/java/sistemainventario/bridge/AuditTrail.java), [`CreditService`](../src/main/java/sistemainventario/service/CreditService.java) |
| SRP | `Product` / `InventoryService` / `ProductRepository`; `UserService` / `SessionService` / `PasswordHasher` |
| OCP | `Repository<T>` con `JsonRepository<T>` y `JdbcRepository<T>`; `Camera` con `WebcamCamera` y `PhoneCamera` |

La tabla completa, con código limpio, está en [TRAZABILIDAD.md](TRAZABILIDAD.md).

## 7. Gestión del proyecto con Git

Datos del repositorio hasta la versión v3.4.0 y los commits de la ronda v3.5.0. Los commits de la v3.5.0 tienen su código (hash) en GitHub.

### 7.1 Commits por integrante

| Integrante | Usuario GitHub | Commits | Pull Requests que aprobó y fusionó |
|---|---|---|---|
| Kevin Jhosep Mercado | @kjhosepmercadom | 27 | 7 |
| Dair José Quintero | @djosequintero | 24 | 10 |
| Reinaldo Padilla Pedroza | @rjpadillap | 23 | 8 |

Además está el commit `Initial commit` con el que GitHub creó el repositorio. Cada Pull Request lo aprueba y lo fusiona un integrante distinto de su autor.

### 7.2 Evidencia de commits

**Kevin Jhosep Mercado**

| Commit | Fecha | Mensaje |
|---|---|---|
| `2646428` | 2026-10-27 | chore: importar codigo base de TBalance desarrollado en Programacion II |
| `2d17723` | 2026-10-27 | chore: configurar repositorio P3 con gitignore, gitattributes y README inicial |
| `5f1d3ea` | 2026-10-27 | feat(bridge): NotificadorVista y RespuestaBridge para centralizar respuestas y errores |
| `3d70123` | 2026-10-27 | refactor(service): GestorEntidad con Template Method para productos, categorias y proveedores |
| `84f302d` | 2026-10-27 | refactor(service): inyeccion de dependencias, FabricaServiciosPerfil y AppContext encapsulado |
| `251cf54` | 2026-10-27 | refactor(bridge): JavaBridge como fachada Controller con controladores por caso de uso |
| `869407a` | 2026-10-27 | build: Launcher para ejecutar desde IntelliJ IDEA |
| `e671da6` | 2026-10-27 | docs: tabla de trazabilidad GRASP, SOLID y codigo limpio |
| `d72ce28` | 2026-10-27 | feat(service): add GestorRespaldo to create, list, restore and delete backups |
| `afbb89f` | 2026-10-27 | feat(app): reload AppContext data after restoring a backup |
| `1f7ad70` | 2026-10-27 | feat(bridge): expose backup operations through RespaldoController |
| `8a0a051` | 2026-10-28 | feat(model): let Producto build and read the content of its QR label |
| `bd12671` | 2026-10-28 | feat(service): add GestorEtiquetas to draw labels and printable sheets |
| `87af73a` | 2026-10-28 | feat(service): add EscanerVentaQR to turn camera frames into products |
| `009c723` | 2026-10-28 | feat(bridge): add EtiquetaController and EscaneoController |
| `f1d0457` | 2026-11-10 | refactor: rename all classes to English |
| `bbc9f3e` | 2026-11-10 | feat: purchase price and estimated profit per product |
| `73820ed` | 2026-11-16 | feat: Colombian payment methods and unit cost on each sale |
| `8e40e40` | 2026-11-16 | feat: sales reports by period with Excel export |
| `73eaead` | 2026-11-16 | feat: AI assistant with Google Gemini (free tier) |
| `b01c1f7` | 2026-11-16 | feat: reports and assistant controllers in the bridge |
| `c90bb54` | 2026-11-22 | feat: PostgreSQL and Oracle storage with JDBC DAO and SQL scripts |
| `af4dcb4` | 2026-11-22 | feat: logical deletion, cancellations and the Costs section |
| `9c451dd` | 2026-11-22 | feat: backups stored inside the database |
| `5db4eb6` | 2026-11-22 | feat: action history (who did what and when) |
| `ccd69e1` | 2026-11-22 | feat: credit sales (fiados) with customer payments |
| `462fab0` | 2026-11-22 | feat: several computers on the same database (cloud) |

**Dair José Quintero**

| Commit | Fecha | Mensaje |
|---|---|---|
| `6b42ca7` | 2026-10-27 | build: corregir pom.xml para compilar en cualquier equipo |
| `6e1b185` | 2026-10-27 | feat(util): agregar ValidacionException y utilidades de texto, fecha e IDs |
| `ae99f95` | 2026-10-27 | refactor(model): Producto como Experto en stock, vencimiento y busqueda |
| `01a422d` | 2026-10-27 | feat(model): jerarquia MovimientoInventario con polimorfismo en Entrada y Salida |
| `ca30839` | 2026-10-27 | refactor(model): Venta como Creator de ItemVenta y Experto del total |
| `5fae3ac` | 2026-10-27 | refactor(model): encapsular EntidadConId y aplicar Experto en perfil y catalogo |
| `3229669` | 2026-10-27 | docs: README del proyecto P3 con arquitectura, ejecucion y ramas |
| `dfd6e31` | 2026-10-27 | fix(ui): show toast notifications by setting the SVG class attribute |
| `052c0a0` | 2026-10-27 | feat(ui): add Backups section to create, restore and delete backups |
| `96fa478` | 2026-10-27 | docs: document the backup feature and update the traceability table |
| `10a5d23` | 2026-10-28 | feat(ui): add QR label preview and printable sheet to Productos |
| `7a6389c` | 2026-10-28 | feat(ui): add sale by QR with live camera and cart |
| `a875307` | 2026-10-28 | docs: document the sale by QR feature and update the traceability table |
| `44e8ab5` | 2026-11-10 | feat(ui): add the Bricolage Grotesque font for titles |
| `b041923` | 2026-11-10 | feat(ui): add the TBalance stylesheet in orange and white |
| `0202eb4` | 2026-11-10 | feat(ui): purchase and sale price, profit column and new dashboard |
| `4d0212a` | 2026-11-10 | docs: add screenshots of the redesigned interface |
| `b4fa826` | 2026-11-16 | feat(ui): Outfit font, T logo and high-resolution app icons |
| `8c03c39` | 2026-11-16 | feat(view): open the interface in an Edge app window |
| `c5fe391` | 2026-11-16 | feat(ui): flat interface with a color per business type, Today view and cash book |
| `9f00176` | 2026-11-16 | docs: add screenshots of the v3.4.0 interface |
| `5ee4cb2` | 2026-11-22 | feat: phone as a wireless QR label reader |
| `7307d51` | 2026-11-22 | feat(ui): glass sign-in and windows with the business color |
| `7953303` | 2026-11-22 | docs: screenshots of the v3.5.0 interface |

**Reinaldo Padilla Pedroza**

| Commit | Fecha | Mensaje |
|---|---|---|
| `d46f2b9` | 2026-10-27 | feat(repository): PersistenciaException y JsonFileStore con escritura segura |
| `f9f8543` | 2026-10-27 | chore: eliminar modulo de usuarios sin uso (codigo muerto) |
| `2445053` | 2026-10-27 | refactor(repository): interfaz Repository y JsonRepository generico (OCP, DIP) |
| `1dc9aa6` | 2026-10-27 | refactor(repository): ConfiguracionGlobalRepository sobre JsonFileStore y DirectorioPerfiles |
| `5e6374c` | 2026-10-27 | docs: arquitectura en capas y diagrama de paquetes |
| `21b084b` | 2026-10-27 | fix(view): incluir Tailwind CSS y la fuente en el proyecto para ejecutar sin internet |
| `548c82e` | 2026-10-27 | feat(repository): add ArchivadorZip to compress and extract the data folder |
| `17bd048` | 2026-10-27 | feat(repository): add Respaldo model and RespaldoRepository for backup files |
| `1d4b8a5` | 2026-10-27 | docs: add backup classes to the architecture document |
| `2edf33d` | 2026-10-27 | chore: remove obsolete working-folder rule from .gitignore |
| `959764b` | 2026-10-28 | build: add ZXing and webcam-capture dependencies |
| `2b39064` | 2026-10-28 | feat(util): add GeneradorQR and LectorQR to create and read QR codes |
| `f6e9e76` | 2026-10-28 | feat(repository): add EtiquetaRepository to store QR label images |
| `ae54179` | 2026-10-28 | feat(device): add Camara interface and webcam implementation |
| `e5e2227` | 2026-11-10 | docs: update the diagrams with the English class names |
| `6ff8804` | 2026-11-10 | docs: add phase 1 specification and phase 2 design |
| `48ac0de` | 2026-11-10 | docs: add phases 3 and 4, update traceability and README |
| `a707362` | 2026-11-16 | docs: update the diagrams for v3.4.0 |
| `5bbb0ef` | 2026-11-16 | docs: phases 1 and 2 with payments, reports and assistant |
| `8cfda15` | 2026-11-16 | docs: phases 3 and 4, traceability and README for v3.4.0 |
| `4c3d9cf` | 2026-11-22 | feat: sign-in with owner, administrator and cashier roles |
| `ef5d28c` | 2026-11-22 | feat: the assistant respects the role of the user |
| `251540d` | 2026-11-22 | docs: phases 1 to 4, diagrams and README for v3.5.0 |

### 7.3 Ramas y fusiones

| PR | Rama | Destino | Fecha | Aprobó y fusionó |
|---|---|---|---|---|
| #1 | `feature/dair-modelo-dominio` | `dev` | 2026-10-27 | @rjpadillap |
| #2 | `feature/reinaldo-persistencia-json` | `dev` | 2026-10-27 | @kjhosepmercadom |
| #3 | `feature/kevin-servicios-bridge` | `dev` | 2026-10-27 | @rjpadillap |
| #4 | `dev` | `main` | 2026-10-27 | @rjpadillap |
| #5 | `feature/reinaldo-backup-storage` | `dev` | 2026-10-27 | @kjhosepmercadom |
| #6 | `feature/kevin-backup-service` | `dev` | 2026-10-27 | @djosequintero |
| #7 | `feature/dair-backup-ui` | `dev` | 2026-10-27 | @rjpadillap |
| #8 | `feature/reinaldo-architecture-docs` | `dev` | 2026-10-27 | @kjhosepmercadom |
| #9 | `dev` | `main` | 2026-10-27 | @djosequintero |
| #10 | `feature/reinaldo-qr-reader-camera` | `dev` | 2026-10-28 | @kjhosepmercadom |
| #11 | `feature/kevin-qr-sale-service` | `dev` | 2026-10-28 | @djosequintero |
| #12 | `feature/dair-qr-sale-ui` | `dev` | 2026-10-28 | @rjpadillap |
| #13 | `dev` | `main` | 2026-10-28 | @djosequintero |
| #14 | `feature/kevin-english-names-profit` | `dev` | 2026-11-10 | @djosequintero |
| #15 | `feature/dair-profit-ui-redesign` | `dev` | 2026-11-10 | @rjpadillap |
| #16 | `feature/reinaldo-docs-v3.3.0` | `dev` | 2026-11-10 | @kjhosepmercadom |
| #17 | `dev` | `main` | 2026-11-10 | @djosequintero |
| #18 | `feature/kevin-payments-reports-assistant` | `dev` | 2026-11-16 | @djosequintero |
| #19 | `feature/dair-edge-window-new-ui` | `dev` | 2026-11-16 | @rjpadillap |
| #20 | `feature/reinaldo-docs-v3.4.0` | `dev` | 2026-11-16 | @kjhosepmercadom |
| #21 | `dev` | `main` | 2026-11-16 | @djosequintero |
| #22 | `feature/kevin-database-history-credit` | `dev` | 2026-11-22 | @djosequintero |
| #23 | `feature/dair-glass-ui-phone-reader` | `dev` | 2026-11-22 | @rjpadillap |
| #24 | `feature/reinaldo-roles-docs-v3.5.0` | `dev` | 2026-11-22 | @kjhosepmercadom |
| #25 | `dev` | `main` | 2026-11-22 | @djosequintero |

Las ramas `feature/*` se borran después de fusionarse; en el repositorio quedan `main` y `dev`.

### 7.4 Estado de las tareas

| Integrante | Tarea | Estado | Versión |
|---|---|---|---|
| Kevin Mercado | Importar el código de P2 y configurar el repositorio | Completada | v3.0.0-avance1 |
| Dair Quintero | Modelo de dominio: `Product` y `Sale` como expertos, jerarquía `InventoryMovement`, `IdentifiedEntity` encapsulada | Completada | v3.0.0-avance1 |
| Reinaldo Padilla | Persistencia: `Repository<T>`, `JsonRepository<T>`, `JsonFileStore`, `ProfileDirectory`; quitar código muerto | Completada | v3.0.0-avance1 |
| Kevin Mercado | Servicios con `EntityService<T>`, inyección de dependencias, `AppContext` y `JavaBridge` con controladores | Completada | v3.0.0-avance1 |
| Reinaldo Padilla | Respaldos: `ZipArchiver`, `Backup`, `BackupRepository` | Completada | v3.1.0 |
| Kevin Mercado | `BackupService`, recarga del `AppContext` y `BackupController` | Completada | v3.1.0 |
| Dair Quintero | Sección Respaldos en la interfaz y documentación | Completada | v3.1.0 |
| Reinaldo Padilla | Documentación de arquitectura de respaldos | Completada | v3.1.0 |
| Reinaldo Padilla | Dependencias ZXing y webcam-capture, `QrGenerator`, `QrReader`, `LabelRepository`, `Camera` | Completada | v3.2.0 |
| Kevin Mercado | QR en `Product`, `LabelService`, `QrSaleScanner`, `LabelController`, `ScanController` | Completada | v3.2.0 |
| Dair Quintero | Etiquetas QR en Productos y pantalla Venta por QR | Completada | v3.2.0 |
| Kevin Mercado | Nombres de las clases en inglés y utilidad por producto | Completada | v3.3.0 |
| Dair Quintero | Precio de compra y venta, columna de utilidad e interfaz rediseñada | Completada | v3.3.0 |
| Reinaldo Padilla | Documentación por fases (Fase 1 a 4) | Completada | v3.3.0 |
| Kevin Mercado | Métodos de pago (`PaymentMethod`), informes (`ReportService`, Excel) y asistente (`ia`, `AssistantService`) | Completada | v3.4.0 |
| Dair Quintero | Ventana de Edge, interfaz plana con color por tipo de negocio, pantalla Hoy, cuaderno de caja, Informes, asistente y métodos de pago, capturas | Completada | v3.4.0 |
| Reinaldo Padilla | Documentación v3.4.0: diagramas, requerimientos, clases nuevas y trazabilidad | Completada | v3.4.0 |
| Kevin Mercado | Guardar los datos en PostgreSQL u Oracle (repositorios JDBC, scripts SQL y cambios automáticos del esquema) | Completada | v3.5.0 |
| Kevin Mercado | Borrado lógico (deshabilitar y anular con motivo) y sección Costos | Completada | v3.5.0 |
| Kevin Mercado | Copias de seguridad dentro de la base de datos | Completada | v3.5.0 |
| Kevin Mercado | Historial de acciones (quién hizo qué y cuándo) | Completada | v3.5.0 |
| Kevin Mercado | Fiados: quién debe, abonos y anulación de abonos | Completada | v3.5.0 |
| Kevin Mercado | Base en la nube desde varios computadores: recarga por cambios, stock real al vender y reconexión | Completada | v3.5.0 |
| Dair Quintero | Celular como lector de etiquetas QR por el Wi-Fi del local | Completada | v3.5.0 |
| Dair Quintero | Interfaz: inicio de sesión y ventanas en vidrio, Fiados, Historial, Usuarios, buscador, cobro por cuenta y QR de Bre-B, y carga más rápida | Completada | v3.5.0 |
| Dair Quintero | Capturas de la interfaz v3.5.0 | Completada | v3.5.0 |
| Reinaldo Padilla | Inicio de sesión con roles (dueño, administrador y cajero) | Completada | v3.5.0 |
| Reinaldo Padilla | El asistente respeta el rol y responde quién debe | Completada | v3.5.0 |
| Reinaldo Padilla | Documentación v3.5.0: fases 1 a 4, diagramas, trazabilidad y README | Completada | v3.5.0 |
| Equipo | Procedimientos y funciones en PL/SQL para vender, anular, abonar e informes | Completada | después de v3.5.0 |
| Equipo | Solo Oracle: todas las consultas y los guardados con funciones y procedimientos | Completada | después de v3.5.0 |
| Equipo | Pruebas unitarias con JUnit | Pendiente | siguiente versión |
| Equipo | Conectar una pasarela de pago (hoy solo se registra el medio de pago) | Pendiente | trabajo futuro |
| Equipo | Versión para celular Android | Pendiente | trabajo futuro |
