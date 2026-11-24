# Fase 2 — Arquitectura y diseño

Universidad Popular del Cesar · Programación de Computadores III (SS462) · Docente: Ing. Esp. Alfredo Bautista<br>
Integrantes: Kevin Jhosep Mercado · Reinaldo Padilla Pedroza · Dair José Quintero

[README](../README.md) · [Fase 1](FASE1_Especificacion.md) · [Fase 2](FASE2_Arquitectura_y_Diseno.md) · [Fase 3](FASE3_Desarrollo.md) · [Fase 4](FASE4_Documentacion_Tecnica.md) · [Trazabilidad](TRAZABILIDAD.md)

---

## 1. Diagrama de paquetes organizados por capas

![Diagrama de paquetes](diagramas/paquetes.png)

| Capa | Paquetes | Qué hace |
|---|---|---|
| Presentación | `resources/html`, `view`, `bridge` | La página web que ve el usuario, la ventana que la muestra y los controladores que reciben lo que el usuario hace. |
| Lógica de negocio | `service`, `model` | Las reglas del negocio: vender, registrar mercancía, calcular la deuda de un cliente, revisar permisos. |
| Acceso a datos | `repository`, `ia`, `dispositivo` | Guardar y leer los datos en Oracle (llamando a sus funciones y procedimientos), hablar con el asistente en internet y usar la cámara o el celular. |
| Transversal | `util`, `exception` | Utilidades que usan todas las capas (fechas, textos, códigos QR, cifrado de contraseñas) y los errores del proyecto. |
| Raíz | `sistemainventario` | `Launcher` y `Main` arrancan el programa; `AppContext` crea los objetos y los conecta. |

Cada capa solo usa a la de abajo: la pantalla nunca lee la base de datos directamente, siempre pasa por los servicios.

## 2. Diagrama de clases

El sistema tiene **142 clases**: 115 concretas, 4 abstractas, 13 interfaces, 5 enumeraciones y 5 registros (`record`). Por su tamaño, el diagrama se divide en siete partes. En los diagramas no se dibujan los getters y setters.

### 2.1 Modelo del negocio (`model`, `exception`)

![Diagrama de clases del modelo](diagramas/clases-modelo.png)

### 2.2 Contenido de las copias de seguridad (`repository`)

TBalance ya no guarda los negocios en archivos. Estas clases solo escriben y leen el contenido de una copia de seguridad (el .zip que se guarda en la tabla `RESPALDO`).

![Diagrama de clases de las copias de seguridad](diagramas/clases-persistencia.png)

### 2.3 Acceso a datos con Oracle (`repository`)

![Diagrama de clases de persistencia con base de datos](diagramas/clases-persistencia-bd.png)

### 2.4 Lógica de negocio (`service`)

![Diagrama de clases de servicios](diagramas/clases-servicios.png)

### 2.5 Presentación (`bridge` y raíz)

![Diagrama de clases de presentación](diagramas/clases-presentacion.png)

### 2.6 Ventanas, cámaras y utilidades (`view`, `dispositivo`, `util`)

![Diagrama de clases de ventanas y utilidades](diagramas/clases-ventanas.png)

### 2.7 Informes y asistente (`service`, `ia`, `model`, `repository`)

![Diagrama de clases de informes y asistente](diagramas/clases-informes-asistente.png)

Los diagramas se generan a partir del código; sus fuentes están en [`docs/diagramas`](diagramas) (archivos `.puml` de PlantUML).

### Tabla de identificación de clases

| # | Clase | Tipo | Paquete | Capa | Rol |
|---|---|---|---|---|---|
| 1 | `AppContext` | Concreta | `raíz` | Presentación | Arma la aplicación al arrancar: crea los repositorios y los servicios, y los cambia cuando se abre otro negocio, se restaura una copia o se cambia de base de datos. |
| 2 | `Launcher` | Concreta | `raíz` | Presentación | Punto de entrada para ejecutar TBalance desde IntelliJ o con Maven. |
| 3 | `Main` | Concreta | `raíz` | Presentación | Arranca TBalance: abre la interfaz en Edge o, si no hay, en la ventana de JavaFX. |
| 4 | `Acceso` | Concreta | `model` | Lógica de negocio | Dice a qué negocio entra una persona y con qué rol. |
| 5 | `Anulable` | Interfaz | `model` | Lógica de negocio | Operación que no se borra sino que se anula: ventas, entradas y salidas. |
| 6 | `AssistantSettings` | Registro | `model` | Lógica de negocio | Guarda la clave y el modelo del asistente. |
| 7 | `AuditEntry` | Concreta | `model` | Lógica de negocio | Una acción registrada en el historial: quién, cuándo, en qué negocio y qué hizo. |
| 8 | `Backup` | Concreta | `model` | Lógica de negocio | Información de un archivo de copia de seguridad (.zip) del sistema. |
| 9 | `BusinessType` | Enumeración | `model` | Lógica de negocio | Tipos de negocio que puede tener un Profile. |
| 10 | `Category` | Concreta | `model` | Lógica de negocio | Agrupación lógica de productos dentro de un perfil (por ejemplo "Medicamentos" o "Herramientas"). |
| 11 | `CreditPayment` | Concreta | `model` | Lógica de negocio | Abono de un cliente para pagar lo que quedó debiendo de sus ventas fiadas; no se borra, se anula con motivo. |
| 12 | `CustomerDebt` | Concreta | `model` | Lógica de negocio | Cuenta de un cliente que compró fiado: cuánto se le fió, cuánto ha abonado y cuánto debe. |
| 13 | `DatabaseEngine` | Enumeración | `model` | Lógica de negocio | Base de datos que usa TBalance: Oracle. |
| 14 | `DatabaseSettings` | Registro | `model` | Lógica de negocio | Conexión a Oracle de este computador. |
| 15 | `Deshabilitable` | Interfaz | `model` | Lógica de negocio | Registro que no se borra sino que se deshabilita (borrado lógico). |
| 16 | `GlobalConfig` | Concreta | `model` | Lógica de negocio | Lo que recuerda este computador: qué negocio quedó abierto. |
| 17 | `IdentifiedEntity` | Abstracta | `model` | Lógica de negocio | Clase base de todo lo que se guarda: le da a cada objeto su ID. |
| 18 | `InventoryMovement` | Abstracta | `model` | Lógica de negocio | Movimiento que modifica el stock de un producto (entrada o salida). |
| 19 | `PaymentMethod` | Enumeración | `model` | Lógica de negocio | Métodos de pago que se usan en las tiendas colombianas. |
| 20 | `Permiso` | Enumeración | `model` | Lógica de negocio | Acciones de TBalance que dependen del rol. |
| 21 | `Product` | Concreta | `model` | Lógica de negocio | Producto del inventario de un negocio, con sus precios, stock y vencimiento. |
| 22 | `Profile` | Concreta | `model` | Lógica de negocio | Un negocio: nombre, tipo, color y configuración (por ejemplo, los medios de pago que recibe). |
| 23 | `QrLabel` | Registro | `model` | Lógica de negocio | Datos que se leen de una etiqueta QR de TBalance. |
| 24 | `Rol` | Enumeración | `model` | Lógica de negocio | Rol de una persona dentro de un negocio. |
| 25 | `Sale` | Concreta | `model` | Lógica de negocio | Transacción comercial compuesta por uno o varios SaleItem. |
| 26 | `SaleItem` | Concreta | `model` | Lógica de negocio | Línea de detalle de una Sale: producto, cantidad y precio. |
| 27 | `SalesReport` | Registro | `model` | Lógica de negocio | Informe de ventas de un período: totales, comparación con el período anterior, ventas agrupadas en el tiempo, productos más vendidos y métodos de pago. |
| 28 | `StockEntry` | Concreta | `model` | Lógica de negocio | Ingreso de mercancía al inventario: incrementa el stock del producto. |
| 29 | `StockExit` | Concreta | `model` | Lógica de negocio | Retiro de mercancía que no es una venta (merma, daño, uso interno): reduce el stock del producto. |
| 30 | `Supplier` | Concreta | `model` | Lógica de negocio | Distribuidor o fabricante que abastece productos al negocio. |
| 31 | `User` | Concreta | `model` | Lógica de negocio | Persona que usa TBalance, con su contraseña cifrada y sus accesos a los negocios. |
| 32 | `AssistantException` | Concreta | `exception` | Transversal | Error al hablar con el modelo de lenguaje (sin internet, clave inválida, límite gratuito alcanzado...). |
| 33 | `PersistenceException` | Concreta | `exception` | Transversal | Error al leer o escribir los datos (Oracle o archivos). |
| 34 | `ValidationException` | Concreta | `exception` | Transversal | Error de una regla del negocio o de un dato mal escrito; su mensaje se le muestra al usuario. |
| 35 | `AssistantConfigRepository` | Concreta | `repository` | Acceso a datos | Guarda la clave y el modelo del asistente en ~/.inventariopro/asistente.json. |
| 36 | `AuditJdbcRepository` | Concreta | `repository` | Acceso a datos | Historial de acciones en la tabla AUDITORIA. |
| 37 | `BackupStore` | Interfaz | `repository` | Acceso a datos | Dónde se guardan las copias de seguridad (archivos .zip con los datos en JSON). |
| 38 | `CategoryJdbcRepository` | Concreta | `repository` | Acceso a datos | Categorías de un negocio en la tabla CATEGORIA. |
| 39 | `CreditPaymentJdbcRepository` | Concreta | `repository` | Acceso a datos | Abonos de los clientes que deben, en la tabla ABONO. |
| 40 | `Database` | Concreta | `repository` | Acceso a datos | Conexión con Oracle: llama a las funciones de consulta y a los procedimientos dentro de transacciones. |
| 41 | `DatabaseBackupRepository` | Concreta | `repository` | Acceso a datos | Copias de seguridad dentro de Oracle: cada copia es una fila de la tabla RESPALDO con el .zip en una columna BLOB. |
| 42 | `DatabaseRoutines` | Interfaz | `repository` | Acceso a datos | Operaciones que hace Oracle con sus procedimientos y funciones (vender, anular, abonar, saldo, utilidad, valor del inventario). |
| 43 | `DatabaseSettingsRepository` | Concreta | `repository` | Acceso a datos | Guarda en ~/.inventariopro/basedatos.json cómo se conecta este computador a Oracle (URL, usuario y clave). |
| 44 | `DatabaseStorageProvider` | Concreta | `repository` | Acceso a datos | Repositorios JDBC sobre Oracle; todos comparten una conexión. |
| 45 | `GlobalConfigRepository` | Concreta | `repository` | Acceso a datos | Persiste la GlobalConfig (perfil activo y lista de perfiles) en configuracion-global.json. |
| 46 | `JdbcRepository` | Abstracta | `repository` | Acceso a datos | Repositorio genérico sobre una tabla de Oracle: lee con FN_LISTAR_ y guarda con SP_GUARDAR_ y SP_BORRAR_. |
| 47 | `JsonFileStore` | Concreta | `repository` | Acceso a datos | Lectura y escritura de un archivo de texto JSON en disco. |
| 48 | `JsonRepository` | Concreta | `repository` | Acceso a datos | Guarda una lista de objetos en un archivo JSON; solo se usa dentro de las copias de seguridad. |
| 49 | `JsonStorageProvider` | Concreta | `repository` | Acceso a datos | Repositorios en archivos JSON; solo se usan para el contenido de las copias de seguridad. |
| 50 | `LabelRepository` | Concreta | `repository` | Acceso a datos | Guarda las etiquetas generadas como imágenes PNG en ~/.inventariopro/etiquetas/ y las convierte a Base64 para mostrarlas en la vista. |
| 51 | `OracleDialect` | Concreta | `repository` | Acceso a datos | Separa los scripts de Oracle, revisa si una tabla existe y convierte los errores de RAISE_APPLICATION_ERROR en mensajes. |
| 52 | `OracleRoutines` | Concreta | `repository` | Acceso a datos | Llama con CallableStatement a los procedimientos y funciones de Oracle para vender, anular, abonar e informes. |
| 53 | `PaymentQrRepository` | Concreta | `repository` | Acceso a datos | Imagen del QR de cobro (Bre-B) de cada negocio, en ~/.inventariopro/qr-cobro/.img. |
| 54 | `ProductJdbcRepository` | Concreta | `repository` | Acceso a datos | Productos de un negocio en la tabla PRODUCTO. |
| 55 | `ProfileJdbcRepository` | Concreta | `repository` | Acceso a datos | Perfiles (negocios) en la tabla PERFIL y su configuración en PERFIL_CONFIGURACION. |
| 56 | `ReportExcelWriter` | Concreta | `repository` | Acceso a datos | Guarda un SalesReport como libro de Excel (.xlsx) con formato de informe: encabezado con el negocio y el período, tablas con estilos, filas alternadas, totales con fórmulas, filtros, barras de datos, encabezados fijos y pie de página con el número de página para imprimir. |
| 57 | `Repository` | Interfaz | `repository` | Acceso a datos | Contrato de acceso a datos para cualquier entidad del sistema. |
| 58 | `SaleJdbcRepository` | Concreta | `repository` | Acceso a datos | Ventas de un negocio en las tablas VENTA y DETALLE_VENTA. |
| 59 | `SinConexionStorageProvider` | Concreta | `repository` | Acceso a datos | Lo que usa TBalance mientras no hay conexión con Oracle: todo vacío y sin poder guardar. |
| 60 | `StockEntryJdbcRepository` | Concreta | `repository` | Acceso a datos | Entradas de inventario de un negocio en la tabla ENTRADA. |
| 61 | `StockExitJdbcRepository` | Concreta | `repository` | Acceso a datos | Salidas de inventario de un negocio en la tabla SALIDA. |
| 62 | `StorageProvider` | Interfaz | `repository` | Acceso a datos | Entrega los repositorios de TBalance: los de Oracle, los vacíos sin conexión o los de una copia de seguridad. |
| 63 | `SupplierJdbcRepository` | Concreta | `repository` | Acceso a datos | Proveedores de un negocio en la tabla PROVEEDOR. |
| 64 | `UserJdbcRepository` | Concreta | `repository` | Acceso a datos | Personas en la tabla USUARIO y sus accesos en NEGOCIO_USUARIO (qué negocio y con qué rol). |
| 65 | `ZipArchiver` | Concreta | `repository` | Acceso a datos | Comprime una carpeta en un archivo .zip y la extrae de nuevo. |
| 66 | `GeminiClient` | Concreta | `ia` | Acceso a datos | Cliente de la API de Google Gemini (capa gratuita) con llamadas a funciones. |
| 67 | `LanguageModel` | Interfaz | `ia` | Acceso a datos | Modelo de lenguaje en la nube que usa el asistente. |
| 68 | `SecureConnection` | Concreta | `ia` | Acceso a datos | Prepara el cliente HTTP del asistente para que se conecte como lo hace Edge en el mismo computador. |
| 69 | `AssistantProposal` | Concreta | `service` | Lógica de negocio | Cambio que el asistente propone y que solo se hace si el usuario lo confirma (registrar una venta, una entrada de mercancía, cambiar un precio...). |
| 70 | `AssistantService` | Concreta | `service` | Lógica de negocio | Maneja la conversación con el asistente y guarda lo que propone hasta que el usuario confirma. |
| 71 | `AssistantSettingsService` | Concreta | `service` | Lógica de negocio | Guarda y prueba la configuración del asistente. |
| 72 | `AssistantTools` | Concreta | `service` | Lógica de negocio | Funciones del inventario que el asistente puede usar. |
| 73 | `AuditService` | Concreta | `service` | Lógica de negocio | Anota acciones en el historial y las lista con filtros (persona, fechas, negocio). |
| 74 | `BackupService` | Concreta | `service` | Lógica de negocio | Crea, lista, restaura y elimina las copias de seguridad. |
| 75 | `CategoryService` | Concreta | `service` | Lógica de negocio | Casos de uso de las categorías de un perfil. |
| 76 | `CostService` | Concreta | `service` | Lógica de negocio | Costos del negocio por sus productos: cuánto cuesta cada producto, cuánto costó todo lo que hay en inventario y cuánto se ha comprado (entradas) en un periodo. |
| 77 | `CreditService` | Concreta | `service` | Lógica de negocio | Calcula quién debe y cuánto (ventas fiadas menos abonos) y registra y anula abonos. |
| 78 | `DataSnapshot` | Interfaz | `service` | Lógica de negocio | Cómo se sacan y se reponen los datos para las copias de seguridad. |
| 79 | `DataTransferService` | Concreta | `service` | Lógica de negocio | Copia todos los negocios de un almacenamiento a otro, para las copias de seguridad: de Oracle a los archivos del .zip al crearla, y de esos archivos a Oracle al restaurarla. |
| 80 | `DatabaseSnapshot` | Concreta | `service` | Lógica de negocio | Copias de seguridad de los datos de Oracle. |
| 81 | `EntityService` | Abstracta | `service` | Lógica de negocio | Servicio base para las entidades con CRUD simple (productos, categorías y proveedores). |
| 82 | `GlobalConfigService` | Concreta | `service` | Lógica de negocio | Lectura y actualización de la GlobalConfig. |
| 83 | `InventoryService` | Concreta | `service` | Lógica de negocio | Casos de uso del catálogo de productos: CRUD, búsqueda, filtros y alertas de stock y vencimiento. |
| 84 | `LabelService` | Concreta | `service` | Lógica de negocio | Genera etiquetas con código QR para los productos: una etiqueta individual (vista previa o imagen) y hojas listas para imprimir. |
| 85 | `MovementService` | Concreta | `service` | Lógica de negocio | Registro de entradas y salidas de inventario. |
| 86 | `ProfileService` | Concreta | `service` | Lógica de negocio | Crea, edita, deshabilita, habilita y abre negocios. |
| 87 | `ProfileServices` | Registro | `service` | Lógica de negocio | Agrupa los servicios de un negocio: inventario, categorías, proveedores, movimientos, ventas y fiados. |
| 88 | `ProfileServicesFactory` | Concreta | `service` | Lógica de negocio | Construye los repositorios y servicios de un perfil. |
| 89 | `QrSaleScanner` | Concreta | `service` | Lógica de negocio | Convierte lo que ve la cámara en productos para la venta por QR. |
| 90 | `ReportExportService` | Concreta | `service` | Lógica de negocio | Exporta un informe de ventas a un libro de Excel en la carpeta Documentos/TBalance. |
| 91 | `ReportService` | Concreta | `service` | Lógica de negocio | Calcula los informes de ventas de un período. |
| 92 | `SaleService` | Concreta | `service` | Lógica de negocio | Registro y consulta de ventas. |
| 93 | `SessionService` | Concreta | `service` | Lógica de negocio | Guarda quién inició sesión y cierra la sesión después de 30 minutos sin uso. |
| 94 | `StorageService` | Concreta | `service` | Lógica de negocio | Conecta TBalance con Oracle y prepara la base. |
| 95 | `SupplierService` | Concreta | `service` | Lógica de negocio | Casos de uso de los proveedores de un perfil. |
| 96 | `UserService` | Concreta | `service` | Lógica de negocio | Casos de uso de las personas que usan TBalance: la primera cuenta (el dueño), iniciar sesión, crear usuarios de un negocio, deshabilitarlos y cambiar contraseñas. |
| 97 | `AccessPolicy` | Concreta | `bridge` | Presentación | Revisa en Java, antes de cada llamada de la página, que la persona tenga la sesión abierta y el permiso que pide esa acción en el negocio abierto. |
| 98 | `AssistantController` | Concreta | `bridge` | Presentación | Recibe las acciones del asistente: configurar la clave, conversar y confirmar o descartar lo que propone. |
| 99 | `AuditTrail` | Concreta | `bridge` | Presentación | Decide qué llamadas del puente quedan en el historial y arma el texto de cada una. |
| 100 | `BackupController` | Concreta | `bridge` | Presentación | Recibe las acciones de la pantalla copias de seguridad. |
| 101 | `BridgeInvoker` | Concreta | `bridge` | Presentación | Ejecuta un método público de JavaBridge a partir de su nombre y de sus argumentos en JSON, y devuelve el resultado también en JSON. |
| 102 | `BridgeResponse` | Concreta | `bridge` | Presentación | Formato de las respuestas que el bridge devuelve a JavaScript. |
| 103 | `CatalogController` | Concreta | `bridge` | Presentación | Recibe las acciones de la pantalla categorías y proveedores. |
| 104 | `CostController` | Concreta | `bridge` | Presentación | Entrega a la pantalla los datos de la sección Costos. |
| 105 | `CreditController` | Concreta | `bridge` | Presentación | Recibe las acciones de la sección Fiados: ver quién debe, ver la cuenta de un cliente, registrar y anular abonos. |
| 106 | `DesktopFileOpener` | Concreta | `bridge` | Presentación | Implementación de FileOpener con Desktop. |
| 107 | `FileOpener` | Interfaz | `bridge` | Presentación | Abre un archivo o carpeta con el programa predeterminado del sistema operativo (visor de imágenes, explorador de archivos...). |
| 108 | `InventoryController` | Concreta | `bridge` | Presentación | Recibe las acciones de la pantalla productos y del dashboard. |
| 109 | `JavaBridge` | Concreta | `bridge` | Presentación | Objeto que la página web llama como window.javaBridge; pasa cada pedido al controlador que le corresponde. |
| 110 | `LabelController` | Concreta | `bridge` | Presentación | Recibe las acciones de la pantalla etiquetas con código QR. |
| 111 | `ProfileController` | Concreta | `bridge` | Presentación | Recibe las acciones de la pantalla perfiles: recibe los eventos de la vista, delega en ProfileService y avisa a la vista cuando cambia el estado. |
| 112 | `ReportController` | Concreta | `bridge` | Presentación | Entrega a la pantalla los informes de ventas y los exporta a Excel. |
| 113 | `ScanController` | Concreta | `bridge` | Presentación | Recibe la acción de la pantalla "registrar venta por QR". |
| 114 | `ScreenTransition` | Interfaz | `bridge` | Presentación | Transición visual que hace la ventana de JavaFX por encima de la página HTML. |
| 115 | `SessionController` | Concreta | `bridge` | Presentación | Recibe las acciones del inicio de sesión y de los usuarios de cada negocio. |
| 116 | `SseNotifier` | Concreta | `bridge` | Presentación | Implementación de ViewNotifier para cuando la interfaz se muestra en Edge: envía cada script a la página por un flujo de eventos del servidor (Server-Sent Events) y la página lo ejecuta al recibirlo. |
| 117 | `StorageController` | Concreta | `bridge` | Presentación | Recibe las acciones de "Dónde se guardan los datos": probar la conexión y cambiar de almacenamiento. |
| 118 | `TransactionController` | Concreta | `bridge` | Presentación | Recibe las acciones de la pantalla que mueven stock: entradas, salidas y ventas. |
| 119 | `ViewNotifier` | Interfaz | `bridge` | Presentación | Canal para que Java avise a la vista web de un cambio de estado. |
| 120 | `WebViewNotifier` | Concreta | `bridge` | Presentación | Implementación de ViewNotifier sobre el WebEngine de JavaFX. |
| 121 | `BrowserWindow` | Concreta | `view` | Presentación | Abre la interfaz en una ventana de aplicación de Microsoft Edge (o de Google Chrome si no hay Edge): una ventana propia, sin pestañas ni barra de direcciones, que dibuja la página con la tarjeta gráfica. |
| 122 | `DesktopBrowserApp` | Concreta | `view` | Presentación | Arranca TBalance mostrando la interfaz en una ventana de Edge (modo aplicación) en lugar del visor de JavaFX. |
| 123 | `LocalWebServer` | Concreta | `view` | Presentación | Servidor HTTP local (solo en 127.0.0.1) que entrega la interfaz a Edge y conecta la página con Java. |
| 124 | `SnapshotTransition` | Concreta | `view` | Presentación | Hace las transiciones grandes de la ventana de JavaFX con animaciones de JavaFX. |
| 125 | `WebViewBridgeAdapter` | Concreta | `view` | Presentación | Conecta la página con JavaBridge cuando se usa la ventana de JavaFX. |
| 126 | `WebViewMain` | Concreta | `view` | Presentación | Ventana de JavaFX que muestra la interfaz cuando no hay Edge. |
| 127 | `WindowsTaskbarIcon` | Concreta | `view` | Presentación | Pone el ícono de TBalance en alta resolución en la ventana de Edge (barra de tareas y barra de título de Windows). |
| 128 | `Camera` | Interfaz | `dispositivo` | Acceso a datos | Cámara de la que se toman cuadros para leer códigos QR. |
| 129 | `CameraProvider` | Interfaz | `dispositivo` | Acceso a datos | Cámaras disponibles en el computador. |
| 130 | `PhoneCamera` | Concreta | `dispositivo` | Acceso a datos | Camera que toma las imágenes del celular conectado por Wi-Fi (PhoneLink). |
| 131 | `PhoneLink` | Concreta | `dispositivo` | Acceso a datos | Conexión con el celular por el Wi-Fi del local, para usarlo como lector de etiquetas sin instalar aplicaciones ni usar internet. |
| 132 | `WebcamCamera` | Concreta | `dispositivo` | Acceso a datos | Camera implementada con la webcam del computador mediante la librería webcam-capture. |
| 133 | `AssistantLog` | Concreta | `util` | Transversal | Registro de errores del asistente en ~/.inventariopro/asistente.log, para poder ver qué pasó si no responde. |
| 134 | `DateUtil` | Concreta | `util` | Transversal | Utilidades de fecha usadas por todo el sistema. |
| 135 | `FileUtil` | Concreta | `util` | Transversal | Operaciones con carpetas que usan las copias de seguridad. |
| 136 | `IdGenerator` | Concreta | `util` | Transversal | Generador de identificadores únicos. |
| 137 | `ImageEncoder` | Concreta | `util` | Transversal | Convierte imágenes en texto para mostrarlas en la página. |
| 138 | `JsonUtil` | Concreta | `util` | Transversal | Punto único de configuración de Gson para todo el sistema. |
| 139 | `PasswordHasher` | Concreta | `util` | Transversal | Cifra las contraseñas antes de guardarlas y las comprueba al iniciar sesión. |
| 140 | `QrGenerator` | Concreta | `util` | Transversal | Genera imágenes de códigos QR usando la librería ZXing. |
| 141 | `QrReader` | Concreta | `util` | Transversal | Lee el texto de un código QR que aparece en una imagen (por ejemplo, un cuadro de la cámara). |
| 142 | `TextUtil` | Concreta | `util` | Transversal | Utilidades puras para el manejo de texto. |

### Descripción de cada clase

Para cada clase: su tipo, su responsabilidad, sus atributos y sus métodos públicos. Los getters y setters se agrupan en una sola fila.

#### Paquete `sistemainventario`

##### AppContext

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Arma la aplicación al arrancar: crea los repositorios y los servicios, y los cambia cuando se abre otro negocio, se restaura una copia o se cambia de base de datos. |
| Archivo | [AppContext.java](../src/main/java/sistemainventario/AppContext.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `instancia (static)` | `AppContext` | Instancia (AppContext) |
| `directorioDatos` | `Path` | Directorio datos |
| `gestorRespaldo` | `BackupService` | Gestor respaldo (BackupService) |
| `gestorEtiquetas` | `LabelService` | Gestor etiquetas (LabelService) |
| `ajustesAsistente` | `AssistantSettingsService` | Ajustes asistente (AssistantSettingsService) |
| `exportadorInformes` | `ReportExportService` | Exportador informes (ReportExportService) |
| `almacenamiento` | `StorageService` | Almacenamiento (StorageService) |
| `almacenActual` | `StorageProvider` | Almacen actual (StorageProvider) |
| `avisoAlmacenamiento` | `String` | Aviso almacenamiento |
| `conectado` | `boolean` | Conectado |
| `gestorPerfiles` | `ProfileService` | Gestor perfiles (ProfileService) |
| `serviciosActivos` | `ProfileServices` | Servicios activos (ProfileServices) |
| `usuarios` | `UserService` | Usuarios (UserService) |
| `auditoria` | `AuditService` | Auditoria (AuditService) |
| `sesion` | `SessionService` | Sesion (SessionService) |

**Constantes:** `DIRECTORIO_DATOS`, `CARPETA_ETIQUETAS`, `ARCHIVO_ASISTENTE`, `ARCHIVO_BASE_DATOS`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `recargar()` | `void` | Se conecta de nuevo con Oracle y vuelve a armar los repositorios y servicios |
| `getUsuarios()` | `UserService` | Personas que usan TBalance |
| `getAuditoria()` | `AuditService` | Historial de acciones (quién hizo qué) |
| `iniciarSesion(String usuario, String clave)` | `User` | La persona que entró |
| `crearPrimerDueno(String nombre, String usuario, String clave)` | `User` | Primera vez: crea la cuenta del dueño con acceso a los negocios que ya hay y abre su sesión |
| `cerrarSesion()` | `void` | Cierra la sesión; los servicios del negocio quedan sin cargar hasta que alguien entre |
| `perfilesPermitidos()` | `List<Profile>` | Negocios habilitados a los que entra la persona con sesión abierta |
| `rolActual()` | `Optional<Rol>` | Rol en el negocio abierto, si hay sesión y negocio |
| `puedeCrearNegocios()` | `boolean` | True si la persona puede crear negocios: es dueño de alguno, o es la primera cuenta y todavía no hay negocios |
| `esDuenoDe(String perfilId)` | `boolean` | True si la persona con sesión es dueño de ese negocio |
| `registrarNegocioCreado(String perfilId)` | `void` | Da acceso de dueño a quien acaba de crear un negocio |
| `cambiarAlmacenamiento(DatabaseSettings nueva)` | `String` | Cambia la conexión con Oracle de este computador (la prueba antes de guardarla) |
| `estaConectado()` | `boolean` | True si hay conexión con Oracle |
| `getAjustesBaseDeDatos()` | `DatabaseSettings` | Configuración de la base de datos de este computador |
| `getAlmacenamiento()` | `StorageService` | Servicio que prueba y guarda la configuración de la base |
| `getAlmacenEnUso()` | `String` | "Oracle", o "Sin conexión" si Oracle no respondió |
| `getAvisoAlmacenamiento()` | `String` | Aviso pendiente sobre el almacenamiento (migración o error de conexión), o null |
| `restaurarRespaldo(String nombre)` | `Backup` | Restaura respaldo |
| `getAjustesAsistente()` | `AssistantSettingsService` | Configuración del asistente (clave y modelo, fuera de los datos de los perfiles) |
| `getExportadorInformes()` | `ReportExportService` | Exportación de informes a Excel |
| `get()` *(static)* | `AppContext` | La instancia única, creada la primera vez que se solicita |
| `getServiciosActivos()` | `Optional<ProfileServices>` | Servicios del perfil activo, si lo hay |
| `requerirServiciosActivos()` | `ProfileServices` | Servicios del perfil activo |
| `activarPerfil(String perfilId)` | `void` | Activa un perfil y reconstruye sus servicios (aislamiento de datos) |
| `deshabilitarPerfil(String perfilId)` | `void` | Deshabilita un negocio; si era el activo, quedan cargados los servicios del negocio que lo reemplaza |
| `habilitarPerfil(String perfilId)` | `void` | Habilita perfil |
| `revisarCambiosDeOtrosEquipos()` | `long` | Con base de datos, pregunta si otro equipo guardó cambios |
| `getQrCobro()` | `PaymentQrRepository` | Imágenes del QR de cobro de cada negocio (~/.inventariopro/qr-cobro) |
| `getCarpetaCelular()` | `Path` | Carpeta del certificado para conectar el celular (~/.inventariopro/celular) |
| `getPerfilActivoId()` | `String` | ID del negocio abierto, o null si no hay ninguno |
| Getters y setters | — | `getGestorEtiquetas()`, `getGestorPerfiles()`, `getGestorRespaldo()`, `getSesion()` |

##### Launcher

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Punto de entrada para ejecutar TBalance desde IntelliJ o con Maven. |
| Archivo | [Launcher.java](../src/main/java/sistemainventario/Launcher.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `main(String[] args)` *(static)* | `void` | Arranca el programa |

##### Main

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | hereda de `Application` |
| Responsabilidad | Arranca TBalance: abre la interfaz en Edge o, si no hay, en la ventana de JavaFX. |
| Archivo | [Main.java](../src/main/java/sistemainventario/Main.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `start(Stage ventanaPrincipal)` | `void` | Arranca la ventana |
| `main(String[] args)` *(static)* | `void` | Abre la interfaz en una ventana de Edge (fluida, dibujada con la tarjeta gráfica); si no hay Edge ni Chrome, usa el visor de JavaFX |

#### Paquete `model`

##### Acceso

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Dice a qué negocio entra una persona y con qué rol. |
| Archivo | [Acceso.java](../src/main/java/sistemainventario/model/Acceso.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `perfilId` | `String` | Negocio al que pertenece |
| `rol` | `String` | Rol |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `Acceso()` / `Acceso(String perfilId, Rol rol)` | — | Constructores |
| `getRol()` | `Rol` | El rol (cajero si el dato guardado no se reconoce, por seguridad) |
| Getters y setters | — | `getPerfilId()`, `setRol()` |

##### Anulable

| Campo | Valor |
|---|---|
| Tipo | Interfaz |
| Herencia | — |
| Responsabilidad | Operación que no se borra sino que se anula: ventas, entradas y salidas. |
| Archivo | [Anulable.java](../src/main/java/sistemainventario/model/Anulable.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `VIGENTE` | `String` | Estado de una operación vigente |
| `ANULADA` | `String` | Estado de una operación anulada |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `isAnulada()` *(abstracto)* | `boolean` | True si la operación fue anulada |
| `getFechaAnulacion()` *(abstracto)* | `String` | Fecha y hora de la anulación, o null |
| `getMotivoAnulacion()` *(abstracto)* | `String` | Motivo de la anulación, o null |
| `anular(String fecha, String motivo)` *(abstracto)* | `void` | Marca la operación como anulada |
| `getAnuladaPor()` *(abstracto)* | `String` | Nombre de quien anuló, o null (anulaciones antes de existir el login) |
| `setAnuladaPor(String anuladaPor)` *(abstracto)* | `void` | Set anulada por |

##### AssistantSettings

| Campo | Valor |
|---|---|
| Tipo | Registro |
| Herencia | — |
| Responsabilidad | Guarda la clave y el modelo del asistente. |
| Archivo | [AssistantSettings.java](../src/main/java/sistemainventario/model/AssistantSettings.java) |

**Datos:** `(String clave, String modelo)`

##### AuditEntry

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | hereda de `IdentifiedEntity` |
| Responsabilidad | Una acción registrada en el historial: quién, cuándo, en qué negocio y qué hizo. |
| Archivo | [AuditEntry.java](../src/main/java/sistemainventario/model/AuditEntry.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `fecha` | `String` | Fecha |
| `usuarioId` | `String` | Persona que lo hizo |
| `usuario` | `String` | Usuario |
| `rol` | `String` | Rol |
| `perfilId` | `String` | Negocio al que pertenece |
| `perfil` | `String` | Perfil |
| `accion` | `String` | Accion |
| `detalle` | `String` | Detalle |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `AuditEntry()` | — | Constructor |
| Getters y setters | — | `getAccion()`, `getDetalle()`, `getFecha()`, `getPerfil()`, `getPerfilId()`, `getRol()`, `getUsuario()`, `getUsuarioId()`, `setAccion()`, `setDetalle()`, `setFecha()`, `setPerfil()`, `setPerfilId()`, `setRol()`, `setUsuario()`, `setUsuarioId()` |

##### Backup

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Información de un archivo de copia de seguridad (.zip) del sistema. |
| Archivo | [Backup.java](../src/main/java/sistemainventario/model/Backup.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `nombre` | `String` | Nombre |
| `fecha` | `String` | Fecha |
| `tamanoBytes` | `long` | Tamano bytes |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `Backup(String nombre, String fecha, long tamanoBytes)` | — | Constructor |
| Getters y setters | — | `getFecha()`, `getNombre()`, `getTamanoBytes()` |

##### BusinessType

| Campo | Valor |
|---|---|
| Tipo | Enumeración |
| Herencia | — |
| Responsabilidad | Tipos de negocio que puede tener un Profile. |
| Archivo | [BusinessType.java](../src/main/java/sistemainventario/model/BusinessType.java) |

**Valores:** `BOUTIQUE`, `FARMACIA`, `FERRETERIA`, `SUPERMERCADO`, `TIENDA_BARRIO`, `TECNOLOGIA`, `NINGUNO`

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `etiqueta` | `String` | Texto que se muestra |
| `categoriasPredefinidas` | `List<String>` | Categorias predefinidas |
| `requiereVencimiento` | `boolean` | Requiere vencimiento |
| `sugerencias` | `Suggestions` | Sugerencias |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `BusinessType(String etiqueta, List<String> categorias, boolean requiereVencimiento, Suggestions sugerencias)` | — | Constructor |
| `desdeClave(String clave)` *(static)* | `BusinessType` | Busca un tipo por su nombre de constante o por su etiqueta |
| `tieneCategorias()` | `boolean` | Tiene categorias |
| `requiereControlVencimiento()` | `boolean` | True si los productos de este negocio manejan fecha de vencimiento |
| `getSugerencias()` | `Map<String, String>` | Mapa con las claves producto, categoria y proveedor |
| `describir()` | `Map<String, Object>` | El tipo de negocio sabe describirse para la vista |
| Getters y setters | — | `getCategoriasPredefinidas()`, `getEtiqueta()` |

##### Category

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | hereda de `IdentifiedEntity`; implementa `Deshabilitable` |
| Responsabilidad | Agrupación lógica de productos dentro de un perfil (por ejemplo "Medicamentos" o "Herramientas"). |
| Archivo | [Category.java](../src/main/java/sistemainventario/model/Category.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `nombre` | `String` | Nombre |
| `descripcion` | `String` | Descripción |
| `activo` | `Boolean` | Null en los datos de versiones anteriores, que cuentan como habilitados |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `Category()` / `Category(String id, String nombre, String descripcion)` | — | Constructores |
| `validar()` | `void` | La categoría valida sus propios datos |
| `getEtiqueta()` | `String` | Etiqueta legible para selectores y tablas |
| Getters y setters | — | `getDescripcion()`, `getNombre()`, `isActivo()`, `setActivo()`, `setDescripcion()`, `setNombre()` |

##### CreditPayment

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | hereda de `IdentifiedEntity`; implementa `Anulable` |
| Responsabilidad | Abono de un cliente para pagar lo que quedó debiendo de sus ventas fiadas; no se borra, se anula con motivo. |
| Archivo | [CreditPayment.java](../src/main/java/sistemainventario/model/CreditPayment.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `cliente` | `String` | Nombre del cliente |
| `clienteDocumento` | `String` | Cédula del cliente |
| `monto` | `double` | Valor |
| `metodoPago` | `String` | Cómo pagó |
| `referenciaPago` | `String` | Número de comprobante o voucher |
| `nota` | `String` | Nota |
| `fecha` | `String` | Fecha |
| `usuarioId` | `String` | Persona que lo hizo |
| `registradoPor` | `String` | Quién lo recibió |
| `estado` | `String` | Estado (COMPLETADA o ANULADA) |
| `fechaAnulacion` | `String` | Cuándo se anuló |
| `motivoAnulacion` | `String` | Por qué se anuló |
| `anuladaPor` | `String` | Quién la anuló |

**Constantes:** `PREFIJO_ID`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `CreditPayment()` / `CreditPayment(String clienteDocumento, double monto, String metodoPago)` | — | Constructores |
| `validar()` | `void` | Revisa que el abono tenga cliente, un valor positivo y un medio de pago válido (no se puede abonar "fiando" otra vez) |
| `isAnulada()` | `boolean` | True si la operación fue anulada |
| `getEstado()` | `String` | COMPLETADA o ANULADA |
| `anular(String fecha, String motivo)` | `void` | Marca la operación como anulada |
| `setAnulacion(String fechaAnulacion, String motivoAnulacion)` | `void` | Set anulacion |
| Getters y setters | — | `getAnuladaPor()`, `getCliente()`, `getClienteDocumento()`, `getFecha()`, `getFechaAnulacion()`, `getMetodoPago()`, `getMonto()`, `getMotivoAnulacion()`, `getNota()`, `getReferenciaPago()`, `getRegistradoPor()`, `getUsuarioId()`, `setAnuladaPor()`, `setCliente()`, `setClienteDocumento()`, `setEstado()`, `setFecha()`, `setMetodoPago()`, `setMonto()`, `setNota()`, `setReferenciaPago()`, `setRegistradoPor()`, `setUsuarioId()` |

##### CustomerDebt

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Cuenta de un cliente que compró fiado: cuánto se le fió, cuánto ha abonado y cuánto debe. |
| Archivo | [CustomerDebt.java](../src/main/java/sistemainventario/model/CustomerDebt.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `clave` | `String` | Clave |
| `cliente` | `String` | Nombre del cliente |
| `documento` | `String` | Documento |
| `totalFiado` | `double` | Total fiado |
| `totalAbonado` | `double` | Total abonado |
| `ultimaFecha` | `String` | Ultima fecha |
| `saldo` | `double` | Saldo |
| `ventas` | `List<Sale>` | Ventas |
| `abonos` | `List<CreditPayment>` | Abonos |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `CustomerDebt(String clave, String cliente, String documento)` | — | Constructor |
| `agregarVenta(Sale venta)` | `void` | Agrega venta |
| `agregarAbono(CreditPayment abono)` | `void` | Agrega abono |
| `getSaldo()` | `double` | Lo que debe todavía (0 si ya pagó todo) |
| `estaAlDia()` | `boolean` | True si ya no debe nada |
| Getters y setters | — | `getAbonos()`, `getClave()`, `getCliente()`, `getDocumento()`, `getTotalAbonado()`, `getTotalFiado()`, `getUltimaFecha()`, `getVentas()` |

##### DatabaseEngine

| Campo | Valor |
|---|---|
| Tipo | Enumeración |
| Herencia | — |
| Responsabilidad | Base de datos que usa TBalance: Oracle. |
| Archivo | [DatabaseEngine.java](../src/main/java/sistemainventario/model/DatabaseEngine.java) |

**Valores:** `ORACLE`

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `etiqueta` | `String` | Texto que se muestra |
| `urlPorDefecto` | `String` | Url por defecto |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `DatabaseEngine(String etiqueta, String urlPorDefecto)` | — | Constructor |
| `getEtiqueta()` | `String` | Nombre para mostrar |
| `getUrlPorDefecto()` | `String` | URL JDBC sugerida |

##### DatabaseSettings

| Campo | Valor |
|---|---|
| Tipo | Registro |
| Herencia | — |
| Responsabilidad | Conexión a Oracle de este computador. |
| Archivo | [DatabaseSettings.java](../src/main/java/sistemainventario/model/DatabaseSettings.java) |

**Datos:** `(DatabaseEngine motor, String url, String usuario, String clave)`

**Constantes:** `POR_DEFECTO`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `DatabaseSettings(String url, String usuario, String clave)` | — | Constructor |
| `estaConfigurada()` | `boolean` | True si ya se escribieron los datos de conexión en este computador |

##### Deshabilitable

| Campo | Valor |
|---|---|
| Tipo | Interfaz |
| Herencia | — |
| Responsabilidad | Registro que no se borra sino que se deshabilita (borrado lógico). |
| Archivo | [Deshabilitable.java](../src/main/java/sistemainventario/model/Deshabilitable.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `isActivo()` *(abstracto)* | `boolean` | True si está habilitado (los datos antiguos sin el campo cuentan como habilitados) |
| `setActivo(boolean activo)` *(abstracto)* | `void` | Set activo |

##### GlobalConfig

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Lo que recuerda este computador: qué negocio quedó abierto. |
| Archivo | [GlobalConfig.java](../src/main/java/sistemainventario/model/GlobalConfig.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `perfilActivoId` | `String` | Perfil activo id |
| `version` | `String` | Version |

**Constantes:** `VERSION_ACTUAL`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `tienePerfilActivo()` | `boolean` | True si hay un negocio abierto guardado |
| `getPerfilActivoId()` | `String` | ID del negocio abierto, o null |
| `setPerfilActivoId(String perfilActivoId)` | `void` | Set perfil activo id |
| `getVersion()` | `String` | Versión del formato del archivo |
| `setVersion(String version)` | `void` | Set version |

##### IdentifiedEntity

| Campo | Valor |
|---|---|
| Tipo | Abstracta |
| Herencia | — |
| Responsabilidad | Clase base de todo lo que se guarda: le da a cada objeto su ID. |
| Archivo | [IdentifiedEntity.java](../src/main/java/sistemainventario/model/IdentifiedEntity.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `id` | `String` | Identificador |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `IdentifiedEntity()` / `IdentifiedEntity(String id)` | — | Constructores |
| `tieneId()` | `boolean` | La entidad sabe si ya fue identificada |
| `getEtiqueta()` | `String` | Etiqueta legible para selectores y tablas |
| `equals(Object otro)` | `boolean` | Dos entidades son iguales si son del mismo tipo concreto y comparten ID |
| `hashCode()` | `int` | Código hash según el ID |
| Getters y setters | — | `getId()`, `setId()` |

##### InventoryMovement

| Campo | Valor |
|---|---|
| Tipo | Abstracta |
| Herencia | hereda de `IdentifiedEntity`; implementa `Anulable` |
| Responsabilidad | Movimiento que modifica el stock de un producto (entrada o salida). |
| Archivo | [InventoryMovement.java](../src/main/java/sistemainventario/model/InventoryMovement.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `productoId` | `String` | Producto |
| `cantidad` | `int` | Cantidad |
| `fecha` | `String` | Fecha |
| `fechaRegistro` | `String` | Cuándo se anotó |
| `estado` | `String` | Null en los datos anteriores, que cuentan como vigentes |
| `fechaAnulacion` | `String` | Cuándo se anuló |
| `motivoAnulacion` | `String` | Por qué se anuló |
| `anuladaPor` | `String` | Quién la anuló |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `InventoryMovement()` / `InventoryMovement(String id, String productoId, int cantidad, String fecha)` | — | Constructores |
| `aplicarA(Product producto)` *(abstracto)* | `void` | Aplica el efecto del movimiento sobre el stock del producto |
| `getPrefijoId()` *(abstracto)* | `String` | Prefijo usado para generar el ID (ENT, SAL, ...) |
| `validar()` | `void` | Comprueba los datos mínimos del movimiento |
| `validarFecha(LocalDate hoy)` | `void` | La fecha es la del movimiento real (por ejemplo, el día en que llegó la mercancía), que puede ser anterior al registro pero nunca futura |
| `registrar(String nuevoId, String fechaRegistro)` | `void` | Marca el movimiento como registrado asignándole ID y fecha |
| `getFechaRegistro()` | `String` | Fecha y hora en que se registró en TBalance (puede ser posterior a getFecha) |
| `setFechaRegistro(String fechaRegistro)` | `void` | Set fecha registro |
| `isAnulada()` | `boolean` | True si la operación fue anulada |
| `getEstado()` | `String` | Estado guardado (COMPLETADA o ANULADA) |
| `setEstado(String estado)` | `void` | Set estado |
| `anular(String fecha, String motivo)` | `void` | Marca la operación como anulada |
| `setAnulacion(String fechaAnulacion, String motivoAnulacion)` | `void` | Set anulacion |
| Getters y setters | — | `getAnuladaPor()`, `getCantidad()`, `getFecha()`, `getFechaAnulacion()`, `getMotivoAnulacion()`, `getProductoId()`, `setAnuladaPor()`, `setCantidad()`, `setFecha()`, `setProductoId()` |

##### PaymentMethod

| Campo | Valor |
|---|---|
| Tipo | Enumeración |
| Herencia | — |
| Responsabilidad | Métodos de pago que se usan en las tiendas colombianas. |
| Archivo | [PaymentMethod.java](../src/main/java/sistemainventario/model/PaymentMethod.java) |

**Valores:** `EFECTIVO`, `NEQUI`, `DAVIPLATA`, `BRE_B`, `BANCOLOMBIA`, `TARJETA_DEBITO`, `TARJETA_CREDITO`, `FIADO`

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `etiqueta` | `String` | Texto que se muestra |
| `digital` | `boolean` | Digital |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `PaymentMethod(String etiqueta, boolean digital)` | — | Constructor |
| `getEtiqueta()` | `String` | El texto que se guarda en la venta y se muestra en pantalla |
| `esDigital()` | `boolean` | True si el pago es electrónico y puede tener número de comprobante |
| `desdeTexto(String texto)` *(static)* | `Optional<PaymentMethod>` | Reconoce el método aunque venga escrito de otra forma ("nequi", "transferencia", "tarjeta", "credito"...) |

##### Permiso

| Campo | Valor |
|---|---|
| Tipo | Enumeración |
| Herencia | — |
| Responsabilidad | Acciones de TBalance que dependen del rol. |
| Archivo | [Permiso.java](../src/main/java/sistemainventario/model/Permiso.java) |

**Valores:** `VENDER`, `VER_GANANCIA`, `EDITAR_CATALOGO`, `MOVIMIENTOS`, `ANULAR`, `DESHABILITAR`, `INFORMES`, `COSTOS`, `USUARIOS`, `NEGOCIOS`, `RESPALDOS`, `CONFIGURACION`, `HISTORIAL`

##### Product

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | hereda de `IdentifiedEntity`; implementa `Deshabilitable` |
| Responsabilidad | Producto del inventario de un negocio, con sus precios, stock y vencimiento. |
| Archivo | [Product.java](../src/main/java/sistemainventario/model/Product.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `nombre` | `String` | Nombre |
| `codigo` | `String` | Código |
| `descripcion` | `String` | Descripción |
| `categoria` | `String` | Categoria |
| `categoriaId` | `String` | Categoría |
| `proveedorId` | `String` | Proveedor |
| `precioCompra` | `double` | Precio de compra |
| `precioVenta` | `double` | Precio de venta |
| `stock` | `int` | Unidades disponibles |
| `stockMin` | `int` | Stock mínimo |
| `tipo` | `String` | Tipo |
| `requiereVencimiento` | `boolean` | Requiere vencimiento |
| `fechaVencimiento` | `String` | Fecha de vencimiento |
| `activo` | `Boolean` | Null en los datos de versiones anteriores, que cuentan como habilitados |

**Constantes:** `ESTADO_AGOTADO`, `ESTADO_BAJO_STOCK`, `ESTADO_DISPONIBLE`, `ENCABEZADO_QR`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `Product()` | — | Constructor |
| `validar(LocalDate hoy)` | `void` | Verifica las reglas de negocio del producto antes de guardarlo |
| `getEstado()` | `String` | El estado legible según el stock actual y el mínimo |
| `estaAgotado()` | `boolean` | True si no quedan unidades |
| `estaBajoStock()` | `boolean` | True si quedan unidades pero en el mínimo o por debajo |
| `tieneStockPara(int cantidad)` | `boolean` | True si el stock cubre la cantidad |
| `aumentarStock(int cantidad)` | `void` | Incrementa el stock (entrada de mercancía) |
| `disminuirStock(int cantidad)` | `void` | Descuenta stock (salida o venta) |
| `verificarStockDisponible(int cantidad)` | `void` | Verifica stock disponible |
| `tienePrecioCompra()` | `boolean` | True si se registró el precio de compra (necesario para estimar la utilidad) |
| `getUtilidadUnitaria()` | `double` | Ganancia por unidad: precio de venta menos precio de compra (negativa si se vende a pérdida) |
| `getMargen()` | `double` | Porcentaje de la venta que es ganancia, o 0 si el precio de venta es 0 |
| `getUtilidadEstimadaStock()` | `double` | Utilidad que dejaría vender todo el stock actual a los precios registrados |
| `getFechaVencimientoComoFecha()` | `Optional<LocalDate>` | La fecha de vencimiento si está controlada y es válida |
| `estaVencido(LocalDate hoy)` | `boolean` | True si el producto controla vencimiento y la fecha ya pasó |
| `venceDentroDe(int dias, LocalDate hoy)` | `boolean` | True si vence desde hoy y antes de hoy + dias |
| `coincideCon(String texto)` | `boolean` | True si hay coincidencia (ignorando mayúsculas) |
| `perteneceACategoria(String nombreCategoria)` | `boolean` | True si el producto pertenece a esa categoría |
| `contenidoEtiquetaQR(String perfilId)` | `String` | El producto conoce sus datos, por eso arma el texto que se codifica en su etiqueta QR (formato en QrLabel) |
| `idDesdeEtiquetaQR(String contenido)` *(static)* | `Optional<String>` | Operación inversa de contenidoEtiquetaQRString |
| `getPrecioVentaFormateado()` | `String` | Precio de venta con separador de miles, por ejemplo $ 2.500 |
| `getEtiqueta()` | `String` | Etiqueta legible para selectores y tablas |
| Getters y setters | — | `getCategoria()`, `getCategoriaId()`, `getCodigo()`, `getDescripcion()`, `getFechaVencimiento()`, `getNombre()`, `getPrecioCompra()`, `getPrecioVenta()`, `getProveedorId()`, `getStock()`, `getStockMin()`, `getTipo()`, `isActivo()`, `isRequiereVencimiento()`, `setActivo()`, `setCategoria()`, `setCategoriaId()`, `setCodigo()`, `setDescripcion()`, `setFechaVencimiento()`, `setNombre()`, `setPrecioCompra()`, `setPrecioVenta()`, `setProveedorId()`, `setRequiereVencimiento()`, `setStock()`, `setStockMin()`, `setTipo()` |

##### Profile

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | hereda de `IdentifiedEntity`; implementa `Deshabilitable` |
| Responsabilidad | Un negocio: nombre, tipo, color y configuración (por ejemplo, los medios de pago que recibe). |
| Archivo | [Profile.java](../src/main/java/sistemainventario/model/Profile.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `nombre` | `String` | Nombre |
| `descripcion` | `String` | Descripción |
| `color` | `String` | Color del negocio |
| `tipoNegocio` | `String` | Tipo negocio |
| `fechaCreacion` | `String` | Fecha creacion |
| `configuracion` | `Map<String, String>` | Configuracion |
| `activo` | `Boolean` | Null en los datos de versiones anteriores, que cuentan como habilitados |

**Constantes:** `COLOR_POR_DEFECTO`, `CLAVES_CUENTAS`, `CLAVE_MEDIOS_PAGO`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `Profile()` / `Profile(String id, String nombre, String descripcion, String color, String tipoNegocio)` | — | Constructores |
| `validar()` | `void` | Valida |
| `actualizarDatos(String nuevoNombre, String nuevaDescripcion, String nuevoColor)` | `void` | Actualiza solo los datos editables y conserva la fecha de creación, la configuración y el tipo de negocio (que no puede cambiarse después de crear el perfil) |
| `getPerfilNegocio()` | `BusinessType` | El tipo de negocio como enumeración (NINGUNO si no tiene) |
| `requiereControlVencimiento()` | `boolean` | True si el tipo de negocio exige controlar vencimientos |
| `getConfig(String clave, String defecto)` | `String` | El valor configurado o defecto |
| `setConfig(String clave, String valor)` | `void` | Set config |
| `copiarAjustesDePago(Profile datos)` | `void` | Copia de otro perfil los medios de pago y las cuentas que llegaron al crear o editar el negocio (solo esas claves, recortadas a 60 caracteres) |
| `getMediosPago()` | `Set<String>` | Medios de pago que el negocio acepta |
| `setMediosPago(Collection<String> medios)` | `void` | Guarda los medios de pago activos |
| `aceptaMedioPago(String metodo)` | `boolean` | True si el negocio acepta ese medio |
| `getEtiqueta()` | `String` | Etiqueta legible para selectores y tablas |
| Getters y setters | — | `getColor()`, `getConfiguracion()`, `getDescripcion()`, `getFechaCreacion()`, `getNombre()`, `getTipoNegocio()`, `isActivo()`, `setActivo()`, `setColor()`, `setConfiguracion()`, `setDescripcion()`, `setFechaCreacion()`, `setNombre()`, `setTipoNegocio()` |

##### QrLabel

| Campo | Valor |
|---|---|
| Tipo | Registro |
| Herencia | — |
| Responsabilidad | Datos que se leen de una etiqueta QR de TBalance. |
| Archivo | [QrLabel.java](../src/main/java/sistemainventario/model/QrLabel.java) |

**Datos:** `(String productoId, String perfilId, String precio)`

**Constantes:** `ENCABEZADO`, `PREFIJO_ID`, `PREFIJO_NEGOCIO`, `PREFIJO_PRECIO`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `texto(Product producto, String perfilId)` *(static)* | `String` | Texto que se codifica en el QR |
| `leer(String contenido)` *(static)* | `Optional<QrLabel>` | Los datos de la etiqueta, o vacío si no es una etiqueta de TBalance o le falta el ID (etiqueta dañada o incompleta) |
| `pareceDeTBalance(String texto)` *(static)* | `boolean` | True si empieza como una etiqueta de TBalance (aunque esté incompleta) |
| `esDeOtroNegocio(String perfilActivo)` | `boolean` | True si la etiqueta es de otro negocio (las antiguas, sin negocio, no lo son) |

##### Rol

| Campo | Valor |
|---|---|
| Tipo | Enumeración |
| Herencia | — |
| Responsabilidad | Rol de una persona dentro de un negocio. |
| Archivo | [Rol.java](../src/main/java/sistemainventario/model/Rol.java) |

**Valores:** `DUENO`, `ADMINISTRADOR`, `CAJERO`

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `etiqueta` | `String` | Texto que se muestra |
| `permisos` | `Set<Permiso>` | Permisos |

**Constantes:** `MINUTOS_ANULAR_PROPIA`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `Rol(String etiqueta, Set<Permiso> permisos)` | — | Constructor |
| `getEtiqueta()` | `String` | Nombre para mostrar ("Dueño", "Administrador", "Cajero") |
| `getPermisos()` | `Set<Permiso>` | Permisos del rol |
| `puede(Permiso permiso)` | `boolean` | True si el rol la puede hacer |
| `puedeAsignar(Rol otro)` | `boolean` | True si este rol puede crear o administrar usuarios con otro |
| `desde(String texto)` *(static)* | `Rol` | El rol |

##### Sale

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | hereda de `IdentifiedEntity`; implementa `Anulable` |
| Responsabilidad | Transacción comercial compuesta por uno o varios SaleItem. |
| Archivo | [Sale.java](../src/main/java/sistemainventario/model/Sale.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `cliente` | `String` | Nombre del cliente |
| `clienteDocumento` | `String` | Cédula u otro documento del cliente; obligatoria cuando la venta es fiada |
| `metodoPago` | `String` | Cómo pagó |
| `observacion` | `String` | Observación |
| `referenciaPago` | `String` | Número de comprobante de un pago digital (Nequi, Daviplata, tarjeta...); opcional |
| `fecha` | `String` | Fecha |
| `estado` | `String` | Estado (COMPLETADA o ANULADA) |
| `fechaAnulacion` | `String` | Cuándo se anuló |
| `motivoAnulacion` | `String` | Por qué se anuló |
| `anuladaPor` | `String` | Quién la anuló |
| `usuarioId` | `String` | Quién registró la venta (para que el cajero solo anule las suyas) |
| `vendedor` | `String` | Quién vendió |
| `fechaRegistro` | `String` | Fecha y hora exactas en que se registró (la fecha de la venta es solo el día) |
| `items` | `List<SaleItem>` | Productos de la venta |
| `total` | `double` | Total |

**Constantes:** `PREFIJO_ID`, `ESTADO_COMPLETADA`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `Sale()` / `Sale(String id, String cliente, String metodoPago, String observacion, String fecha)` | — | Constructores |
| `agregarItem(Product producto, int cantidad, double precioUnitario)` | `SaleItem` | Crea y agrega un ítem a partir de un producto |
| `validar()` | `void` | Valida |
| `completar(String nuevoId, String fechaRegistro)` | `void` | Asigna ID, fecha y estado COMPLETADA, y sincroniza el total persistido |
| `calcularTotal()` | `double` | Suma de los subtotales de los ítems |
| `recalcularTotal()` | `void` | Sincroniza los atributos total y subtotal antes de serializar |
| `getTotal()` | `double` | Total de la venta, siempre consistente con sus ítems |
| `getTotalUnidades()` | `int` | Número total de unidades vendidas |
| `cantidadesPorProducto()` | `Map<String, Integer>` | Agrupa las cantidades por producto; útil cuando el mismo producto aparece en más de un ítem |
| `estaCompletada()` | `boolean` | True si la venta está en estado COMPLETADA |
| `normalizarPago()` | `void` | Deja el método de pago con su nombre oficial y revisa sus reglas: un fiado necesita el nombre y la cédula del cliente (para saber quién debe), una venta de contado sin cliente queda como "Consumidor final" y solo los pagos digitales llevan comprobante |
| `getItems()` | `List<SaleItem>` | Vista de solo lectura de los ítems |
| `isAnulada()` | `boolean` | True si la operación fue anulada |
| `anular(String fecha, String motivo)` | `void` | Marca la operación como anulada |
| `setAnulacion(String fechaAnulacion, String motivoAnulacion)` | `void` | Set anulacion |
| `getVendedor()` | `String` | Nombre de quien vendió, o null en ventas de antes del login |
| Getters y setters | — | `getAnuladaPor()`, `getCliente()`, `getClienteDocumento()`, `getEstado()`, `getFecha()`, `getFechaAnulacion()`, `getFechaRegistro()`, `getMetodoPago()`, `getMotivoAnulacion()`, `getObservacion()`, `getReferenciaPago()`, `getUsuarioId()`, `setAnuladaPor()`, `setCliente()`, `setClienteDocumento()`, `setEstado()`, `setFecha()`, `setFechaRegistro()`, `setItems()`, `setMetodoPago()`, `setObservacion()`, `setReferenciaPago()`, `setUsuarioId()`, `setVendedor()` |

##### SaleItem

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Línea de detalle de una Sale: producto, cantidad y precio. |
| Archivo | [SaleItem.java](../src/main/java/sistemainventario/model/SaleItem.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `productoId` | `String` | Producto |
| `nombreProducto` | `String` | Nombre producto |
| `cantidad` | `int` | Cantidad |
| `precioUnitario` | `double` | Precio unitario |
| `subtotal` | `double` | Subtotal |
| `costoUnitario` | `double` | Precio de compra del producto en el momento de la venta |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `SaleItem()` / `SaleItem(String productoId, String nombreProducto, int cantidad, double precioUnitario)` | — | Constructores |
| `validar()` | `void` | Valida |
| `tieneCosto()` | `boolean` | True si la venta guardó el costo del producto |
| `utilidadCon(double costo)` | `double` | Utilidad del ítem: (precio de venta - costo) por cantidad |
| `getSubtotal()` | `double` | Cantidad × precio unitario, siempre calculado |
| `sincronizarSubtotal()` | `void` | Actualiza el atributo persistido con el subtotal calculado |
| Getters y setters | — | `getCantidad()`, `getCostoUnitario()`, `getNombreProducto()`, `getPrecioUnitario()`, `getProductoId()`, `setCantidad()`, `setCostoUnitario()`, `setNombreProducto()`, `setPrecioUnitario()`, `setProductoId()` |

##### SalesReport

| Campo | Valor |
|---|---|
| Tipo | Registro |
| Herencia | — |
| Responsabilidad | Informe de ventas de un período: totales, comparación con el período anterior, ventas agrupadas en el tiempo, productos más vendidos y métodos de pago. |
| Archivo | [SalesReport.java](../src/main/java/sistemainventario/model/SalesReport.java) |

**Datos:** `(String desde, public record SalesReport(String desde, String hasta, String anteriorDesde, String anteriorHasta, String agrupacion, Totals actual, Totals anterior, List serie, List productos, List metodosPago, List ventas)`

##### StockEntry

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | hereda de `InventoryMovement` |
| Responsabilidad | Ingreso de mercancía al inventario: incrementa el stock del producto. |
| Archivo | [StockEntry.java](../src/main/java/sistemainventario/model/StockEntry.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `proveedorId` | `String` | Proveedor |
| `precioUnitario` | `double` | Precio unitario |
| `observacion` | `String` | Observación |

**Constantes:** `PREFIJO_ID`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `StockEntry()` | — | Constructor |
| `aplicarA(Product producto)` | `void` | Una entrada suma unidades al producto |
| `getPrefijoId()` | `String` | Prefijo usado para generar el ID (ENT, SAL, ...) |
| Getters y setters | — | `getObservacion()`, `getPrecioUnitario()`, `getProveedorId()`, `setObservacion()`, `setPrecioUnitario()`, `setProveedorId()` |

##### StockExit

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | hereda de `InventoryMovement` |
| Responsabilidad | Retiro de mercancía que no es una venta (merma, daño, uso interno): reduce el stock del producto. |
| Archivo | [StockExit.java](../src/main/java/sistemainventario/model/StockExit.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `motivo` | `String` | Motivo |
| `tipo` | `String` | Tipo |

**Constantes:** `PREFIJO_ID`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `StockExit()` | — | Constructor |
| `aplicarA(Product producto)` | `void` | Una salida descuenta unidades y valida el stock disponible |
| `getPrefijoId()` | `String` | Prefijo usado para generar el ID (ENT, SAL, ...) |
| Getters y setters | — | `getMotivo()`, `getTipo()`, `setMotivo()`, `setTipo()` |

##### Supplier

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | hereda de `IdentifiedEntity`; implementa `Deshabilitable` |
| Responsabilidad | Distribuidor o fabricante que abastece productos al negocio. |
| Archivo | [Supplier.java](../src/main/java/sistemainventario/model/Supplier.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `nombre` | `String` | Nombre |
| `contacto` | `String` | Contacto |
| `telefono` | `String` | Teléfono |
| `email` | `String` | Correo |
| `direccion` | `String` | Dirección |
| `categoria` | `String` | Categoria |
| `categoriaIds` | `List<String>` | Tipos de producto que surte: IDs de categorías del negocio |
| `activo` | `boolean` | Si está habilitado (borrado lógico) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `Supplier()` | — | Constructor |
| `validar()` | `void` | El proveedor valida sus propios datos |
| `coincideCon(String texto)` | `boolean` | El proveedor sabe si coincide con un texto de búsqueda |
| `getEtiqueta()` | `String` | Etiqueta legible para selectores y tablas |
| `toString()` | `String` | Texto que describe el objeto |
| `getCategoriaIds()` | `List<String>` | IDs de las categorías que surte (nunca null) |
| `setCategoriaIds(List<String> categoriaIds)` | `void` | Set categoria ids |
| Getters y setters | — | `getCategoria()`, `getContacto()`, `getDireccion()`, `getEmail()`, `getNombre()`, `getTelefono()`, `isActivo()`, `setActivo()`, `setCategoria()`, `setContacto()`, `setDireccion()`, `setEmail()`, `setNombre()`, `setTelefono()` |

##### User

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | hereda de `IdentifiedEntity`; implementa `Deshabilitable` |
| Responsabilidad | Persona que usa TBalance, con su contraseña cifrada y sus accesos a los negocios. |
| Archivo | [User.java](../src/main/java/sistemainventario/model/User.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `nombre` | `String` | Nombre |
| `usuario` | `String` | Usuario |
| `clave` | `String` | Clave |
| `activo` | `Boolean` | Si está habilitado (borrado lógico) |
| `intentosFallidos` | `int` | Intentos fallidos |
| `bloqueadoHasta` | `String` | Bloqueado hasta |
| `fechaCreacion` | `String` | Fecha creacion |
| `ultimoIngreso` | `String` | Ultimo ingreso |
| `accesos` | `List<Acceso>` | Accesos |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `User()` | — | Constructor |
| `getUsuario()` | `String` | Nombre de usuario para iniciar sesión, en minúsculas |
| `getClave()` | `String` | Resumen de la contraseña (pbkdf2_sha256$iteraciones$sal$resumen) |
| `getBloqueadoHasta()` | `String` | Fecha y hora hasta la que no puede entrar por intentos fallidos, o null |
| `getAccesos()` | `List<Acceso>` | Accesos a negocios (copia) |
| `rolEn(String perfilId)` | `Optional<Rol>` | La persona conoce sus accesos y responde qué rol tiene en cada negocio |
| `darAcceso(String perfilId, Rol rol)` | `void` | Da o cambia el acceso a un negocio |
| `esDuenoDeAlguno()` | `boolean` | True si es dueño de al menos un negocio |
| Getters y setters | — | `getFechaCreacion()`, `getIntentosFallidos()`, `getNombre()`, `getUltimoIngreso()`, `isActivo()`, `setAccesos()`, `setActivo()`, `setBloqueadoHasta()`, `setClave()`, `setFechaCreacion()`, `setIntentosFallidos()`, `setNombre()`, `setUltimoIngreso()`, `setUsuario()` |

#### Paquete `exception`

##### AssistantException

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | hereda de `RuntimeException` |
| Responsabilidad | Error al hablar con el modelo de lenguaje (sin internet, clave inválida, límite gratuito alcanzado...). |
| Archivo | [AssistantException.java](../src/main/java/sistemainventario/exception/AssistantException.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `AssistantException(String mensaje)` / `AssistantException(String mensaje, Throwable causa)` | — | Constructores |

##### PersistenceException

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | hereda de `RuntimeException` |
| Responsabilidad | Error al leer o escribir los datos (Oracle o archivos). |
| Archivo | [PersistenceException.java](../src/main/java/sistemainventario/exception/PersistenceException.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `PersistenceException(String mensaje, Throwable causa)` | — | Constructor |

##### ValidationException

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | hereda de `RuntimeException` |
| Responsabilidad | Error de una regla del negocio o de un dato mal escrito; su mensaje se le muestra al usuario. |
| Archivo | [ValidationException.java](../src/main/java/sistemainventario/exception/ValidationException.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `ValidationException(String mensaje)` | — | Constructor |

#### Paquete `repository`

##### AssistantConfigRepository

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Guarda la clave y el modelo del asistente en ~/.inventariopro/asistente.json. |
| Archivo | [AssistantConfigRepository.java](../src/main/java/sistemainventario/repository/AssistantConfigRepository.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `archivo` | `Path` | Archivo |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `AssistantConfigRepository(Path archivo)` | — | Constructor |
| `leer()` | `Optional<AssistantSettings>` | La configuración guardada, si existe |
| `guardar(AssistantSettings config)` | `void` | Guarda |
| `borrar()` | `void` | Borra la clave guardada |

##### AuditJdbcRepository

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | hereda de `JdbcRepository` |
| Responsabilidad | Historial de acciones en la tabla AUDITORIA. |
| Archivo | [AuditJdbcRepository.java](../src/main/java/sistemainventario/repository/AuditJdbcRepository.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `AuditJdbcRepository(Database bd)` | — | Constructor |
| `tabla()` | `String` | Nombre de la tabla |
| `funcionListar()` | `String` | Función de Oracle que devuelve las filas, por ejemplo FN_LISTAR_PRODUCTOS |
| `columnas()` | `List<String>` | Columnas que se escriben, sin PERFIL_ID ni ID (mismo orden que los parámetros de SP_GUARDAR) |
| `valores(AuditEntry e)` | `Object[]` | Valores en el mismo orden de columnas |
| `leer(ResultSet fila)` | `AuditEntry` | La entidad |

##### BackupStore

| Campo | Valor |
|---|---|
| Tipo | Interfaz |
| Herencia | — |
| Responsabilidad | Dónde se guardan las copias de seguridad (archivos .zip con los datos en JSON). |
| Archivo | [BackupStore.java](../src/main/java/sistemainventario/repository/BackupStore.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `listar()` *(abstracto)* | `List<Backup>` | Las copias, de la más reciente a la más antigua |
| `existe(String nombre)` *(abstracto)* | `boolean` | True si ya hay una copia con ese nombre |
| `guardar(String nombre, Path zip)` *(abstracto)* | `Backup` | Guarda una copia |
| `leer(String nombre, Path destino)` *(abstracto)* | `void` | Deja el contenido de una copia en un archivo .zip para restaurarla |
| `eliminar(String nombre)` *(abstracto)* | `void` | Elimina |
| `ubicacion()` *(abstracto)* | `String` | Dónde están las copias, para mostrarlo ("C:\\...\\respaldos" o "la base de datos Oracle") |
| `carpeta()` *(abstracto)* | `Path` | La carpeta de las copias, o null si están en una base de datos |

##### CategoryJdbcRepository

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | hereda de `JdbcRepository` |
| Responsabilidad | Categorías de un negocio en la tabla CATEGORIA. |
| Archivo | [CategoryJdbcRepository.java](../src/main/java/sistemainventario/repository/CategoryJdbcRepository.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `CategoryJdbcRepository(Database bd, String perfilId)` | — | Constructor |
| `tabla()` | `String` | Nombre de la tabla |
| `funcionListar()` | `String` | Función de Oracle que devuelve las filas, por ejemplo FN_LISTAR_PRODUCTOS |
| `columnas()` | `List<String>` | Columnas que se escriben, sin PERFIL_ID ni ID (mismo orden que los parámetros de SP_GUARDAR) |
| `valores(Category c)` | `Object[]` | Valores en el mismo orden de columnas |
| `leer(ResultSet fila)` | `Category` | La entidad |

##### CreditPaymentJdbcRepository

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | hereda de `JdbcRepository` |
| Responsabilidad | Abonos de los clientes que deben, en la tabla ABONO. |
| Archivo | [CreditPaymentJdbcRepository.java](../src/main/java/sistemainventario/repository/CreditPaymentJdbcRepository.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `CreditPaymentJdbcRepository(Database bd, String perfilId)` | — | Constructor |
| `tabla()` | `String` | Nombre de la tabla |
| `funcionListar()` | `String` | Función de Oracle que devuelve las filas, por ejemplo FN_LISTAR_PRODUCTOS |
| `columnas()` | `List<String>` | Columnas que se escriben, sin PERFIL_ID ni ID (mismo orden que los parámetros de SP_GUARDAR) |
| `valores(CreditPayment a)` | `Object[]` | Valores en el mismo orden de columnas |
| `leer(ResultSet fila)` | `CreditPayment` | La entidad |

##### Database

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | implementa `AutoCloseable` |
| Responsabilidad | Conexión con Oracle: llama a las funciones de consulta y a los procedimientos dentro de transacciones. |
| Archivo | [Database.java](../src/main/java/sistemainventario/repository/Database.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `config` | `DatabaseSettings` | Config (DatabaseSettings) |
| `oracle` | `OracleDialect` | Oracle (OracleDialect) |
| `conexion` | `Connection` | Conexion |
| `ultimoUso` | `long` | Ultimo uso |
| `seguimiento` | `Boolean` | Null mientras no se sepa si la base ya tiene todo en Oracle (cambio v9) |
| `versionConocida` | `long` | Version conocida |
| `generacion` | `long` | Generacion |
| `ultimaRevision` | `long` | Ultima revision |

**Constantes:** `SCRIPTS_INICIALES`, `VERSION_TODO_EN_ORACLE`, `CARPETA`, `SEGUNDOS_VALIDACION`, `VALIDAR_TRAS_MS`, `REVISAR_CAMBIOS_CADA_MS`, `CLAVE_CAMBIOS`, `CURSOR`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `Database(DatabaseSettings config)` | — | Constructor |
| `nombreMotor()` | `String` | "Oracle" |
| `probar()` | `void` | Abre la conexión (lanza una excepción con un mensaje claro si no se puede) |
| `tieneTablas()` | `boolean` | True si ya existen las tablas de TBalance |
| `versionEsquema()` | `int` | Versión del esquema: 0 sin tablas, 1 con las tablas iniciales y después la última anotada en ESQUEMA_VERSION |
| `versionActual()` *(static)* | `int` | Versión a la que este programa deja el esquema |
| `actualizarEsquema()` | `int` | Crea las tablas si no existen y aplica, en orden y una sola vez, los cambios que falten (tablas nuevas, funciones y procedimientos) |
| `estaVacia()` | `boolean` | True si no hay ningún negocio (FN_CONTAR_NEGOCIOS) |
| `contarNegocios()` | `int` | Cuántos negocios hay, o -1 si la base todavía no tiene las funciones de consulta |
| `ejecutarScript(String archivo)` | `void` | Ejecuta un script de sql/oracle dentro de una transacción |
| `llamar(String funcion, LectorDeFila<T> lector, Object... parametros)` | `List<T>` | Llama a una función de Oracle que devuelve un cursor y lee sus filas |
| `llamar(Connection conexion, String funcion, LectorDeFila<T> lector, Object... parametros)` | `List<T>` | Igual que llamarString,, dentro de una transacción |
| `llamarNumero(String funcion, Object... parametros)` | `long` | Llama a una función de Oracle que devuelve un número entero |
| `llamarDecimal(String funcion, Object... parametros)` | `double` | Llama a una función de Oracle que devuelve un número con decimales |
| `procedimiento(Connection conexion, String procedimiento, Object... parametros)` *(static)* | `void` | Llama a un procedimiento almacenado dentro de una transacción |
| `llamar(Trabajo<T> trabajo)` | `T` | Llama a procedimientos que cambian datos por su cuenta (por ejemplo, SP_REGISTRAR_VENTA) |
| `transaccion(Trabajo<T> trabajo)` | `T` | Ejecuta un trabajo en una transacción: COMMIT si termina bien, ROLLBACK si falla |
| `traducir(SQLException error, String accion)` | `RuntimeException` | Convierte un error de Oracle en un mensaje para el usuario |
| `revisarCambios(boolean ahora)` | `long` | Pregunta a Oracle si otro computador guardó algo (FN_VERSION_CAMBIOS) |
| `generacion()` | `long` | El número actual de revisarCambiosboolean sin preguntar a la base |
| `close()` | `void` | Cierra la conexión (se vuelve a abrir sola si se necesita) |
| `asignarUno(PreparedStatement sentencia, int posicion, Object valor)` *(static)* | `void` | Asigna un parámetro de una llamada |

##### DatabaseBackupRepository

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | implementa `BackupStore` |
| Responsabilidad | Copias de seguridad dentro de Oracle: cada copia es una fila de la tabla RESPALDO con el .zip en una columna BLOB. |
| Archivo | [DatabaseBackupRepository.java](../src/main/java/sistemainventario/repository/DatabaseBackupRepository.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `bd` | `Database` | Conexión a la base de datos |

**Constantes:** `NOMBRE_VALIDO`, `FORMATO_FECHA`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `DatabaseBackupRepository(Database bd)` | — | Constructor |
| `listar()` | `List<Backup>` | Las copias, de la más reciente a la más antigua |
| `existe(String nombre)` | `boolean` | True si ya hay una copia con ese nombre |
| `guardar(String nombre, Path zip)` | `Backup` | Guarda una copia |
| `leer(String nombre, Path destino)` | `void` | Deja el contenido de una copia en un archivo .zip para restaurarla |
| `eliminar(String nombre)` | `void` | Elimina |
| `ubicacion()` | `String` | Dónde están las copias, para mostrarlo ("C:\\...\\respaldos" o "la base de datos Oracle") |
| `carpeta()` | `Path` | La carpeta de las copias, o null si están en una base de datos |

##### DatabaseRoutines

| Campo | Valor |
|---|---|
| Tipo | Interfaz |
| Herencia | — |
| Responsabilidad | Operaciones que hace Oracle con sus procedimientos y funciones (vender, anular, abonar, saldo, utilidad, valor del inventario). |
| Archivo | [DatabaseRoutines.java](../src/main/java/sistemainventario/repository/DatabaseRoutines.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `registrarVenta(Sale venta)` *(abstracto)* | `double` | SP_REGISTRAR_VENTA: revisa el stock, guarda la venta con su detalle y descuenta las unidades, todo en una transacción |
| `anularVenta(String ventaId, String motivo, AuditEntry historial)` *(abstracto)* | `void` | SP_ANULAR_VENTA: anula la venta, devuelve el stock y la anota en el historial |
| `registrarAbono(CreditPayment abono)` *(abstracto)* | `String` | SP_REGISTRAR_ABONO: revisa que no pase de la deuda y guarda el abono |
| `saldoCliente(String claveCliente)` *(abstracto)* | `double` | FN_SALDO_CLIENTE |
| `valorInventario()` *(abstracto)* | `double` | FN_VALOR_INVENTARIO: cuánto costó lo que hay en inventario |
| `utilidadPeriodo(LocalDate desde, LocalDate hasta)` *(abstracto)* | `double` | FN_UTILIDAD_PERIODO |

##### DatabaseSettingsRepository

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Guarda en ~/.inventariopro/basedatos.json cómo se conecta este computador a Oracle (URL, usuario y clave). |
| Archivo | [DatabaseSettingsRepository.java](../src/main/java/sistemainventario/repository/DatabaseSettingsRepository.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `archivo` | `Path` | Archivo |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `DatabaseSettingsRepository(Path archivo)` | — | Constructor |
| `leer()` | `DatabaseSettings` | La configuración guardada o, si no hay, la de Oracle local sin clave |
| `guardar(DatabaseSettings config)` | `void` | Guarda |

##### DatabaseStorageProvider

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | implementa `StorageProvider` |
| Responsabilidad | Repositorios JDBC sobre Oracle; todos comparten una conexión. |
| Archivo | [DatabaseStorageProvider.java](../src/main/java/sistemainventario/repository/DatabaseStorageProvider.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `bd` | `Database` | Conexión a la base de datos |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `DatabaseStorageProvider(Database bd)` | — | Constructor |
| `getBaseDeDatos()` | `Database` | La conexión, para crear tablas o copiar datos |
| `nombre()` | `String` | Nombre para mostrar ("Oracle" o "Sin conexión") |
| `perfiles()` | `Repository<Profile>` | Repositorio de perfiles |
| `usuarios()` | `Repository<User>` | Personas que usan TBalance, con sus accesos a los negocios |
| `auditoria()` | `Repository<AuditEntry>` | Historial de acciones (quién hizo qué) |
| `productos(String perfilId)` | `Repository<Product>` | Repositorio de productos del negocio |
| `categorias(String perfilId)` | `Repository<Category>` | Repositorio de categorías del negocio |
| `proveedores(String perfilId)` | `Repository<Supplier>` | Repositorio de proveedores del negocio |
| `entradas(String perfilId)` | `Repository<StockEntry>` | Repositorio de entradas del negocio |
| `abonos(String perfilId)` | `Repository<CreditPayment>` | Abonos de los clientes que deben (fiados) |
| `salidas(String perfilId)` | `Repository<StockExit>` | Repositorio de salidas del negocio |
| `ventas(String perfilId)` | `Repository<Sale>` | Repositorio de ventas del negocio |
| `rutinas(String perfilId)` | `Optional<DatabaseRoutines>` | Los procedimientos y funciones de la base, si el motor los tiene (vacío mientras no hay conexión) |
| `close()` | `void` | Libera la conexión, si la hay |

##### GlobalConfigRepository

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Persiste la GlobalConfig (perfil activo y lista de perfiles) en configuracion-global.json. |
| Archivo | [GlobalConfigRepository.java](../src/main/java/sistemainventario/repository/GlobalConfigRepository.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `almacen` | `JsonFileStore` | Almacen (JsonFileStore) |

**Constantes:** `ARCHIVO`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `GlobalConfigRepository(Path directorioBase)` | — | Constructor |
| `existe()` | `boolean` | True si el archivo de configuración ya fue creado |
| `cargar()` | `GlobalConfig` | Carga la configuración guardada |
| `guardar(GlobalConfig configuracion)` | `void` | Guarda |

##### JdbcRepository

| Campo | Valor |
|---|---|
| Tipo | Abstracta |
| Herencia | implementa `Repository` |
| Responsabilidad | Repositorio genérico sobre una tabla de Oracle: lee con FN_LISTAR_ y guarda con SP_GUARDAR_ y SP_BORRAR_. |
| Archivo | [JdbcRepository.java](../src/main/java/sistemainventario/repository/JdbcRepository.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `bd` | `Database` | Conexión a la base de datos |
| `perfilId` | `String` | Negocio al que pertenece |
| `cache` | `List<T>` | Copia en memoria de los registros |
| `generacionCache` | `long` | Generación de cambios de otros equipos con la que se cargó cache |

**Constantes:** `FECHA_HORA`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `JdbcRepository(Database bd, String perfilId)` | — | Constructor |
| `tabla()` *(abstracto)* | `String` | Nombre de la tabla |
| `columnas()` *(abstracto)* | `List<String>` | Columnas que se escriben, sin PERFIL_ID ni ID (mismo orden que los parámetros de SP_GUARDAR) |
| `funcionListar()` *(abstracto)* | `String` | Función de Oracle que devuelve las filas, por ejemplo FN_LISTAR_PRODUCTOS |
| `valores(T entidad)` *(abstracto)* | `Object[]` | Valores en el mismo orden de columnas |
| `leer(ResultSet fila)` *(abstracto)* | `T` | La entidad |
| `completar(List<T> entidades)` | `void` | Completa las entidades leídas con datos de otras tablas (por ejemplo el detalle de las ventas) |
| `guardarHijos(Connection conexion, T entidad, boolean nueva)` | `void` | Escribe los datos de tablas hijas después del INSERT o del UPDATE |
| `refrescar()` | `void` | Pregunta ya a la base si otro equipo cambió algo y, si es así, recarga |
| `listarTodos()` | `List<T>` | Copia de todas las entidades almacenadas |
| `buscarPorId(String id)` | `Optional<T>` | La entidad, o vacío si no existe |
| `existe(String id)` | `boolean` | True si existe una entidad con ese ID |
| `filtrar(Predicate<T> condicion)` | `List<T>` | Entidades que cumplen la condición |
| `agregar(T entidad)` | `void` | Agrega una entidad nueva y persiste el cambio |
| `actualizar(T entidad)` | `boolean` | Reemplaza la entidad que tenga el mismo ID y persiste el cambio |
| `eliminar(String id)` | `boolean` | True si existía y fue eliminada |
| `siguienteIdNumerico()` | `String` | El siguiente ID numérico disponible (mayor ID + 1) |
| `porPerfil()` | `boolean` | True si la tabla pertenece a un negocio (tiene PERFIL_ID) |
| `referenciaValida(String funcionExiste, String id)` | `Object` | El ID si existe en ese negocio; si no, un nulo (para no romper la llave foránea) |
| `texto(String valor)` *(static)* | `Object` | Texto o nulo de tipo VARCHAR |
| `dia(String iso)` *(static)* | `Object` | Fecha (columna DATE) a partir de "yyyy-MM-dd", o nulo |
| `momento(String iso)` *(static)* | `Object` | Fecha y hora (columna TIMESTAMP) a partir de "yyyy-MM-dd" o "yyyy-MM-ddTHH:mm:ss", o nulo |
| `leerDia(ResultSet fila, String columna)` *(static)* | `String` | La columna DATE como "yyyy-MM-dd" o null |
| `leerMomento(ResultSet fila, String columna)` *(static)* | `String` | La columna TIMESTAMP como "yyyy-MM-ddTHH:mm:ss", o solo "yyyy-MM-dd" si la hora es 00:00:00 (así quedan igual las fechas que se guardaron sin hora) |
| `leerNumero(ResultSet fila, String columna)` *(static)* | `double` | La columna numérica como double (0 si es nula) |

##### JsonFileStore

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Lectura y escritura de un archivo de texto JSON en disco. |
| Archivo | [JsonFileStore.java](../src/main/java/sistemainventario/repository/JsonFileStore.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `archivo` | `Path` | Archivo |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `JsonFileStore(Path archivo)` | — | Constructor |
| `existe()` | `boolean` | True si el archivo existe en disco |
| `leer()` | `Optional<String>` | El contenido del archivo, o vacío si todavía no existe |
| `escribir(String contenido)` | `void` | Reemplaza el contenido del archivo de forma segura |
| `crearDirectorio(Path directorio)` *(static)* | `void` | Crea una carpeta (y sus padres) si no existe |
| Getters y setters | — | `getArchivo()` |

##### JsonRepository

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | implementa `Repository` |
| Responsabilidad | Guarda una lista de objetos en un archivo JSON; solo se usa dentro de las copias de seguridad. |
| Archivo | [JsonRepository.java](../src/main/java/sistemainventario/repository/JsonRepository.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `almacen` | `JsonFileStore` | Almacen (JsonFileStore) |
| `tipo` | `Class<T>` | Tipo |
| `cache` | `List<T>` | Copia en memoria de los registros |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `JsonRepository(Path directorio, String nombreArchivo, Class<T> tipo)` | — | Constructor |
| `listarTodos()` | `List<T>` | Copia de todas las entidades almacenadas |
| `buscarPorId(String id)` | `Optional<T>` | La entidad, o vacío si no existe |
| `existe(String id)` | `boolean` | True si existe una entidad con ese ID |
| `filtrar(Predicate<T> condicion)` | `List<T>` | Entidades que cumplen la condición |
| `agregar(T entidad)` | `void` | Agrega una entidad nueva y persiste el cambio |
| `actualizar(T entidad)` | `boolean` | Reemplaza la entidad que tenga el mismo ID y persiste el cambio |
| `eliminar(String id)` | `boolean` | True si existía y fue eliminada |
| `siguienteIdNumerico()` | `String` | El siguiente ID numérico disponible (mayor ID + 1) |
| `getArchivo()` | `Path` | Ruta del archivo JSON administrado |

##### JsonStorageProvider

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | implementa `StorageProvider` |
| Responsabilidad | Repositorios en archivos JSON; solo se usan para el contenido de las copias de seguridad. |
| Archivo | [JsonStorageProvider.java](../src/main/java/sistemainventario/repository/JsonStorageProvider.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `carpeta` | `Path` | Carpeta |

**Constantes:** `ARCHIVO_PERFILES`, `ARCHIVO_USUARIOS`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `JsonStorageProvider(Path carpeta)` | — | Constructor |
| `nombre()` | `String` | Nombre para mostrar ("Oracle" o "Sin conexión") |
| `perfiles()` | `Repository<Profile>` | Repositorio de perfiles |
| `usuarios()` | `Repository<User>` | Personas que usan TBalance, con sus accesos a los negocios |
| `auditoria()` | `Repository<AuditEntry>` | Historial de acciones (quién hizo qué) |
| `productos(String perfilId)` | `Repository<Product>` | Repositorio de productos del negocio |
| `categorias(String perfilId)` | `Repository<Category>` | Repositorio de categorías del negocio |
| `proveedores(String perfilId)` | `Repository<Supplier>` | Repositorio de proveedores del negocio |
| `entradas(String perfilId)` | `Repository<StockEntry>` | Repositorio de entradas del negocio |
| `abonos(String perfilId)` | `Repository<CreditPayment>` | Abonos de los clientes que deben (fiados) |
| `salidas(String perfilId)` | `Repository<StockExit>` | Repositorio de salidas del negocio |
| `ventas(String perfilId)` | `Repository<Sale>` | Repositorio de ventas del negocio |

##### LabelRepository

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Guarda las etiquetas generadas como imágenes PNG en ~/.inventariopro/etiquetas/ y las convierte a Base64 para mostrarlas en la vista. |
| Archivo | [LabelRepository.java](../src/main/java/sistemainventario/repository/LabelRepository.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `carpeta` | `Path` | Carpeta |

**Constantes:** `NOMBRE_VALIDO`, `FORMATO`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `LabelRepository(Path carpeta)` | — | Constructor |
| `guardar(String nombre, BufferedImage imagen)` | `Path` | Ruta del archivo guardado |
| `aDataUri(BufferedImage imagen)` | `String` | La imagen como data:image/png;base64,... para usarla en un |
| Getters y setters | — | `getCarpeta()` |

##### OracleDialect

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Separa los scripts de Oracle, revisa si una tabla existe y convierte los errores de RAISE_APPLICATION_ERROR en mensajes. |
| Archivo | [OracleDialect.java](../src/main/java/sistemainventario/repository/OracleDialect.java) |

**Constantes:** `COMANDO_SQLPLUS`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `tablaExiste(Connection conexion, String tabla)` | `boolean` | True si la tabla existe en el esquema del usuario (USER_TABLES) |
| `separarSentencias(String script)` | `List<String>` | Sentencias listas para ejecutar, sin ";" ni "/" al final |
| `mensajeDeDisparador(SQLException error)` | `Optional<String>` | El mensaje de RAISE_APPLICATION_ERROR (códigos -20000 a -20999), si lo es |
| `erroresDeRutinas(Connection conexion)` | `Optional<String>` | Revisa si alguna función o procedimiento de TBalance quedó con errores (USER_ERRORS) |

##### OracleRoutines

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | implementa `DatabaseRoutines` |
| Responsabilidad | Llama con CallableStatement a los procedimientos y funciones de Oracle para vender, anular, abonar e informes. |
| Archivo | [OracleRoutines.java](../src/main/java/sistemainventario/repository/OracleRoutines.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `bd` | `Database` | Conexión a la base de datos |
| `perfilId` | `String` | Negocio al que pertenece |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `OracleRoutines(Database bd, String perfilId)` | — | Constructor |
| `registrarVenta(Sale venta)` | `double` | SP_REGISTRAR_VENTA: revisa el stock, guarda la venta con su detalle y descuenta las unidades, todo en una transacción |
| `anularVenta(String ventaId, String motivo, AuditEntry h)` | `void` | SP_ANULAR_VENTA: anula la venta, devuelve el stock y la anota en el historial |
| `registrarAbono(CreditPayment a)` | `String` | SP_REGISTRAR_ABONO: revisa que no pase de la deuda y guarda el abono |
| `saldoCliente(String claveCliente)` | `double` | FN_SALDO_CLIENTE |
| `valorInventario()` | `double` | FN_VALOR_INVENTARIO: cuánto costó lo que hay en inventario |
| `utilidadPeriodo(LocalDate desde, LocalDate hasta)` | `double` | FN_UTILIDAD_PERIODO |
| `itemsJson(Sale venta)` *(static)* | `String` | Los productos de la venta como texto JSON, para leerlos con JSON_TABLE |

##### PaymentQrRepository

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Imagen del QR de cobro (Bre-B) de cada negocio, en ~/.inventariopro/qr-cobro/.img. |
| Archivo | [PaymentQrRepository.java](../src/main/java/sistemainventario/repository/PaymentQrRepository.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `carpeta` | `Path` | Carpeta |

**Constantes:** `TAMANO_MAXIMO`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `PaymentQrRepository(Path carpeta)` | — | Constructor |
| `guardar(String perfilId, byte[] imagen)` | `void` | Guarda |
| `leerComoDataUrl(String perfilId)` | `Optional<String>` | La imagen como "data:image/...;base64,...", si el negocio subió una |
| `eliminar(String perfilId)` | `void` | Elimina |

##### ProductJdbcRepository

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | hereda de `JdbcRepository` |
| Responsabilidad | Productos de un negocio en la tabla PRODUCTO. |
| Archivo | [ProductJdbcRepository.java](../src/main/java/sistemainventario/repository/ProductJdbcRepository.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `ProductJdbcRepository(Database bd, String perfilId)` | — | Constructor |
| `tabla()` | `String` | Nombre de la tabla |
| `funcionListar()` | `String` | Función de Oracle que devuelve las filas, por ejemplo FN_LISTAR_PRODUCTOS |
| `columnas()` | `List<String>` | Columnas que se escriben, sin PERFIL_ID ni ID (mismo orden que los parámetros de SP_GUARDAR) |
| `valores(Product p)` | `Object[]` | Valores en el mismo orden de columnas |
| `leer(ResultSet fila)` | `Product` | La entidad |

##### ProfileJdbcRepository

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | hereda de `JdbcRepository` |
| Responsabilidad | Perfiles (negocios) en la tabla PERFIL y su configuración en PERFIL_CONFIGURACION. |
| Archivo | [ProfileJdbcRepository.java](../src/main/java/sistemainventario/repository/ProfileJdbcRepository.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `ProfileJdbcRepository(Database bd)` | — | Constructor |
| `tabla()` | `String` | Nombre de la tabla |
| `funcionListar()` | `String` | Función de Oracle que devuelve las filas, por ejemplo FN_LISTAR_PRODUCTOS |
| `columnas()` | `List<String>` | Columnas que se escriben, sin PERFIL_ID ni ID (mismo orden que los parámetros de SP_GUARDAR) |
| `valores(Profile p)` | `Object[]` | Valores en el mismo orden de columnas |
| `leer(ResultSet fila)` | `Profile` | La entidad |
| `completar(List<Profile> perfiles)` | `void` | Completa las entidades leídas con datos de otras tablas (por ejemplo el detalle de las ventas) |
| `guardarHijos(Connection conexion, Profile perfil, boolean nuevo)` | `void` | Escribe los datos de tablas hijas después del INSERT o del UPDATE |

##### ReportExcelWriter

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Guarda un SalesReport como libro de Excel (.xlsx) con formato de informe: encabezado con el negocio y el período, tablas con estilos, filas alternadas, totales con fórmulas, filtros, barras de datos, encabezados fijos y pie de página con el número de página para imprimir. |
| Archivo | [ReportExcelWriter.java](../src/main/java/sistemainventario/repository/ReportExcelWriter.java) |

**Constantes:** `TITULO`, `SUBTITULO`, `ENCABEZADO`, `TEXTO`, `DINERO`, `ENTERO`, `PORCENTAJE`, `VARIACION`, `FECHA`, `ALTERNA`, `TOTAL_TEXTO`, `TOTAL_DINERO`, `TOTAL_ENTERO`, `TOTAL_PORCENTAJE`, `NOTA`, `INDICADOR`, `DIA`, `COLUMNAS`, `CABECERA`, `RELACIONES_RAIZ`, `ESTILOS`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `escribir(SalesReport informe, String negocio, Path destino)` | `void` | Escribe |

##### Repository

| Campo | Valor |
|---|---|
| Tipo | Interfaz |
| Herencia | — |
| Responsabilidad | Contrato de acceso a datos para cualquier entidad del sistema. |
| Archivo | [Repository.java](../src/main/java/sistemainventario/repository/Repository.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `listarTodos()` *(abstracto)* | `List<T>` | Copia de todas las entidades almacenadas |
| `buscarPorId(String id)` *(abstracto)* | `Optional<T>` | La entidad, o vacío si no existe |
| `existe(String id)` *(abstracto)* | `boolean` | True si existe una entidad con ese ID |
| `filtrar(Predicate<T> condicion)` *(abstracto)* | `List<T>` | Entidades que cumplen la condición |
| `agregar(T entidad)` *(abstracto)* | `void` | Agrega una entidad nueva y persiste el cambio |
| `actualizar(T entidad)` *(abstracto)* | `boolean` | Reemplaza la entidad que tenga el mismo ID y persiste el cambio |
| `eliminar(String id)` *(abstracto)* | `boolean` | True si existía y fue eliminada |
| `siguienteIdNumerico()` *(abstracto)* | `String` | El siguiente ID numérico disponible (mayor ID + 1) |
| `refrescar()` | `void` | Vuelve a leer los datos si otro equipo los cambió |

##### SaleJdbcRepository

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | hereda de `JdbcRepository` |
| Responsabilidad | Ventas de un negocio en las tablas VENTA y DETALLE_VENTA. |
| Archivo | [SaleJdbcRepository.java](../src/main/java/sistemainventario/repository/SaleJdbcRepository.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `SaleJdbcRepository(Database bd, String perfilId)` | — | Constructor |
| `tabla()` | `String` | Nombre de la tabla |
| `funcionListar()` | `String` | Función de Oracle que devuelve las filas, por ejemplo FN_LISTAR_PRODUCTOS |
| `columnas()` | `List<String>` | Columnas que se escriben, sin PERFIL_ID ni ID (mismo orden que los parámetros de SP_GUARDAR) |
| `valores(Sale v)` | `Object[]` | Valores en el mismo orden de columnas |
| `leer(ResultSet fila)` | `Sale` | La entidad |
| `completar(List<Sale> ventas)` | `void` | Lee todo el detalle del negocio en una sola consulta y lo reparte entre las ventas |
| `guardarHijos(Connection conexion, Sale venta, boolean nueva)` | `void` | Reescribe el detalle de la venta (al editar se borra el anterior) |

##### SinConexionStorageProvider

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | implementa `StorageProvider` |
| Responsabilidad | Lo que usa TBalance mientras no hay conexión con Oracle: todo vacío y sin poder guardar. |
| Archivo | [SinConexionStorageProvider.java](../src/main/java/sistemainventario/repository/SinConexionStorageProvider.java) |

**Constantes:** `MENSAJE`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `nombre()` | `String` | Nombre para mostrar ("Oracle" o "Sin conexión") |
| `perfiles()` | `Repository<Profile>` | Repositorio de perfiles |
| `usuarios()` | `Repository<User>` | Personas que usan TBalance, con sus accesos a los negocios |
| `auditoria()` | `Repository<AuditEntry>` | Historial de acciones (quién hizo qué) |
| `productos(String perfilId)` | `Repository<Product>` | Repositorio de productos del negocio |
| `categorias(String perfilId)` | `Repository<Category>` | Repositorio de categorías del negocio |
| `proveedores(String perfilId)` | `Repository<Supplier>` | Repositorio de proveedores del negocio |
| `entradas(String perfilId)` | `Repository<StockEntry>` | Repositorio de entradas del negocio |
| `salidas(String perfilId)` | `Repository<StockExit>` | Repositorio de salidas del negocio |
| `ventas(String perfilId)` | `Repository<Sale>` | Repositorio de ventas del negocio |
| `abonos(String perfilId)` | `Repository<CreditPayment>` | Abonos de los clientes que deben (fiados) |

##### StockEntryJdbcRepository

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | hereda de `JdbcRepository` |
| Responsabilidad | Entradas de inventario de un negocio en la tabla ENTRADA. |
| Archivo | [StockEntryJdbcRepository.java](../src/main/java/sistemainventario/repository/StockEntryJdbcRepository.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `StockEntryJdbcRepository(Database bd, String perfilId)` | — | Constructor |
| `tabla()` | `String` | Nombre de la tabla |
| `funcionListar()` | `String` | Función de Oracle que devuelve las filas, por ejemplo FN_LISTAR_PRODUCTOS |
| `columnas()` | `List<String>` | Columnas que se escriben, sin PERFIL_ID ni ID (mismo orden que los parámetros de SP_GUARDAR) |
| `valores(StockEntry e)` | `Object[]` | Valores en el mismo orden de columnas |
| `leer(ResultSet fila)` | `StockEntry` | La entidad |

##### StockExitJdbcRepository

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | hereda de `JdbcRepository` |
| Responsabilidad | Salidas de inventario de un negocio en la tabla SALIDA. |
| Archivo | [StockExitJdbcRepository.java](../src/main/java/sistemainventario/repository/StockExitJdbcRepository.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `StockExitJdbcRepository(Database bd, String perfilId)` | — | Constructor |
| `tabla()` | `String` | Nombre de la tabla |
| `funcionListar()` | `String` | Función de Oracle que devuelve las filas, por ejemplo FN_LISTAR_PRODUCTOS |
| `columnas()` | `List<String>` | Columnas que se escriben, sin PERFIL_ID ni ID (mismo orden que los parámetros de SP_GUARDAR) |
| `valores(StockExit s)` | `Object[]` | Valores en el mismo orden de columnas |
| `leer(ResultSet fila)` | `StockExit` | La entidad |

##### StorageProvider

| Campo | Valor |
|---|---|
| Tipo | Interfaz |
| Herencia | extiende `AutoCloseable` |
| Responsabilidad | Entrega los repositorios de TBalance: los de Oracle, los vacíos sin conexión o los de una copia de seguridad. |
| Archivo | [StorageProvider.java](../src/main/java/sistemainventario/repository/StorageProvider.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `nombre()` *(abstracto)* | `String` | Nombre para mostrar ("Oracle" o "Sin conexión") |
| `perfiles()` *(abstracto)* | `Repository<Profile>` | Repositorio de perfiles |
| `usuarios()` *(abstracto)* | `Repository<User>` | Personas que usan TBalance, con sus accesos a los negocios |
| `auditoria()` *(abstracto)* | `Repository<AuditEntry>` | Historial de acciones (quién hizo qué) |
| `productos(String perfilId)` *(abstracto)* | `Repository<Product>` | Repositorio de productos del negocio |
| `categorias(String perfilId)` *(abstracto)* | `Repository<Category>` | Repositorio de categorías del negocio |
| `proveedores(String perfilId)` *(abstracto)* | `Repository<Supplier>` | Repositorio de proveedores del negocio |
| `entradas(String perfilId)` *(abstracto)* | `Repository<StockEntry>` | Repositorio de entradas del negocio |
| `salidas(String perfilId)` *(abstracto)* | `Repository<StockExit>` | Repositorio de salidas del negocio |
| `ventas(String perfilId)` *(abstracto)* | `Repository<Sale>` | Repositorio de ventas del negocio |
| `abonos(String perfilId)` *(abstracto)* | `Repository<CreditPayment>` | Abonos de los clientes que deben (fiados) |
| `rutinas(String perfilId)` | `Optional<DatabaseRoutines>` | Los procedimientos y funciones de la base, si el motor los tiene (vacío mientras no hay conexión) |
| `close()` | `void` | Libera la conexión, si la hay |

##### SupplierJdbcRepository

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | hereda de `JdbcRepository` |
| Responsabilidad | Proveedores de un negocio en la tabla PROVEEDOR. |
| Archivo | [SupplierJdbcRepository.java](../src/main/java/sistemainventario/repository/SupplierJdbcRepository.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `SupplierJdbcRepository(Database bd, String perfilId)` | — | Constructor |
| `tabla()` | `String` | Nombre de la tabla |
| `funcionListar()` | `String` | Función de Oracle que devuelve las filas, por ejemplo FN_LISTAR_PRODUCTOS |
| `columnas()` | `List<String>` | Columnas que se escriben, sin PERFIL_ID ni ID (mismo orden que los parámetros de SP_GUARDAR) |
| `valores(Supplier p)` | `Object[]` | Valores en el mismo orden de columnas |
| `leer(ResultSet fila)` | `Supplier` | La entidad |
| `completar(List<Supplier> proveedores)` | `void` | Lee de PROVEEDOR_CATEGORIA los tipos de producto de todos los proveedores del negocio |
| `guardarHijos(Connection conexion, Supplier proveedor, boolean nuevo)` | `void` | Reescribe los tipos de producto del proveedor (solo categorías que existen en el negocio) |

##### UserJdbcRepository

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | hereda de `JdbcRepository` |
| Responsabilidad | Personas en la tabla USUARIO y sus accesos en NEGOCIO_USUARIO (qué negocio y con qué rol). |
| Archivo | [UserJdbcRepository.java](../src/main/java/sistemainventario/repository/UserJdbcRepository.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `UserJdbcRepository(Database bd)` | — | Constructor |
| `tabla()` | `String` | Nombre de la tabla |
| `funcionListar()` | `String` | Función de Oracle que devuelve las filas, por ejemplo FN_LISTAR_PRODUCTOS |
| `columnas()` | `List<String>` | Columnas que se escriben, sin PERFIL_ID ni ID (mismo orden que los parámetros de SP_GUARDAR) |
| `valores(User u)` | `Object[]` | Valores en el mismo orden de columnas |
| `leer(ResultSet fila)` | `User` | La entidad |
| `completar(List<User> usuarios)` | `void` | Completa las entidades leídas con datos de otras tablas (por ejemplo el detalle de las ventas) |
| `guardarHijos(Connection conexion, User usuario, boolean nuevo)` | `void` | Escribe los datos de tablas hijas después del INSERT o del UPDATE |

##### ZipArchiver

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Comprime una carpeta en un archivo .zip y la extrae de nuevo. |
| Archivo | [ZipArchiver.java](../src/main/java/sistemainventario/repository/ZipArchiver.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `comprimir(Path carpeta, Path destino)` | `void` | Comprime todo el contenido de una carpeta |
| `extraer(Path archivoZip, Path carpeta)` | `int` | Extrae un .zip dentro de una carpeta (que se crea si no existe) |

#### Paquete `ia`

##### GeminiClient

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | implementa `LanguageModel` |
| Responsabilidad | Cliente de la API de Google Gemini (capa gratuita) con llamadas a funciones. |
| Archivo | [GeminiClient.java](../src/main/java/sistemainventario/ia/GeminiClient.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `clave` | `String` | Clave |
| `modelo` | `String` | Modelo |
| `http` | `HttpClient` | Http |
| `alCambiarModelo` | `Consumer<String>` | Al cambiar modelo |

**Constantes:** `MODELO_POR_DEFECTO`, `ESPERA_MAXIMA`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `GeminiClient(String clave, String modelo)` | — | Constructor |
| `modelo()` | `String` | El modelo que se está usando (puede cambiar si Google retiró el configurado) |
| `alCambiarModelo(Consumer<String> accion)` | `void` | Al cambiar modelo |
| `generar(String instrucciones, List<JsonObject> contenidos, JsonArray funciones)` | `ModelReply` | Pide la siguiente respuesta del modelo |
| `buscarModeloDisponible()` | `Optional<String>` | Pregunta a Google qué modelos puede usar esta clave y elige el más liviano: el Flash-Lite estable de versión más alta; si no hay, el Flash estable más alto |
| `elegirModelo(List<String> nombres)` *(static)* | `Optional<String>` | Elige entre los nombres de modelo el Flash-Lite (o si no, el Flash) estable más nuevo |
| `textoComoContenido(String rol, String texto)` *(static)* | `JsonObject` | Un contenido de Gemini con un solo texto |

##### LanguageModel

| Campo | Valor |
|---|---|
| Tipo | Interfaz |
| Herencia | — |
| Responsabilidad | Modelo de lenguaje en la nube que usa el asistente. |
| Archivo | [LanguageModel.java](../src/main/java/sistemainventario/ia/LanguageModel.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `generar(String instrucciones, List<JsonObject> contenidos, JsonArray funciones)` *(abstracto)* | `ModelReply` | Pide la siguiente respuesta del modelo |

##### SecureConnection

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Prepara el cliente HTTP del asistente para que se conecte como lo hace Edge en el mismo computador. |
| Archivo | [SecureConnection.java](../src/main/java/sistemainventario/ia/SecureConnection.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `crearCliente()` *(static)* | `HttpClient` | Un cliente HTTP con el proxy del sistema y los certificados de Java y de Windows |

#### Paquete `service`

##### AssistantProposal

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Cambio que el asistente propone y que solo se hace si el usuario lo confirma (registrar una venta, una entrada de mercancía, cambiar un precio...). |
| Archivo | [AssistantProposal.java](../src/main/java/sistemainventario/service/AssistantProposal.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `id` | `String` | Identificador |
| `tipo` | `String` | Tipo |
| `titulo` | `String` | Titulo |
| `lineas` | `List<String>` | Lineas |
| `total` | `Double` | Total |
| `accion` | `Runnable` | Accion |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `AssistantProposal(String id, String tipo, String titulo, List<String> lineas, Double total, Runnable accion)` | — | Constructor |
| `ejecutar()` | `void` | Hace el cambio propuesto |
| Getters y setters | — | `getId()`, `getLineas()`, `getTipo()`, `getTitulo()`, `getTotal()` |

##### AssistantService

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Maneja la conversación con el asistente y guarda lo que propone hasta que el usuario confirma. |
| Archivo | [AssistantService.java](../src/main/java/sistemainventario/service/AssistantService.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `modelo` | `LanguageModel` | Modelo (LanguageModel) |
| `servicios` | `Supplier<ProfileServices>` | Servicios |
| `perfil` | `Supplier<Profile>` | Perfil |
| `hiloDeDatos` | `Executor` | Hilo de datos |
| `permisos` | `Supplier<Set<Permiso>>` | Permisos |
| `historial` | `List<JsonObject>` | Historial |
| `pendientes` | `Map<String, AssistantProposal>` | Se consultan desde dos hilos (la conversación y la confirmación), por eso son colecciones seguras |
| `notas` | `List<String>` | Notas |

**Constantes:** `MAXIMO_PASOS`, `MAXIMO_MENSAJES`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `conversar(String mensaje)` | `Reply` | La respuesta del asistente |
| `confirmar(String id)` | `String` | Hace el cambio propuesto |
| `descartar(String id)` | `void` | Descarta |
| `reiniciar()` | `void` | Empieza una conversación nueva |
| `loQuePuedeHacer(Set<Permiso> permisos)` *(static)* | `String` | Regla con lo que el rol de quien conversa puede y no puede hacer, para que el asistente no ofrezca lo que después Java va a rechazar |

##### AssistantSettingsService

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Guarda y prueba la configuración del asistente. |
| Archivo | [AssistantSettingsService.java](../src/main/java/sistemainventario/service/AssistantSettingsService.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `repositorio` | `AssistantConfigRepository` | Repositorio (AssistantConfigRepository) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `AssistantSettingsService(AssistantConfigRepository repositorio)` | — | Constructor |
| `leer()` | `Optional<AssistantSettings>` | La configuración guardada, si hay una clave |
| `modeloActual()` | `String` | El modelo guardado o, si no hay, el modelo por defecto |
| `configurar(String clave, String modelo)` | `void` | Guarda la clave y el modelo; con la clave vacía borra la configuración |
| `requerir()` | `AssistantSettings` | La configuración guardada |
| `crearModelo(Consumer<String> alCambiarModelo)` | `LanguageModel` | Crea el cliente del modelo con la configuración guardada |
| `probarConexion(Consumer<String> alCambiarModelo)` | `ConnectionTest` | Envía un mensaje corto para comprobar la clave y la conexión |
| `probarClave(String clave, String modelo)` | `ConnectionTest` | Prueba una clave escrita en la pantalla, sin guardarla |

##### AssistantTools

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Funciones del inventario que el asistente puede usar. |
| Archivo | [AssistantTools.java](../src/main/java/sistemainventario/service/AssistantTools.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `servicios` | `ProfileServices` | Servicios (ProfileServices) |
| `permisos` | `Set<Permiso>` | Permisos |
| `alProponer` | `Consumer<AssistantProposal>` | Al proponer |
| `contador` | `int` | Contador |

**Constantes:** `OBSERVACION`, `MAXIMO_PRODUCTOS_VENDIDOS`, `MAXIMO_VENTAS_DETALLE`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `AssistantTools(ProfileServices servicios, Consumer<AssistantProposal> alProponer)` / `AssistantTools(ProfileServices servicios, Consumer<AssistantProposal> alProponer, Set<Permiso> permisos)` | — | Constructores |
| `permisoParaConfirmar(String tipo)` *(static)* | `Permiso` | Permiso que se necesita para confirmarla |
| `declaraciones()` *(static)* | `JsonArray` | Todas las funciones, en el formato de declaraciones de Gemini |
| `declaraciones(Set<Permiso> permisos)` *(static)* | `JsonArray` | El modelo solo conoce las funciones que el rol puede usar: así no ofrece, por ejemplo, registrar mercancía a un cajero (y Java igual lo rechazaría al confirmar) |
| `ejecutar(String nombre, JsonObject argumentos)` | `JsonObject` | Resultado en JSON para devolverle al modelo |
| `normalizar(String texto)` *(static)* | `String` | Normalizar |
| `dinero(double valor)` *(static)* | `String` | Dinero |

##### AuditService

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Anota acciones en el historial y las lista con filtros (persona, fechas, negocio). |
| Archivo | [AuditService.java](../src/main/java/sistemainventario/service/AuditService.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `historial` | `Repository<AuditEntry>` | Historial |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `AuditService(Repository<AuditEntry> historial)` | — | Constructor |
| `registrar(AuditEntry entrada)` | `void` | Registra |
| `nuevoId()` *(static)* | `String` | Un ID nuevo para una fila del historial |

##### BackupService

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Crea, lista, restaura y elimina las copias de seguridad. |
| Archivo | [BackupService.java](../src/main/java/sistemainventario/service/BackupService.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `archivador` | `ZipArchiver` | Archivador (ZipArchiver) |
| `almacen` | `BackupStore` | Almacen (BackupStore) |
| `datos` | `DataSnapshot` | Datos (DataSnapshot) |

**Constantes:** `PREFIJO`, `PREFIJO_SEGURIDAD`, `FORMATO_NOMBRE`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `BackupService(BackupStore almacen, DataSnapshot datos, ZipArchiver archivador)` | — | Constructor |
| `usar(BackupStore almacen, DataSnapshot datos)` | `void` | Cambia el destino de las copias al cambiar de almacenamiento: con base de datos las copias quedan en la misma base; con archivos, en la carpeta |
| `listar()` | `List<Backup>` | Copias guardadas, de la más reciente a la más antigua |
| `crear()` | `Backup` | La copia creada |
| `restaurar(String nombre)` | `Backup` | Restaura una copia |
| `eliminar(String nombre)` | `void` | Elimina |
| `getCarpetaRespaldos()` | `Path` | La carpeta de las copias, o null si están en la base de datos |
| `getUbicacion()` | `String` | Dónde están las copias, para mostrarlo |

##### CategoryService

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | hereda de `EntityService` |
| Responsabilidad | Casos de uso de las categorías de un perfil. |
| Archivo | [CategoryService.java](../src/main/java/sistemainventario/service/CategoryService.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `productos` | `Supplier<List<Product>>` | Productos habilitados del negocio, para no deshabilitar una categoría en uso |

**Constantes:** `PREFIJO_ID`, `SIN_CATEGORIA_ID`, `SIN_CATEGORIA`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `CategoryService(Repository<Category> repositorio)` | — | Constructor |
| `usarProductos(Supplier<List<Product>> productos)` | `void` | Usar productos |
| `resolverCategoria(Product producto)` | `void` | Completa la categoría de un producto: con el ID busca el nombre, y con solo el nombre busca el ID |
| `sinCategoria()` | `Category` | La categoría "Sin categoría", creándola si no existe |
| `buscarPorNombre(String nombre)` | `Optional<Category>` | La categoría con ese nombre, habilitada o no |
| `validarDeshabilitar(Category categoria)` | `void` | Reglas extra antes de deshabilitar |
| `validar(Category categoria)` | `void` | Paso variable: reglas de validación propias de la entidad |
| `generarId()` | `String` | Paso variable: estrategia de generación de ID de la entidad |
| `nombreEntidad()` | `String` | Nombre legible de la entidad para mensajes de error |
| `crearPredefinidas(BusinessType tipoNegocio)` | `void` | Crea las categorías iniciales de un tipo de negocio |

##### CostService

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Costos del negocio por sus productos: cuánto cuesta cada producto, cuánto costó todo lo que hay en inventario y cuánto se ha comprado (entradas) en un periodo. |
| Archivo | [CostService.java](../src/main/java/sistemainventario/service/CostService.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `inventario` | `InventoryService` | Inventario (InventoryService) |
| `movimientos` | `MovementService` | Movimientos (MovementService) |
| `proveedores` | `SupplierService` | Proveedores (SupplierService) |
| `rutinas` | `DatabaseRoutines` | FN_VALOR_INVENTARIO (solo Oracle); null para sumar en Java |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `CostService(InventoryService inventario, MovementService movimientos, SupplierService proveedores)` | — | Constructor |
| `usarRutinas(DatabaseRoutines rutinas)` | `CostService` | Este servicio |
| `resumen(LocalDate desde, LocalDate hasta)` | `Summary` | Costos del inventario y compras del periodo |

##### CreditService

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Calcula quién debe y cuánto (ventas fiadas menos abonos) y registra y anula abonos. |
| Archivo | [CreditService.java](../src/main/java/sistemainventario/service/CreditService.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `ventas` | `SaleService` | Ventas (SaleService) |
| `abonos` | `Repository<CreditPayment>` | Abonos |

**Constantes:** `TOLERANCIA`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `CreditService(SaleService ventas, Repository<CreditPayment> abonos)` | — | Constructor |
| `listarCuentas()` | `List<CustomerDebt>` | Las cuentas de todos los clientes con fiados: primero los que deben más; al final los que ya están al día |
| `obtenerCuenta(String clave)` | `CustomerDebt` | La cuenta con sus ventas fiadas y sus abonos |
| `totalPorCobrar()` | `double` | Lo que deben entre todos los clientes |
| `registrarAbono(CreditPayment abono)` | `CreditPayment` | Registra un abono |
| `anularAbono(String id, String motivo, String quien)` | `CreditPayment` | Anula un abono registrado por error; el saldo del cliente vuelve a subir |
| `listarAbonos()` | `List<CreditPayment>` | Todos los abonos del negocio, también los anulados |
| `claveCliente(Sale venta)` *(static)* | `String` | Clave con la que se agrupan los fiados de un cliente: su cédula, o su nombre (sin tildes ni mayúsculas) en ventas fiadas viejas sin cédula |

##### DataSnapshot

| Campo | Valor |
|---|---|
| Tipo | Interfaz |
| Herencia | — |
| Responsabilidad | Cómo se sacan y se reponen los datos para las copias de seguridad. |
| Archivo | [DataSnapshot.java](../src/main/java/sistemainventario/service/DataSnapshot.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `prepararCarpeta()` *(abstracto)* | `Path` | Carpeta con los datos en JSON, lista para comprimir |
| `liberarCarpeta(Path carpeta)` *(abstracto)* | `void` | Liberar carpeta |
| `restaurarDesde(Path carpeta)` *(abstracto)* | `void` | Reemplaza los datos actuales por los de una copia |

##### DataTransferService

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Copia todos los negocios de un almacenamiento a otro, para las copias de seguridad: de Oracle a los archivos del .zip al crearla, y de esos archivos a Oracle al restaurarla. |
| Archivo | [DataTransferService.java](../src/main/java/sistemainventario/service/DataTransferService.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `copiar(StorageProvider origen, StorageProvider destino)` | `Resumen` | Cuánto se copió |
| `copiarUsuarios(List<User> usuarios, StorageProvider destino)` | `void` | Copia personas al almacenamiento, dejando solo los accesos a negocios que existen ahí |
| `vaciar(StorageProvider almacen)` | `void` | Borra todos los negocios del almacenamiento |

##### DatabaseSnapshot

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | implementa `DataSnapshot` |
| Responsabilidad | Copias de seguridad de los datos de Oracle. |
| Archivo | [DatabaseSnapshot.java](../src/main/java/sistemainventario/service/DatabaseSnapshot.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `base` | `StorageProvider` | Base (StorageProvider) |
| `copiador` | `DataTransferService` | Copiador (DataTransferService) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `DatabaseSnapshot(StorageProvider base, DataTransferService copiador)` | — | Constructor |
| `prepararCarpeta()` | `Path` | Carpeta con los datos en JSON, lista para comprimir |
| `liberarCarpeta(Path carpeta)` | `void` | Liberar carpeta |
| `restaurarDesde(Path carpeta)` | `void` | Reemplaza los datos actuales por los de una copia |

##### EntityService

| Campo | Valor |
|---|---|
| Tipo | Abstracta |
| Herencia | — |
| Responsabilidad | Servicio base para las entidades con CRUD simple (productos, categorías y proveedores). |
| Archivo | [EntityService.java](../src/main/java/sistemainventario/service/EntityService.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `repositorio` | `Repository<T>` | Repositorio |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `EntityService(Repository<T> repositorio)` | — | Constructor |
| `refrescar()` | `void` | Si otro equipo cambió los datos en la base, los vuelve a leer |
| `listarTodos()` | `List<T>` | Todas las entidades del perfil activo |
| `listarDeshabilitados()` | `List<T>` | Los registros deshabilitados (borrado lógico), para poder volver a habilitarlos |
| `buscarPorId(String id)` | `Optional<T>` | La entidad, o vacío si no existe |
| `obtener(String id)` | `T` | La entidad encontrada |
| `guardar(T entidad)` | `T` | Crea o actualiza una entidad (método plantilla) |
| `cambiarEstado(String id, boolean activo)` | `T` | Borrado lógico: deshabilita o vuelve a habilitar el registro |
| `validarDeshabilitar(T entidad)` | `void` | Reglas extra antes de deshabilitar |
| `validar(T entidad)` *(abstracto)* | `void` | Paso variable: reglas de validación propias de la entidad |
| `generarId()` *(abstracto)* | `String` | Paso variable: estrategia de generación de ID de la entidad |
| `nombreEntidad()` *(abstracto)* | `String` | Nombre legible de la entidad para mensajes de error |

##### GlobalConfigService

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Lectura y actualización de la GlobalConfig. |
| Archivo | [GlobalConfigService.java](../src/main/java/sistemainventario/service/GlobalConfigService.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `repositorio` | `GlobalConfigRepository` | Repositorio (GlobalConfigRepository) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `GlobalConfigService(GlobalConfigRepository repositorio)` | — | Constructor |
| `getPerfilActivoId()` | `Optional<String>` | ID del perfil activo, si hay uno |
| `establecerPerfilActivo(String perfilId)` | `void` | Establecer perfil activo |

##### InventoryService

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | hereda de `EntityService` |
| Responsabilidad | Casos de uso del catálogo de productos: CRUD, búsqueda, filtros y alertas de stock y vencimiento. |
| Archivo | [InventoryService.java](../src/main/java/sistemainventario/service/InventoryService.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `categorias` | `CategoryService` | Categorias (CategoryService) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `InventoryService(Repository<Product> repositorio)` | — | Constructor |
| `usarCategorias(CategoryService categorias)` | `void` | Conecta las categorías del mismo negocio: la categoría del producto es obligatoria y debe existir |
| `validar(Product producto)` | `void` | Paso variable: reglas de validación propias de la entidad |
| `normalizarCategorias()` | `int` | Deja cada producto con una categoría válida: si tenía solo el nombre, le pone el ID de esa categoría; si no tenía ninguna, lo pasa a "Sin categoría" |
| `generarId()` | `String` | Paso variable: estrategia de generación de ID de la entidad |
| `nombreEntidad()` | `String` | Nombre legible de la entidad para mensajes de error |
| `obtenerHabilitado(String id)` | `Product` | El producto, si está habilitado |
| `registrarCambioDeStock(Product producto)` | `void` | Persiste un cambio de stock sin volver a validar la ficha completa; así un producto ya vencido puede seguir recibiendo movimientos |
| `buscarPorTexto(String texto)` | `List<Product>` | Productos que coinciden por nombre, descripción, código o ID |
| `filtrarPorCategoria(String categoria)` | `List<Product>` | Productos de la categoría |
| `getUtilidadEstimadaInventario()` | `double` | Utilidad estimada del inventario: lo que se ganaría vendiendo todo el stock de los productos que tienen precio de compra |
| `getSinPrecioCompra()` | `List<Product>` | Productos sin precio de compra, cuya utilidad no se puede estimar |
| `getBajoStock()` | `List<Product>` | Productos con stock en el mínimo o por debajo (sin estar agotados) |
| `getAgotados()` | `List<Product>` | Productos sin unidades |
| `getVencidos()` | `List<Product>` | Productos cuya fecha de vencimiento ya pasó |
| `getPorVencer(int dias)` | `List<Product>` | Productos que vencen dentro de los próximos dias días |
| `getCategorias()` | `Set<String>` | Nombres de categorías usadas por los productos, ordenados |

##### LabelService

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Genera etiquetas con código QR para los productos: una etiqueta individual (vista previa o imagen) y hojas listas para imprimir. |
| Archivo | [LabelService.java](../src/main/java/sistemainventario/service/LabelService.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `generadorQR` | `QrGenerator` | Generador qr (QrGenerator) |
| `etiquetas` | `LabelRepository` | Etiquetas (LabelRepository) |

**Constantes:** `ANCHO_ETIQUETA`, `ALTO_ETIQUETA`, `ANCHO_HOJA`, `ALTO_HOJA`, `COLUMNAS`, `FILAS`, `MARGEN_HOJA`, `TAMANO_QR`, `COLOR_MARCA`, `COLOR_TEXTO`, `COLOR_SECUNDARIO`, `COLOR_BORDE`, `FORMATO_NOMBRE`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `LabelService(QrGenerator generadorQR, LabelRepository etiquetas)` | — | Constructor |
| `vistaPrevia(Product producto, String perfilId)` | `String` | La etiqueta como imagen Base64 para mostrarla en la vista |
| `guardarEtiqueta(Product producto, String perfilId)` | `Path` | Guarda la etiqueta de un producto como imagen PNG |
| `guardarHojas(List<Product> productos, String perfilId)` | `List<Path>` | Genera hojas tamaño carta con 18 etiquetas cada una, listas para imprimir |
| `getCarpeta()` | `Path` | Carpeta donde se guardan las etiquetas |
| `dibujarEtiqueta(Product producto, String perfilId)` | `BufferedImage` | Dibuja una etiqueta: código QR a la izquierda; nombre, precio, ID y categoría a la derecha |

##### MovementService

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Registro de entradas y salidas de inventario. |
| Archivo | [MovementService.java](../src/main/java/sistemainventario/service/MovementService.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `inventario` | `InventoryService` | Inventario (InventoryService) |
| `entradas` | `Repository<StockEntry>` | Entradas |
| `salidas` | `Repository<StockExit>` | Salidas |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `listarEntradas()` | `List<StockEntry>` | Entradas vigentes (sin las anuladas) |
| `listarSalidas()` | `List<StockExit>` | Salidas vigentes (sin las anuladas) |
| `listarEntradasAnuladas()` | `List<StockEntry>` | Entradas anuladas |
| `listarSalidasAnuladas()` | `List<StockExit>` | Salidas anuladas |
| `anularEntrada(String id, String motivo)` | `StockEntry` | Anula una entrada y quita del stock las unidades que había sumado |
| `anularEntrada(String id, String motivo, String quien)` | `StockEntry` | Igual que la anulación sin nombre, dejando anotado quién anuló |
| `anularSalida(String id, String motivo)` | `StockExit` | Anula una salida y devuelve al stock las unidades que había quitado |
| `anularSalida(String id, String motivo, String quien)` | `StockExit` | Igual que la anulación sin nombre, dejando anotado quién anuló |
| `registrarEntrada(StockEntry entrada)` | `StockEntry` | La entrada registrada |
| `registrarSalida(StockExit salida)` | `StockExit` | La salida registrada |

##### ProfileService

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Crea, edita, deshabilita, habilita y abre negocios. |
| Archivo | [ProfileService.java](../src/main/java/sistemainventario/service/ProfileService.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `perfiles` | `Repository<Profile>` | Perfiles |
| `configuracion` | `GlobalConfigService` | Configuracion (GlobalConfigService) |
| `fabricaServicios` | `ProfileServicesFactory` | Fabrica servicios (ProfileServicesFactory) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `listarTodos()` | `List<Profile>` | Lista todos |
| `buscarPorId(String id)` | `Optional<Profile>` | Busca por id |
| `getPerfilActivo()` | `Optional<Profile>` | El perfil activo, si existe |
| `resolverPerfilInicial()` | `Optional<String>` | Determina qué perfil cargar al iniciar: el activo guardado o, si no hay, el primero de la lista (comportamiento de P2) |
| `crear(Profile datos)` | `Profile` | Crea el perfil, su carpeta y, si el tipo de negocio lo indica, sus categorías predefinidas |
| `actualizar(Profile cambios)` | `Profile` | Actualiza nombre, descripción y color; conserva el resto de datos |
| `deshabilitar(String id)` | `void` | Deshabilita un negocio (borrado lógico): sus productos, ventas y demás datos se conservan y se puede volver a habilitar |
| `habilitar(String id)` | `void` | Vuelve a habilitar un negocio deshabilitado |
| `seleccionar(String id)` | `Optional<ProfileServices>` | Servicios del perfil activado, o vacío si se desactivó |
| `crearServicios(String id)` | `ProfileServices` | Servicios para operar sobre los datos de ese perfil |
| `normalizarCategorias()` | `int` | Revisa la categoría de los productos de todos los negocios (los datos de versiones en que era opcional quedan en "Sin categoría") |

##### ProfileServices

| Campo | Valor |
|---|---|
| Tipo | Registro |
| Herencia | — |
| Responsabilidad | Agrupa los servicios de un negocio: inventario, categorías, proveedores, movimientos, ventas y fiados. |
| Archivo | [ProfileServices.java](../src/main/java/sistemainventario/service/ProfileServices.java) |

**Datos:** `(InventoryService inventario, public record ProfileServices(InventoryService inventario, CategoryService categorias, SupplierService proveedores, MovementService movimientos, SaleService ventas, CreditService fiados)`

##### ProfileServicesFactory

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Construye los repositorios y servicios de un perfil. |
| Archivo | [ProfileServicesFactory.java](../src/main/java/sistemainventario/service/ProfileServicesFactory.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `almacen` | `StorageProvider` | Almacen (StorageProvider) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `ProfileServicesFactory(StorageProvider almacen)` | — | Constructor |
| `crear(String perfilId)` | `ProfileServices` | Servicios listos para operar sobre los datos de ese perfil |
| `normalizarCategorias(String perfilId)` | `int` | Deja todos los productos del negocio con una categoría válida (ver normalizarCategorias), sin cargar sus ventas |

##### QrSaleScanner

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Convierte lo que ve la cámara en productos para la venta por QR. |
| Archivo | [QrSaleScanner.java](../src/main/java/sistemainventario/service/QrSaleScanner.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `lector` | `QrReader` | Lector (QrReader) |
| `reloj` | `LongSupplier` | Reloj |
| `nombreNegocio` | `Function<String, Optional<String>>` | Nombre negocio |
| `ultimoContenido` | `String` | Ultimo contenido |
| `ultimaLectura` | `long` | Ultima lectura |

**Constantes:** `ESPERA_REPETIDO_MS`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `QrSaleScanner(QrReader lector)` / `QrSaleScanner(QrReader lector, LongSupplier reloj)` | — | Constructores |
| `conNombresDeNegocio(Function<String, Optional<String>> nombres)` | `QrSaleScanner` | Este escáner |
| `procesar(BufferedImage cuadro, InventoryService inventario, String perfilActivo)` | `Optional<Lectura>` | Valida la etiqueta antes de agregar el producto a la venta: que sea de TBalance, que esté completa, que sea del negocio abierto, que el producto exista, esté habilitado y tenga unidades |
| `validar(String texto, InventoryService inventario, String perfilActivo)` | `Lectura` | Interpreta y valida el texto de una etiqueta (separado de la cámara para probarlo) |

##### ReportExportService

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Exporta un informe de ventas a un libro de Excel en la carpeta Documentos/TBalance. |
| Archivo | [ReportExportService.java](../src/main/java/sistemainventario/service/ReportExportService.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `excel` | `ReportExcelWriter` | Excel (ReportExcelWriter) |
| `carpeta` | `Path` | Carpeta |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `ReportExportService(ReportExcelWriter excel, Path carpeta)` | — | Constructor |
| `exportarExcel(SalesReport informe, String negocio)` | `Path` | Guarda el informe como .xlsx |
| `carpetaPorDefecto(Path usuario)` *(static)* | `Path` | Documentos/TBalance si existe la carpeta Documentos; si no, /TBalance |

##### ReportService

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Calcula los informes de ventas de un período. |
| Archivo | [ReportService.java](../src/main/java/sistemainventario/service/ReportService.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `ventas` | `SaleService` | Ventas (SaleService) |
| `inventario` | `InventoryService` | Inventario (InventoryService) |

**Constantes:** `DIAS_MAX_POR_DIA`, `DIAS_MAX_POR_SEMANA`, `TOPE_PRODUCTOS`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `ReportService(SaleService ventas, InventoryService inventario)` | — | Constructor |
| `generar(String desdeIso, String hastaIso)` | `SalesReport` | El informe del período |
| `generar(String desdeIso, String hastaIso, String anteriorDesdeIso, String anteriorHastaIso)` | `SalesReport` | El informe del período |

##### SaleService

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Registro y consulta de ventas. |
| Archivo | [SaleService.java](../src/main/java/sistemainventario/service/SaleService.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `inventario` | `InventoryService` | Inventario (InventoryService) |
| `ventas` | `Repository<Sale>` | Ventas |
| `medioPermitido` | `Predicate<String>` | Medios de pago que acepta el negocio; por defecto, todos |
| `rutinas` | `DatabaseRoutines` | Procedimientos de la base (solo Oracle); null si se usa la lógica de Java |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `SaleService(InventoryService inventario, Repository<Sale> ventas)` | — | Constructor |
| `usarMediosDePago(Predicate<String> medioPermitido)` | `void` | Usar medios de pago |
| `usarRutinas(DatabaseRoutines rutinas)` | `void` | Usar rutinas |
| `rutinas()` | `Optional<DatabaseRoutines>` | Los procedimientos y funciones de la base, si se usan |
| `anotaAnulacionEnLaBase()` | `boolean` | True si la base anota la anulación en el historial (SP_ANULAR_VENTA) |
| `aceptaMedioPago(String metodo)` | `boolean` | True si el negocio acepta ese medio |
| `registrar(Sale venta)` | `Sale` | La venta registrada con ID, fecha, estado y total |
| `listarTodas()` | `List<Sale>` | Ventas vigentes (sin las anuladas), de la más reciente a la más antigua |
| `listarAnuladas()` | `List<Sale>` | Ventas anuladas, de la más reciente a la más antigua |
| `anular(String id, String motivo)` | `Sale` | Anula una venta: queda en el historial con el motivo, deja de contar en los totales y el stock de sus productos se devuelve |
| `obtener(String id)` | `Sale` | La venta (vigente o anulada) |
| `anular(String id, String motivo, String quien)` | `Sale` | Igual que anularString,, dejando anotado quién anuló |
| `anular(String id, String motivo, AuditEntry historial)` | `Sale` | Anula una venta dejando anotado quién lo hizo |
| `getTotalVentas()` | `double` | Suma de los totales de las ventas completadas |
| `getTotalTransacciones()` | `int` | Cantidad de ventas completadas |

##### SessionService

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Guarda quién inició sesión y cierra la sesión después de 30 minutos sin uso. |
| Archivo | [SessionService.java](../src/main/java/sistemainventario/service/SessionService.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `reloj` | `LongSupplier` | Reloj |
| `usuario` | `User` | Usuario (User) |
| `ultimaActividad` | `long` | Ultima actividad |

**Constantes:** `MINUTOS_INACTIVIDAD`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `SessionService()` / `SessionService(LongSupplier reloj)` | — | Constructores |
| `abrir(User persona)` | `void` | Abre |
| `cerrar()` | `void` | Cierra la sesión |
| `refrescar(User persona)` | `void` | Refrescar |
| `registrarActividad()` | `boolean` | Anota que la persona sigue usando TBalance |
| `actual()` | `Optional<User>` | La persona con sesión abierta (vacío si nadie o si venció por inactividad) |
| `requerir()` | `User` | La persona con sesión abierta |
| `rolEn(String perfilId)` | `Optional<Rol>` | Rol de la persona en ese negocio, si tiene acceso |
| `puede(String perfilId, Permiso permiso)` | `boolean` | True si la persona puede hacerla en ese negocio |

##### StorageService

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Conecta TBalance con Oracle y prepara la base. |
| Archivo | [StorageService.java](../src/main/java/sistemainventario/service/StorageService.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `ajustes` | `DatabaseSettingsRepository` | Ajustes (DatabaseSettingsRepository) |
| `copiador` | `DataTransferService` | Copiador (DataTransferService) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `StorageService(DatabaseSettingsRepository ajustes, DataTransferService copiador)` | — | Constructor |
| `leer()` | `DatabaseSettings` | La conexión guardada en este computador |
| `abrir()` | `Apertura` | Abre Oracle con la conexión guardada |
| `probar(DatabaseSettings config)` | `String` | Prueba una conexión sin guardarla |
| `guardar(DatabaseSettings config)` | `void` | Prueba la conexión y, si funciona, la guarda en este computador |

##### SupplierService

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | hereda de `EntityService` |
| Responsabilidad | Casos de uso de los proveedores de un perfil. |
| Archivo | [SupplierService.java](../src/main/java/sistemainventario/service/SupplierService.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `SupplierService(Repository<Supplier> repositorio)` | — | Constructor |
| `validar(Supplier proveedor)` | `void` | Paso variable: reglas de validación propias de la entidad |
| `generarId()` | `String` | Paso variable: estrategia de generación de ID de la entidad |
| `nombreEntidad()` | `String` | Nombre legible de la entidad para mensajes de error |
| `buscar(String texto)` | `List<Supplier>` | Proveedores que coinciden |

##### UserService

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Casos de uso de las personas que usan TBalance: la primera cuenta (el dueño), iniciar sesión, crear usuarios de un negocio, deshabilitarlos y cambiar contraseñas. |
| Archivo | [UserService.java](../src/main/java/sistemainventario/service/UserService.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `repositorio` | `Repository<User>` | Repositorio |
| `ficticio (static)` | `String` | Ficticio |

**Constantes:** `INTENTOS_PERMITIDOS`, `MINUTOS_BLOQUEO`, `LARGO_MINIMO_CLAVE`, `FORMATO_USUARIO`, `MENSAJE_INGRESO`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `UserService(Repository<User> repositorio)` | — | Constructor |
| `esPrimeraVez()` | `boolean` | True si todavía no hay ninguna cuenta (primera vez que se abre TBalance) |
| `crearPrimerDueno(String nombre, String usuario, String clave, Collection<String> negocios)` | `User` | Crea la primera cuenta, la del dueño, con acceso a todos los negocios que ya existan |
| `iniciarSesion(String usuario, String clave)` | `User` | La persona, si la contraseña es correcta |
| `usuariosDe(String perfilId)` | `List<User>` | Personas con acceso a ese negocio (habilitadas y deshabilitadas), por nombre |
| `crear(User quien, String perfilId, String nombre, String usuario, String clave, Rol rol)` | `User` | Crea una persona con acceso a un negocio, o le da acceso si ya existe ese usuario |
| `editar(User quien, String perfilId, String id, String nombre, Rol rol)` | `User` | Cambia el nombre y el rol de una persona en un negocio |
| `cambiarEstado(User quien, String perfilId, String id, boolean activo)` | `void` | Deshabilita o vuelve a habilitar una persona (no se borra: sus ventas quedan con su nombre) |
| `restablecerClave(User quien, String perfilId, String id, String nueva)` | `void` | Pone una contraseña nueva a otra persona (por ejemplo, si la olvidó) |
| `cambiarMiClave(User quien, String actual, String nueva)` | `void` | Cambia mi clave |
| `hacerDueno(User quien, String perfilId)` | `void` | Da acceso de dueño a quien crea un negocio nuevo |
| `obtener(String id)` | `User` | La persona con los datos guardados más recientes |

#### Paquete `bridge`

##### AccessPolicy

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Revisa en Java, antes de cada llamada de la página, que la persona tenga la sesión abierta y el permiso que pide esa acción en el negocio abierto. |
| Archivo | [AccessPolicy.java](../src/main/java/sistemainventario/bridge/AccessPolicy.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `contexto` | `AppContext` | Contexto de la aplicación |
| `historial` | `AuditTrail` | Historial (AuditTrail) |

**Constantes:** `CAMPOS_COSTO`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `AccessPolicy(AppContext contexto)` | — | Constructor |
| `antesDeLlamar()` | `AuditTrail.Momento` | Quién y dónde, justo antes de una llamada (para el historial) |
| `revisar(String metodo)` | `Optional<String>` | Mensaje de rechazo, o vacío si se puede llamar |
| `filtrar(String metodo, Object resultado)` | `Object` | Quita precios de compra y ganancias de la respuesta si el rol no los puede ver (el cajero ve precios de venta, pero no cuánto costó la mercancía) |

##### AssistantController

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Recibe las acciones del asistente: configurar la clave, conversar y confirmar o descartar lo que propone. |
| Archivo | [AssistantController.java](../src/main/java/sistemainventario/bridge/AssistantController.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `contexto` | `AppContext` | Contexto de la aplicación |
| `notificador` | `ViewNotifier` | Notificador (ViewNotifier) |
| `hiloDeDatos` | `Executor` | Hilo de datos |
| `ajustes` | `AssistantSettingsService` | Ajustes (AssistantSettingsService) |
| `asistente` | `AssistantService` | Asistente (AssistantService) |
| `perfilDeLaConversacion` | `String` | Perfil de la conversacion |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `AssistantController(AppContext contexto, ViewNotifier notificador, Executor hiloDeDatos)` | — | Constructor |
| `asistenteEstado()` | `String` | {"configurado":bool,"modelo":"..."} |
| `asistenteConfigurar(String clave, String modelo)` | `String` | "OK" o "ERROR: ..." |
| `asistenteEnviar(String mensaje)` | `String` | Envía un mensaje; la respuesta llega después con onAsistenteRespuesta |
| `asistenteProbar()` | `String` | Prueba la clave y la conexión con un mensaje corto; el resultado llega con onAsistentePrueba({"ok":bool,"mensaje":"...")} |
| `asistenteProbarClave(String clave, String modelo)` | `String` | Prueba la clave escrita en la pantalla sin guardarla; el resultado llega con onAsistentePrueba(...) igual que asistenteProbar |
| `asistenteConfirmar(String propuestaId)` | `String` | Descripción de lo registrado o "ERROR: ..." |
| `asistenteDescartar(String propuestaId)` | `String` | "OK" |
| `asistenteReiniciar()` | `String` | "OK" |

##### AuditTrail

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Decide qué llamadas del puente quedan en el historial y arma el texto de cada una. |
| Archivo | [AuditTrail.java](../src/main/java/sistemainventario/bridge/AuditTrail.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `contexto` | `AppContext` | Contexto de la aplicación |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `AuditTrail(AppContext contexto)` | — | Constructor |
| `antes()` | `Momento` | Quién está y en qué negocio, justo antes de una llamada |
| `anotar(String metodo, JsonArray argumentos, Object resultado, String rechazo, Momento momento)` | `void` | Anota la llamada si es de las que cambian datos y salió bien, o si fue un intento sin permiso o un ingreso fallido |

##### BackupController

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Recibe las acciones de la pantalla copias de seguridad. |
| Archivo | [BackupController.java](../src/main/java/sistemainventario/bridge/BackupController.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `contexto` | `AppContext` | Contexto de la aplicación |
| `notificador` | `ViewNotifier` | Notificador (ViewNotifier) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `BackupController(AppContext contexto, ViewNotifier notificador)` | — | Constructor |
| `listarRespaldos()` | `String` | Lista respaldos |
| `crearRespaldo()` | `String` | Crea respaldo |
| `restaurarRespaldo(String nombre)` | `String` | Restaura un respaldo y pide a la vista que recargue todos los datos |
| `eliminarRespaldo(String nombre)` | `String` | Elimina respaldo |
| `obtenerCarpetaRespaldos()` | `String` | La carpeta de las copias o, con base de datos, en qué base están |

##### BridgeInvoker

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Ejecuta un método público de JavaBridge a partir de su nombre y de sus argumentos en JSON, y devuelve el resultado también en JSON. |
| Archivo | [BridgeInvoker.java](../src/main/java/sistemainventario/bridge/BridgeInvoker.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `puente` | `Object` | Puente |
| `politica` | `AccessPolicy` | Politica (AccessPolicy) |
| `gson` | `Gson` | Gson |
| `metodos` | `Map<String, List<Method>>` | Métodos públicos del puente agrupados por nombre (puede haber sobrecargas) |

**Constantes:** `LOTE`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `BridgeInvoker(Object puente)` / `BridgeInvoker(Object puente, AccessPolicy politica)` | — | Constructores |
| `invocar(String nombre, String argumentosJson)` | `String` | Objeto JSON {"ok":true,"v":resultado} o {"ok":false,"error":"..."} |

##### BridgeResponse

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Formato de las respuestas que el bridge devuelve a JavaScript. |
| Archivo | [BridgeResponse.java](../src/main/java/sistemainventario/bridge/BridgeResponse.java) |

**Constantes:** `OK`, `PREFIJO_ERROR`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `json(Object objeto)` *(static)* | `String` | El objeto serializado como JSON |
| `error(String mensaje)` *(static)* | `String` | Mensaje de error en el formato que entiende la vista |
| `ejecutar(Supplier<String> accion)` *(static)* | `String` | Ejecuta una acción y traduce cualquier excepción a "ERROR: ..." |
| `ejecutarOk(Runnable accion)` *(static)* | `String` | Ejecuta una acción sin resultado y responde "OK" si no falla |
| `literalJs(String texto)` *(static)* | `String` | Escapa un texto para insertarlo como literal en un script JavaScript |

##### CatalogController

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Recibe las acciones de la pantalla categorías y proveedores. |
| Archivo | [CatalogController.java](../src/main/java/sistemainventario/bridge/CatalogController.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `contexto` | `AppContext` | Contexto de la aplicación |

**Constantes:** `LISTA_VACIA`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `CatalogController(AppContext contexto)` | — | Constructor |
| `obtenerCategorias()` | `String` | Devuelve categorias |
| `guardarCategoria(String jsonCategoria)` | `String` | Guarda categoria |
| `obtenerCategoriasDeshabilitadas()` | `String` | Categorías deshabilitadas del perfil activo |
| `cambiarEstadoCategoria(String id, String activa)` | `String` | "OK" o "ERROR: ..." |
| `obtenerProveedores()` | `String` | Devuelve proveedores |
| `guardarProveedor(String jsonProveedor)` | `String` | Guarda proveedor |
| `obtenerProveedoresDeshabilitados()` | `String` | Proveedores deshabilitados del perfil activo |
| `cambiarEstadoProveedor(String id, String activo)` | `String` | "OK" o "ERROR: ..." |

##### CostController

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Entrega a la pantalla los datos de la sección Costos. |
| Archivo | [CostController.java](../src/main/java/sistemainventario/bridge/CostController.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `contexto` | `AppContext` | Contexto de la aplicación |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `CostController(AppContext contexto)` | — | Constructor |
| `obtenerCostos(String desde, String hasta)` | `String` | JSON con el costo del inventario por producto y las compras del periodo |

##### CreditController

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Recibe las acciones de la sección Fiados: ver quién debe, ver la cuenta de un cliente, registrar y anular abonos. |
| Archivo | [CreditController.java](../src/main/java/sistemainventario/bridge/CreditController.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `contexto` | `AppContext` | Contexto de la aplicación |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `CreditController(AppContext contexto)` | — | Constructor |
| `listarFiados()` | `String` | Cuentas de los clientes con fiados (lo que deben y lo abonado) |
| `resumenFiados()` | `String` | Solo lo que la pantalla Hoy necesita: cliente, lo que debe y la fecha de la primera compra fiada |
| `obtenerFiado(String clave)` | `String` | La cuenta del cliente con sus ventas fiadas y sus abonos |
| `registrarAbono(String jsonAbono)` | `String` | El abono registrado |
| `anularAbono(String id, String motivo)` | `String` | OK o el error |

##### DesktopFileOpener

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | implementa `FileOpener` |
| Responsabilidad | Implementación de FileOpener con Desktop. |
| Archivo | [DesktopFileOpener.java](../src/main/java/sistemainventario/bridge/DesktopFileOpener.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `abrir(Path ruta)` | `void` | Abre |

##### FileOpener

| Campo | Valor |
|---|---|
| Tipo | Interfaz |
| Herencia | — |
| Responsabilidad | Abre un archivo o carpeta con el programa predeterminado del sistema operativo (visor de imágenes, explorador de archivos...). |
| Archivo | [FileOpener.java](../src/main/java/sistemainventario/bridge/FileOpener.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `abrir(Path ruta)` *(abstracto)* | `void` | Abre |

##### InventoryController

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Recibe las acciones de la pantalla productos y del dashboard. |
| Archivo | [InventoryController.java](../src/main/java/sistemainventario/bridge/InventoryController.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `contexto` | `AppContext` | Contexto de la aplicación |

**Constantes:** `LISTA_VACIA`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `InventoryController(AppContext contexto)` | — | Constructor |
| `obtenerProductos()` | `String` | Devuelve productos |
| `buscarProductos(String texto)` | `String` | Busca productos |
| `filtrarProductosPorCategoria(String categoria)` | `String` | Filtra productos por categoria |
| `obtenerProductosVencidos()` | `String` | Devuelve productos vencidos |
| `obtenerProductosPorVencer(int dias)` | `String` | Devuelve productos por vencer |
| `guardarProducto(String jsonProducto)` | `String` | Guarda producto |
| `obtenerProductosDeshabilitados()` | `String` | Productos deshabilitados del perfil activo |
| `cambiarEstadoProducto(String id, String activo)` | `String` | "OK" o "ERROR: ..." |
| `obtenerEstadisticas(boolean incluirVencimientos)` | `String` | Indicadores clave del perfil activo en JSON |

##### JavaBridge

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Objeto que la página web llama como window.javaBridge; pasa cada pedido al controlador que le corresponde. |
| Archivo | [JavaBridge.java](../src/main/java/sistemainventario/bridge/JavaBridge.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `perfiles` | `ProfileController` | Perfiles (ProfileController) |
| `inventario` | `InventoryController` | Inventario (InventoryController) |
| `catalogo` | `CatalogController` | Catalogo (CatalogController) |
| `transacciones` | `TransactionController` | Transacciones (TransactionController) |
| `respaldos` | `BackupController` | Respaldos (BackupController) |
| `almacenamiento` | `StorageController` | Almacenamiento (StorageController) |
| `etiquetas` | `LabelController` | Etiquetas (LabelController) |
| `informes` | `ReportController` | Informes (ReportController) |
| `asistente` | `AssistantController` | Asistente (AssistantController) |
| `escaneo` | `ScanController` | Escaneo (ScanController) |
| `costos` | `CostController` | Costos (CostController) |
| `fiados` | `CreditController` | Fiados (CreditController) |
| `sesion` | `SessionController` | Sesion (SessionController) |
| `notificador` | `ViewNotifier` | Notificador (ViewNotifier) |
| `transicion` | `ScreenTransition` | Transiciones grandes de la ventana; es null si la vista no las ofrece |

**Constantes:** `CANDADO_DATOS`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `JavaBridge(AppContext contexto, ViewNotifier notificador)` / `JavaBridge(AppContext contexto, ViewNotifier notificador, ScreenTransition transicion, Executor hiloDeDatos)` | — | Constructores |
| `transicionPantalla(double x, double y, double ancho, double alto, int milisegundos)` | `String` | Pide a la ventana que desvanezca una imagen de la zona indicada por encima de la página (ver ScreenTransition) |
| `obtenerInforme(String desde, String hasta, String anteriorDesde, String anteriorHasta)` | `String` | El informe en JSON o "ERROR: ..." |
| `exportarInformeExcel(String desde, String hasta, String anteriorDesde, String anteriorHasta)` | `String` | Guarda el informe como .xlsx en la carpeta Documentos/TBalance y lo abre |
| `asistenteEstado()` | `String` | {"configurado":bool,"modelo":"..."} |
| `asistenteConfigurar(String clave, String modelo)` | `String` | "OK" o "ERROR: ..." |
| `asistenteEnviar(String mensaje)` | `String` | Envía un mensaje; la respuesta llega después con onAsistenteRespuesta |
| `asistenteProbar()` | `String` | Prueba la clave y la conexión con un mensaje corto; el resultado llega con onAsistentePrueba({"ok":bool,"mensaje":"...")} |
| `asistenteProbarClave(String clave, String modelo)` | `String` | "OK" o "ERROR: ..." |
| `asistenteConfirmar(String propuestaId)` | `String` | Descripción de lo registrado o "ERROR: ..." |
| `asistenteDescartar(String propuestaId)` | `String` | "OK" |
| `asistenteReiniciar()` | `String` | "OK" |
| `estadoSesion()` | `String` | JSON con primeraVez y la sesión abierta (o null) |
| `iniciarSesion(String usuario, String clave)` | `String` | Estado de la sesión, o "ERROR: ..." |
| `listarHistorial(String filtrosJson)` | `String` | Historial de acciones visible para quien consulta |
| `crearPrimerDueno(String nombre, String usuario, String clave)` | `String` | Estado de la sesión, o "ERROR: ..." |
| `cerrarSesion()` | `String` | "OK" |
| `cambiarMiClave(String actual, String nueva)` | `String` | "OK" o "ERROR: ..." |
| `listarUsuarios()` | `String` | Personas del negocio abierto |
| `guardarUsuario(String json)` | `String` | La persona, o "ERROR: ..." |
| `cambiarEstadoUsuario(String id, String activo)` | `String` | "OK" o "ERROR: ..." |
| `restablecerClaveUsuario(String id, String nueva)` | `String` | "OK" o "ERROR: ..." |
| `listarPerfiles()` | `String` | Negocios a los que entra la persona con sesión (también los deshabilitados, para el dueño) |
| `obtenerPerfilActivo()` | `String` | Devuelve perfil activo |
| `obtenerSugerencias()` | `String` | Devuelve sugerencias |
| `requiereControlVencimiento()` | `boolean` | True si los productos de este negocio manejan fecha de vencimiento |
| `obtenerTiposNegocio()` | `String` | Devuelve tipos negocio |
| `crearPerfil(String jsonPerfil)` | `String` | Crea perfil |
| `editarPerfil(String jsonPerfil)` | `String` | Edita perfil |
| `deshabilitarPerfil(String id)` | `String` | "OK" o "ERROR: ..." |
| `habilitarPerfil(String id)` | `String` | "OK" o "ERROR: ..." |
| `seleccionarPerfil(String id)` | `String` | Selecciona perfil |
| `obtenerEstadisticas()` | `String` | Indicadores clave del perfil activo en JSON |
| `obtenerProductosVencidos()` | `String` | Devuelve productos vencidos |
| `obtenerProductosPorVencer(int dias)` | `String` | Devuelve productos por vencer |
| `obtenerProductos()` | `String` | Devuelve productos |
| `buscarProductos(String texto)` | `String` | Busca productos |
| `filtrarProductosPorCategoria(String categoria)` | `String` | Filtra productos por categoria |
| `guardarProducto(String jsonProducto)` | `String` | Guarda producto |
| `obtenerProductosDeshabilitados()` | `String` | Productos deshabilitados |
| `cambiarEstadoProducto(String id, String activo)` | `String` | "OK" o "ERROR: ..." |
| `obtenerCategorias()` | `String` | Devuelve categorias |
| `guardarCategoria(String jsonCategoria)` | `String` | Guarda categoria |
| `obtenerCategoriasDeshabilitadas()` | `String` | Categorías deshabilitadas |
| `cambiarEstadoCategoria(String id, String activa)` | `String` | "OK" o "ERROR: ..." |
| `obtenerProveedores()` | `String` | Devuelve proveedores |
| `guardarProveedor(String jsonProveedor)` | `String` | Guarda proveedor |
| `obtenerProveedoresDeshabilitados()` | `String` | Proveedores deshabilitados |
| `cambiarEstadoProveedor(String id, String activo)` | `String` | "OK" o "ERROR: ..." |
| `obtenerEntradas()` | `String` | Devuelve entradas |
| `registrarEntrada(String jsonEntrada)` | `String` | La entrada registrada |
| `obtenerSalidas()` | `String` | Devuelve salidas |
| `registrarSalida(String jsonSalida)` | `String` | La salida registrada |
| `obtenerVentas()` | `String` | Devuelve ventas |
| `crearVenta(String jsonVenta)` | `String` | Crea venta |
| `obtenerVentasAnuladas()` | `String` | Ventas anuladas |
| `obtenerEntradasAnuladas()` | `String` | Entradas anuladas |
| `obtenerSalidasAnuladas()` | `String` | Salidas anuladas |
| `anularVenta(String id, String motivo)` | `String` | "OK" o "ERROR: ..." |
| `anularEntrada(String id, String motivo)` | `String` | "OK" o "ERROR: ..." |
| `anularSalida(String id, String motivo)` | `String` | "OK" o "ERROR: ..." |
| `listarRespaldos()` | `String` | Lista respaldos |
| `crearRespaldo()` | `String` | Crea respaldo |
| `restaurarRespaldo(String nombre)` | `String` | Restaura un respaldo y pide a la vista que recargue todos los datos |
| `eliminarRespaldo(String nombre)` | `String` | Elimina respaldo |
| `obtenerCarpetaRespaldos()` | `String` | La carpeta de las copias o, con base de datos, en qué base están |
| `obtenerCostos(String desde, String hasta)` | `String` | Costos del inventario y compras del periodo |
| `conectarBaseExistente(String datosJson)` | `String` | Mensaje o "ERROR: ..." (solo funciona antes de que existan usuarios en este computador) |
| `revisarCambios()` | `String` | Número que cambia cuando otro equipo guardó algo en la misma base |
| `guardarQrCobro(String perfilId, String dataUrl)` | `String` | OK o el error |
| `obtenerQrCobro(String perfilId)` | `String` | La imagen del QR de cobro o vacío |
| `resumenFiados()` | `String` | Quién debe y cuánto, sin el detalle (para la pantalla Hoy) |
| `listarFiados()` | `String` | Quién debe, cuánto y lo abonado |
| `obtenerFiado(String clave)` | `String` | Su cuenta con las ventas fiadas y los abonos |
| `registrarAbono(String jsonAbono)` | `String` | El abono registrado |
| `anularAbono(String id, String motivo)` | `String` | OK o el error |
| `obtenerAlmacenamiento()` | `String` | Configuración del almacenamiento (sin la clave) |
| `probarAlmacenamiento(String datosJson)` | `String` | Resultado de la prueba de conexión |
| `guardarAlmacenamiento(String datosJson)` | `String` | Mensaje del cambio de almacenamiento |
| `obtenerEtiquetaQR(String productoId)` | `String` | JSON con la imagen de la etiqueta y el texto que contiene el QR |
| `guardarEtiquetaQR(String productoId)` | `String` | Guarda la etiqueta como PNG y la abre con el visor de imágenes |
| `generarHojaEtiquetas()` | `String` | Genera las hojas de etiquetas de todos los productos del perfil activo y abre la carpeta donde quedaron |
| `iniciarEscaneoQR()` | `String` | Enciende la cámara y empieza a buscar etiquetas QR |
| `iniciarEscaneoQRCon(String nombre)` | `String` | "OK" o "ERROR: ..." |
| `listarCamaras()` | `String` | "OK"; la lista llega con tbCamarasDisponibles(...) |
| `prepararCelular()` | `String` | JSON con la dirección y el QR para conectar el celular, o "ERROR: ..." |
| `celularConectado()` | `boolean` | True si el celular está enviando imágenes |
| `detenerEscaneoQR()` | `String` | Apaga la cámara |
| `recargarEstado()` | `void` | Vuelve a cargar estado |
| `mostrarToast(String mensaje, String tipo)` | `void` | Muestra toast |

##### LabelController

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Recibe las acciones de la pantalla etiquetas con código QR. |
| Archivo | [LabelController.java](../src/main/java/sistemainventario/bridge/LabelController.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `contexto` | `AppContext` | Contexto de la aplicación |
| `abridor` | `FileOpener` | Abridor (FileOpener) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `LabelController(AppContext contexto, FileOpener abridor)` | — | Constructor |
| `obtenerEtiquetaQR(String productoId)` | `String` | JSON con la imagen de la etiqueta y el texto que contiene el QR |
| `guardarEtiquetaQR(String productoId)` | `String` | Guarda la etiqueta como PNG y la abre con el visor de imágenes |
| `generarHojaEtiquetas()` | `String` | Genera las hojas de etiquetas de todos los productos del perfil activo y abre la carpeta donde quedaron |

##### ProfileController

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Recibe las acciones de la pantalla perfiles: recibe los eventos de la vista, delega en ProfileService y avisa a la vista cuando cambia el estado. |
| Archivo | [ProfileController.java](../src/main/java/sistemainventario/bridge/ProfileController.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `contexto` | `AppContext` | Contexto de la aplicación |
| `notificador` | `ViewNotifier` | Notificador (ViewNotifier) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `ProfileController(AppContext contexto, ViewNotifier notificador)` | — | Constructor |
| `listarPerfiles()` | `String` | Negocios a los que entra la persona con sesión (también los deshabilitados, para el dueño) |
| `obtenerPerfilActivo()` | `String` | Devuelve perfil activo |
| `obtenerSugerencias()` | `String` | Devuelve sugerencias |
| `requiereControlVencimiento()` | `boolean` | True si los productos de este negocio manejan fecha de vencimiento |
| `obtenerTiposNegocio()` | `String` | Devuelve tipos negocio |
| `crearPerfil(String jsonPerfil)` | `String` | Crea perfil |
| `editarPerfil(String jsonPerfil)` | `String` | Edita perfil |
| `deshabilitarPerfil(String id)` | `String` | Borrado lógico del negocio: queda deshabilitado con todos sus datos |
| `habilitarPerfil(String id)` | `String` | "OK" o "ERROR: ..." |
| `seleccionarPerfil(String id)` | `String` | Selecciona perfil |
| `guardarQrCobro(String perfilId, String dataUrl)` | `String` | Guarda la imagen del QR de cobro (Bre-B) de un negocio, o la quita si llega vacía |
| `obtenerQrCobro(String perfilId)` | `String` | La imagen del QR de cobro como data URL, o vacío si no tiene |

##### ReportController

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Entrega a la pantalla los informes de ventas y los exporta a Excel. |
| Archivo | [ReportController.java](../src/main/java/sistemainventario/bridge/ReportController.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `contexto` | `AppContext` | Contexto de la aplicación |
| `abridor` | `FileOpener` | Abridor (FileOpener) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `ReportController(AppContext contexto, FileOpener abridor)` | — | Constructor |
| `obtenerInforme(String desde, String hasta, String anteriorDesde, String anteriorHasta)` | `String` | El informe en JSON o "ERROR: ..." |
| `exportarInformeExcel(String desde, String hasta, String anteriorDesde, String anteriorHasta)` | `String` | Guarda el informe como .xlsx en la carpeta Documentos/TBalance y lo abre |

##### ScanController

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Recibe la acción de la pantalla "registrar venta por QR". |
| Archivo | [ScanController.java](../src/main/java/sistemainventario/bridge/ScanController.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `contexto` | `AppContext` | Contexto de la aplicación |
| `camaras` | `CameraProvider` | Camaras (CameraProvider) |
| `camaraElegida` | `String` | Camara elegida |
| `vista` | `ViewNotifier` | Vista (ViewNotifier) |
| `celular` | `PhoneLink` | Celular (PhoneLink) |
| `sesionActiva` | `AtomicBoolean` | Sesion activa |
| `hiloActual` | `Thread` | Hilo actual |

**Constantes:** `PAUSA_ENTRE_CUADROS_MS`, `ANCHO_VISTA_PREVIA`, `ESPERA_SESION_ANTERIOR_MS`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `ScanController(AppContext contexto, CameraProvider camaras, ViewNotifier vista, PhoneLink celular)` | — | Constructor |
| `prepararCelular()` | `String` | Prepara la conexión con el celular: abre el servidor en la red local y devuelve el código QR que el celular debe escanear para conectarse |
| `celularConectado()` | `boolean` | True si el celular está enviando imágenes |
| `iniciarEscaneoQR()` | `String` | Enciende la cámara y empieza a buscar etiquetas QR |
| `iniciarEscaneoQRCon(String nombre)` | `String` | Enciende una cámara concreta (por ejemplo, el celular usado como cámara web) |
| `listarCamaras()` | `String` | Busca las cámaras en segundo plano (puede tardar unos segundos) y avisa a la vista con tbCamarasDisponibles(["...", ...]) |
| `detenerEscaneoQR()` | `String` | Apaga la cámara |
| `estaEscaneando()` | `boolean` | True mientras la cámara está encendida o encendiéndose |

##### ScreenTransition

| Campo | Valor |
|---|---|
| Tipo | Interfaz |
| Herencia | — |
| Responsabilidad | Transición visual que hace la ventana de JavaFX por encima de la página HTML. |
| Archivo | [ScreenTransition.java](../src/main/java/sistemainventario/bridge/ScreenTransition.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `desvanecerZona(double x, double y, double ancho, double alto, int milisegundos)` *(abstracto)* | `void` | Desvanece una imagen de la zona indicada, encima de la página |

##### SessionController

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Recibe las acciones del inicio de sesión y de los usuarios de cada negocio. |
| Archivo | [SessionController.java](../src/main/java/sistemainventario/bridge/SessionController.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `contexto` | `AppContext` | Contexto de la aplicación |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `SessionController(AppContext contexto)` | — | Constructor |
| `estadoSesion()` | `String` | JSON {primeraVez, sesion}: sesion es null si nadie ha entrado; si no, trae nombre, usuario, rol en el negocio abierto y la lista de permisos |
| `iniciarSesion(String usuario, String clave)` | `String` | El estado de la sesión, o "ERROR: ..." |
| `crearPrimerDueno(String nombre, String usuario, String clave)` | `String` | Solo funciona la primera vez, cuando todavía no hay cuentas |
| `cerrarSesion()` | `String` | "OK" |
| `cambiarMiClave(String actual, String nueva)` | `String` | "OK" o "ERROR: ..." |
| `listarUsuarios()` | `String` | Personas del negocio abierto (sin contraseñas) |
| `guardarUsuario(String json)` | `String` | Crea una persona (sin id) o cambia su nombre y rol (con id) |
| `cambiarEstadoUsuario(String id, String activo)` | `String` | "OK" o "ERROR: ..." |
| `restablecerClaveUsuario(String id, String nueva)` | `String` | "OK" o "ERROR: ..." |
| `listarHistorial(String filtrosJson)` | `String` | Historial de acciones que puede ver quien consulta: el dueño ve el de sus negocios y las acciones generales (ingresos, copias de seguridad); el administrador ve el de los negocios que administra |

##### SseNotifier

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | implementa `ViewNotifier` |
| Responsabilidad | Implementación de ViewNotifier para cuando la interfaz se muestra en Edge: envía cada script a la página por un flujo de eventos del servidor (Server-Sent Events) y la página lo ejecuta al recibirlo. |
| Archivo | [SseNotifier.java](../src/main/java/sistemainventario/bridge/SseNotifier.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `conexiones` | `List<OutputStream>` | Conexiones abiertas de la página (normalmente una; dos un instante si se recarga) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `agregar(OutputStream salida)` | `void` | Registra una conexión nueva de la página |
| `quitar(OutputStream salida)` | `void` | Quitar |
| `conectadas()` | `int` | Cuántas páginas están conectadas en este momento |
| `ejecutarScript(String script)` | `void` | Ejecuta script |
| `latido()` | `void` | Envía un comentario vacío para mantener viva la conexión y detectar si se cerró |
| `formatear(String script)` *(static)* | `String` | Da formato de evento al script: cada línea va precedida de data:  |

##### StorageController

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Recibe las acciones de "Dónde se guardan los datos": probar la conexión y cambiar de almacenamiento. |
| Archivo | [StorageController.java](../src/main/java/sistemainventario/bridge/StorageController.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `contexto` | `AppContext` | Contexto de la aplicación |
| `notificador` | `ViewNotifier` | Notificador (ViewNotifier) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `StorageController(AppContext contexto, ViewNotifier notificador)` | — | Constructor |
| `revisarCambios()` | `String` | Número que cambia cuando otro equipo guardó algo en la misma base; la pantalla lo consulta cada pocos segundos para actualizarse sola |
| `obtenerAlmacenamiento()` | `String` | JSON con la URL, el usuario, si hay conexión y el aviso pendiente |
| `probarAlmacenamiento(String datosJson)` | `String` | El resultado de la prueba o "ERROR: ..." |
| `guardarAlmacenamiento(String datosJson)` | `String` | Guarda la configuración y empieza a usarla (crea tablas y copia datos si hace falta) |
| `conectarBaseExistente(String datosJson)` | `String` | Desde la pantalla de inicio de un computador nuevo (antes de que exista algún usuario), se conecta a la base que ya usa el negocio, por ejemplo en la nube |

##### TransactionController

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Recibe las acciones de la pantalla que mueven stock: entradas, salidas y ventas. |
| Archivo | [TransactionController.java](../src/main/java/sistemainventario/bridge/TransactionController.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `contexto` | `AppContext` | Contexto de la aplicación |

**Constantes:** `LISTA_VACIA`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `TransactionController(AppContext contexto)` | — | Constructor |
| `obtenerEntradas()` | `String` | Devuelve entradas |
| `registrarEntrada(String jsonEntrada)` | `String` | La entrada registrada |
| `obtenerSalidas()` | `String` | Devuelve salidas |
| `registrarSalida(String jsonSalida)` | `String` | La salida registrada |
| `obtenerVentas()` | `String` | Devuelve ventas |
| `crearVenta(String jsonVenta)` | `String` | Crea venta |
| `obtenerVentasAnuladas()` | `String` | Ventas anuladas del perfil activo |
| `obtenerEntradasAnuladas()` | `String` | Entradas anuladas del perfil activo |
| `obtenerSalidasAnuladas()` | `String` | Salidas anuladas del perfil activo |
| `anularVenta(String id, String motivo)` | `String` | "OK" o "ERROR: ..." |
| `puedeAnularPropia(User quien, Sale venta)` *(static)* | `boolean` | El cajero se puede equivocar al cobrar: anula su propia venta si no han pasado 10 minutos |
| `anularEntrada(String id, String motivo)` | `String` | "OK" o "ERROR: ..." |
| `anularSalida(String id, String motivo)` | `String` | "OK" o "ERROR: ..." |

##### ViewNotifier

| Campo | Valor |
|---|---|
| Tipo | Interfaz |
| Herencia | — |
| Responsabilidad | Canal para que Java avise a la vista web de un cambio de estado. |
| Archivo | [ViewNotifier.java](../src/main/java/sistemainventario/bridge/ViewNotifier.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `ejecutarScript(String script)` *(abstracto)* | `void` | Ejecuta script |

##### WebViewNotifier

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | implementa `ViewNotifier` |
| Responsabilidad | Implementación de ViewNotifier sobre el WebEngine de JavaFX. |
| Archivo | [WebViewNotifier.java](../src/main/java/sistemainventario/bridge/WebViewNotifier.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `motor` | `WebEngine` | Motor |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `WebViewNotifier(WebEngine motor)` | — | Constructor |
| `ejecutarScript(String script)` | `void` | Ejecuta script |

#### Paquete `view`

##### BrowserWindow

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Abre la interfaz en una ventana de aplicación de Microsoft Edge (o de Google Chrome si no hay Edge): una ventana propia, sin pestañas ni barra de direcciones, que dibuja la página con la tarjeta gráfica. |
| Archivo | [BrowserWindow.java](../src/main/java/sistemainventario/view/BrowserWindow.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `carpetaPerfil` | `Path` | Carpeta perfil |
| `proceso` | `Process` | Proceso |

**Constantes:** `ANCHO`, `ALTO`, `VERSION_ICONO`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `BrowserWindow(Path carpetaPerfil)` | — | Constructor |
| `buscarNavegador()` *(static)* | `Optional<String>` | Ruta del ejecutable de Edge o Chrome, si está instalado |
| `abrir(String navegador, String url)` | `void` | Abre la ventana con la dirección indicada |
| `getProceso()` | `Process` | El proceso del navegador, o null si no se ha abierto |
| `cerrar()` | `void` | Cierra la ventana si sigue abierta |

##### DesktopBrowserApp

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Arranca TBalance mostrando la interfaz en una ventana de Edge (modo aplicación) en lugar del visor de JavaFX. |
| Archivo | [DesktopBrowserApp.java](../src/main/java/sistemainventario/view/DesktopBrowserApp.java) |

**Constantes:** `ESPERA_CIERRE`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `iniciar(AppContext contexto)` *(static)* | `boolean` | Intenta abrir la interfaz en Edge |

##### LocalWebServer

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Servidor HTTP local (solo en 127.0.0.1) que entrega la interfaz a Edge y conecta la página con Java. |
| Archivo | [LocalWebServer.java](../src/main/java/sistemainventario/view/LocalWebServer.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `invocador` | `BridgeInvoker` | Invocador (BridgeInvoker) |
| `notificador` | `SseNotifier` | Notificador (SseNotifier) |
| `clave` | `String` | Clave |
| `servidor` | `HttpServer` | Servidor |
| `hilos` | `ExecutorService` | Hilos |

**Constantes:** `RAIZ_RECURSOS`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `LocalWebServer(BridgeInvoker invocador, SseNotifier notificador)` | — | Constructor |
| `iniciar()` | `String` | Arranca el servidor en un puerto libre |
| `detener()` | `void` | Detiene el servidor |

##### SnapshotTransition

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | implementa `ScreenTransition` |
| Responsabilidad | Hace las transiciones grandes de la ventana de JavaFX con animaciones de JavaFX. |
| Archivo | [SnapshotTransition.java](../src/main/java/sistemainventario/view/SnapshotTransition.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `raiz` | `StackPane` | Raiz |
| `vista` | `WebView` | Vista |

**Constantes:** `DESENFOQUE_FINAL`, `DESPLAZAMIENTO_FINAL`, `DURACION_MAXIMA`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `SnapshotTransition(StackPane raiz, WebView vista)` | — | Constructor |
| `desvanecerZona(double x, double y, double ancho, double alto, int milisegundos)` | `void` | Desvanece una imagen de la zona indicada, encima de la página |

##### WebViewBridgeAdapter

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Conecta la página con JavaBridge cuando se usa la ventana de JavaFX. |
| Archivo | [WebViewBridgeAdapter.java](../src/main/java/sistemainventario/view/WebViewBridgeAdapter.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `invocador` | `BridgeInvoker` | Invocador (BridgeInvoker) |

**Constantes:** `SCRIPT_PUENTE`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `WebViewBridgeAdapter(BridgeInvoker invocador)` | — | Constructor |
| `llamar(String metodo, String argumentosJson)` | `String` | Respuesta JSON {ok, v} o {ok:false, error} |

##### WebViewMain

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Ventana de JavaFX que muestra la interfaz cuando no hay Edge. |
| Archivo | [WebViewMain.java](../src/main/java/sistemainventario/view/WebViewMain.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `contexto` | `AppContext` | Contexto de la aplicación |
| `motor` | `WebEngine` | Motor |
| `webView` | `WebView` | Web view |
| `raiz` | `StackPane` | Contenedor de la ventana: el visor web y, encima, las imágenes de las transiciones |
| `bridge` | `JavaBridge` | Se conserva como atributo para que el recolector de basura no lo elimine |
| `puenteJs` | `WebViewBridgeAdapter` | Se guarda en un campo para que JavaFX no lo libere mientras la página lo usa |

**Constantes:** `RECURSO_HTML`, `ANCHO_INICIAL`, `ALTO_INICIAL`, `ANCHO_MINIMO`, `ALTO_MINIMO`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `WebViewMain(AppContext contexto)` | — | Constructor |
| `show(Stage ventana)` | `void` | Construye y muestra la ventana principal |
| `actualizarTitulo(Stage ventana)` | `void` | Actualiza titulo |

##### WindowsTaskbarIcon

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Pone el ícono de TBalance en alta resolución en la ventana de Edge (barra de tareas y barra de título de Windows). |
| Archivo | [WindowsTaskbarIcon.java](../src/main/java/sistemainventario/view/WindowsTaskbarIcon.java) |

**Constantes:** `RECURSO`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `aplicar(Path carpeta)` *(static)* | `void` | Empieza a aplicar el ícono (solo en Windows; en otros sistemas no hace nada) |

#### Paquete `dispositivo`

##### Camera

| Campo | Valor |
|---|---|
| Tipo | Interfaz |
| Herencia | — |
| Responsabilidad | Cámara de la que se toman cuadros para leer códigos QR. |
| Archivo | [Camera.java](../src/main/java/sistemainventario/dispositivo/Camera.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `abrir()` *(abstracto)* | `void` | Enciende la cámara |
| `capturar()` *(abstracto)* | `BufferedImage` | El cuadro actual, o null si todavía no hay imagen |
| `cerrar()` *(abstracto)* | `void` | Apaga la cámara; no falla si ya estaba apagada |
| `avisar(String mensaje, boolean correcto)` | `void` | Avisa en la cámara lo que pasó con la última lectura |

##### CameraProvider

| Campo | Valor |
|---|---|
| Tipo | Interfaz |
| Herencia | — |
| Responsabilidad | Cámaras disponibles en el computador. |
| Archivo | [CameraProvider.java](../src/main/java/sistemainventario/dispositivo/CameraProvider.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `listar()` *(abstracto)* | `List<String>` | Nombres de las cámaras conectadas, la predeterminada primero |
| `crear(String nombre)` *(abstracto)* | `Camera` | La cámara, sin abrir |

##### PhoneCamera

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | implementa `Camera` |
| Responsabilidad | Camera que toma las imágenes del celular conectado por Wi-Fi (PhoneLink). |
| Archivo | [PhoneCamera.java](../src/main/java/sistemainventario/dispositivo/PhoneCamera.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `enlace` | `PhoneLink` | Enlace (PhoneLink) |

**Constantes:** `NOMBRE`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `PhoneCamera(PhoneLink enlace)` | — | Constructor |
| `abrir()` | `void` | Enciende la cámara |
| `capturar()` | `BufferedImage` | El cuadro actual, o null si todavía no hay imagen |
| `cerrar()` | `void` | Apaga la cámara; no falla si ya estaba apagada |
| `avisar(String mensaje, boolean correcto)` | `void` | Avisa en la cámara lo que pasó con la última lectura |

##### PhoneLink

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Conexión con el celular por el Wi-Fi del local, para usarlo como lector de etiquetas sin instalar aplicaciones ni usar internet. |
| Archivo | [PhoneLink.java](../src/main/java/sistemainventario/dispositivo/PhoneLink.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `carpeta` | `Path` | Carpeta |
| `servidor` | `HttpsServer` | Servidor |
| `clave` | `String` | Clave |
| `direccion` | `String` | Dirección |
| `ultimaFoto` | `byte[]` | Ultima foto |
| `fotoNueva` | `boolean` | Foto nueva |
| `ultimaFotoMs` | `long` | Ultima foto ms |
| `avisos` | `List<String>` | Avisos |

**Constantes:** `PUERTO_PREFERIDO`, `MAXIMO_BYTES_FOTO`, `DESCONECTADO_MS`, `ALIAS`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `PhoneLink(Path carpeta)` | — | Constructor |
| `iniciar()` | `String` | Abre el servidor si no estaba abierto |
| `detener()` | `void` | Cierra el servidor; la dirección anterior deja de servir |
| `prepararCertificado()` | `void` | Crea el certificado en segundo plano si todavía no existe, para que la primera conexión con el celular no tenga que esperarlo |
| `estaActivo()` | `boolean` | True si el servidor está abierto |
| `celularConectado()` | `boolean` | True si el celular envió una foto hace poco |
| `tomarFotoNueva()` | `BufferedImage` | La foto más reciente si no se había entregado antes, o null |
| `avisar(String mensaje, boolean correcto)` | `void` | Deja un aviso para el celular; se entrega con la respuesta a la próxima foto |
| `direccionLocal()` *(static)* | `Optional<String>` | Dirección IPv4 del computador en la red local (Wi-Fi o cable), sin contar adaptadores virtuales (VirtualBox, VMware, Hyper-V, WSL, Docker) |

##### WebcamCamera

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | implementa `Camera` |
| Responsabilidad | Camera implementada con la webcam del computador mediante la librería webcam-capture. |
| Archivo | [WebcamCamera.java](../src/main/java/sistemainventario/dispositivo/WebcamCamera.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `nombre` | `String` | Nombre |
| `webcam` | `Webcam` | Webcam |

**Constantes:** `ESPERA_DETECCION_MS`, `ANCHO_MAXIMO`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `WebcamCamera()` / `WebcamCamera(String nombre)` | — | Constructores |
| `disponibles()` *(static)* | `List<String>` | Nombres de las cámaras conectadas, la predeterminada primero |
| `abrir()` | `void` | Enciende la cámara |
| `capturar()` | `BufferedImage` | El cuadro actual, o null si todavía no hay imagen |
| `cerrar()` | `void` | Apaga la cámara; no falla si ya estaba apagada |

#### Paquete `util`

##### AssistantLog

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Registro de errores del asistente en ~/.inventariopro/asistente.log, para poder ver qué pasó si no responde. |
| Archivo | [AssistantLog.java](../src/main/java/sistemainventario/util/AssistantLog.java) |

**Constantes:** `ARCHIVO`, `TAMANO_MAXIMO`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `registrar(String mensaje)` *(static)* | `void` | Registra |
| `archivo()` *(static)* | `Path` | Ruta del archivo de registro |

##### DateUtil

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Utilidades de fecha usadas por todo el sistema. |
| Archivo | [DateUtil.java](../src/main/java/sistemainventario/util/DateUtil.java) |

**Constantes:** `FORMATO_FECHA`, `FORMATO_FECHA_HORA`, `FORMATO_VISUAL`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `hoy()` *(static)* | `String` | La fecha actual en formato yyyy-MM-dd |
| `ahora()` *(static)* | `String` | La fecha y hora actual en formato ISO |
| `parsear(String fechaIso)` *(static)* | `Optional<LocalDate>` | Convierte un texto ISO en LocalDate sin lanzar excepciones |
| `formatearFecha(String fechaIso)` *(static)* | `String` | La fecha en formato dd/MM/yyyy, o el texto original si no es válida |
| `parsearMomento(String momentoIso)` *(static)* | `Optional<LocalDateTime>` | El momento, o empty si es nulo o inválido |

##### FileUtil

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Operaciones con carpetas que usan las copias de seguridad. |
| Archivo | [FileUtil.java](../src/main/java/sistemainventario/util/FileUtil.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `carpetaTemporal(String prefijo)` *(static)* | `Path` | Una carpeta temporal nueva |
| `borrarCarpeta(Path carpeta)` *(static)* | `void` | Borrar carpeta |
| `copiarCarpeta(Path origen, Path destino)` *(static)* | `void` | Copia carpeta |

##### IdGenerator

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Generador de identificadores únicos. |
| Archivo | [IdGenerator.java](../src/main/java/sistemainventario/util/IdGenerator.java) |

**Constantes:** `BASE_NUMERICA`, `ULTIMA_MARCA`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `conPrefijo(String prefijo)` *(static)* | `String` | Genera un ID con el formato PREFIJO-marcaDeTiempo |
| `siguienteNumerico(Collection<String> idsExistentes)` *(static)* | `String` | Calcula el siguiente ID numérico a partir de los existentes |

##### ImageEncoder

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Convierte imágenes en texto para mostrarlas en la página. |
| Archivo | [ImageEncoder.java](../src/main/java/sistemainventario/util/ImageEncoder.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `aJpegDataUri(BufferedImage imagen, int anchoMaximo)` *(static)* | `String` | Reduce la imagen si es más ancha que el máximo y la codifica en JPEG, un formato liviano para enviar varios cuadros de cámara por segundo |
| `aPngDataUri(BufferedImage imagen)` *(static)* | `String` | La imagen como data:image/png;base64, |

##### JsonUtil

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Punto único de configuración de Gson para todo el sistema. |
| Archivo | [JsonUtil.java](../src/main/java/sistemainventario/util/JsonUtil.java) |

**Constantes:** `GSON`, `GSON_COMPACTO`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `toJsonCompacto(Object objeto)` *(static)* | `String` | Representación JSON compacta (sin formato) |
| `toJson(Object objeto)` *(static)* | `String` | Representación JSON con formato legible |
| `fromJson(String json, Class<T> clase)` *(static)* | `T` | La instancia deserializada |
| `fromJsonList(String json, Class<T> clase)` *(static)* | `List<T>` | Lista deserializada; vacía si el texto está vacío |
| `getGson()` *(static)* | `Gson` | Get gson |

##### PasswordHasher

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Cifra las contraseñas antes de guardarlas y las comprueba al iniciar sesión. |
| Archivo | [PasswordHasher.java](../src/main/java/sistemainventario/util/PasswordHasher.java) |

**Constantes:** `PREFIJO`, `ITERACIONES`, `BYTES_SAL`, `BITS_RESUMEN`, `AZAR`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `resumir(String clave)` *(static)* | `String` | Texto para guardar (nunca la contraseña misma) |
| `coincide(String clave, String guardado)` *(static)* | `boolean` | True si coinciden (la comparación tarda lo mismo aunque no coincidan) |

##### QrGenerator

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Genera imágenes de códigos QR usando la librería ZXing. |
| Archivo | [QrGenerator.java](../src/main/java/sistemainventario/util/QrGenerator.java) |

**Constantes:** `NEGRO`, `BLANCO`

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `generar(String contenido, int tamanoPx)` | `BufferedImage` | Imagen del código QR |

##### QrReader

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Lee el texto de un código QR que aparece en una imagen (por ejemplo, un cuadro de la cámara). |
| Archivo | [QrReader.java](../src/main/java/sistemainventario/util/QrReader.java) |

**Atributos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `pistas` | `Map<DecodeHintType, Object>` | Pistas |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `QrReader()` | — | Constructor |
| `leer(BufferedImage imagen)` | `Optional<String>` | El texto del QR, o vacío si la imagen no contiene uno legible |

##### TextUtil

| Campo | Valor |
|---|---|
| Tipo | Concreta |
| Herencia | — |
| Responsabilidad | Utilidades puras para el manejo de texto. |
| Archivo | [TextUtil.java](../src/main/java/sistemainventario/util/TextUtil.java) |

**Métodos**

| Método | Retorna | Descripción |
|---|---|---|
| `esVacio(String texto)` *(static)* | `boolean` | True si es null, vacía o solo contiene espacios |
| `contieneIgnorandoMayusculas(String campo, String consulta)` *(static)* | `boolean` | Indica si campo contiene consulta ignorando mayúsculas |
| `normalizar(String texto)` *(static)* | `String` | El texto sin espacios externos y en minúsculas; cadena vacía si es null |
| `recortar(String texto)` *(static)* | `String` | El texto sin espacios externos, o null si era null |

## 3. Relaciones entre clases

| Relación | Clase (todo / padre) | Clase (parte / hija) | Explicación |
|---|---|---|---|
| Herencia | `IdentifiedEntity` | `Product`, `Category`, `Supplier`, `Profile`, `Sale`, `InventoryMovement`, `User`, `AuditEntry`, `CreditPayment` | Todo lo que se guarda tiene un ID; lo heredan de esta clase. |
| Herencia | `InventoryMovement` | `StockEntry`, `StockExit` | Una entrada suma stock y una salida lo resta; cada una sobrescribe `aplicarA(Product)`. |
| Herencia | `JdbcRepository<T>` | `ProductJdbcRepository`, `CategoryJdbcRepository`, `SupplierJdbcRepository`, `StockEntryJdbcRepository`, `StockExitJdbcRepository`, `SaleJdbcRepository`, `ProfileJdbcRepository`, `UserJdbcRepository`, `AuditJdbcRepository`, `CreditPaymentJdbcRepository` | Cada una dice su tabla y sus columnas; consultar, insertar y actualizar lo hace la clase padre. |
| Herencia | `EntityService<T>` | `InventoryService`, `CategoryService`, `SupplierService` | Comparten el paso a paso de guardar; cada una pone su validación. |
| Herencia | `RuntimeException` | `ValidationException`, `PersistenceException`, `AssistantException` | Los errores propios del proyecto. |
| Composición | `Sale` | `SaleItem` | La venta crea sus renglones; un renglón no existe sin su venta. |
| Composición | `User` | `Acceso` | Los accesos (negocio y rol) son parte de la persona y se guardan con ella. |
| Composición | `JavaBridge` | los controladores (`InventoryController`, `TransactionController`, `CreditController`, `SessionController` y los demás) | El puente crea sus controladores y viven mientras él vive. |
| Composición | `ProfileServices` | `InventoryService`, `CategoryService`, `SupplierService`, `MovementService`, `SaleService`, `CreditService` | Los servicios de un negocio se crean juntos y se reemplazan juntos al abrir otro negocio. |
| Composición | `ReportService` | `SalesReport` | El servicio arma el informe completo de un período. |
| Agregación | `CustomerDebt` | `Sale`, `CreditPayment` | La cuenta de un cliente reúne sus ventas fiadas y sus abonos, que existen por su cuenta. |
| Agregación | `CreditService` | `SaleService`, `Repository<CreditPayment>` | Recibe las ventas y los abonos del negocio por el constructor. |
| Agregación | `EntityService<T>`, `MovementService`, `SaleService` | `Repository<T>` | Reciben el repositorio por el constructor; no saben cómo se guarda. |
| Agregación | `AppContext` | `StorageProvider`, `ProfileService`, `BackupService`, `UserService`, `AuditService`, `SessionService` | El contexto guarda estos objetos y los cambia cuando se cambia de base de datos. |
| Agregación | `ProfileServicesFactory` | `StorageProvider` | Pide los repositorios de cada negocio al almacenamiento que esté en uso. |
| Agregación | `JdbcRepository<T>` | `Database` | Todos los repositorios de la base comparten una conexión. |
| Agregación | `BackupService` | `BackupStore`, `DataSnapshot` | Recibe dónde guardar las copias y cómo sacar los datos. |
| Agregación | `AssistantService` | `LanguageModel`, `ProfileServices` | Recibe el modelo de lenguaje y los servicios del negocio abierto. |

## 4. Herencia, interfaces y clases abstractas

### 4.1 Clases abstractas

| Clase | Métodos abstractos | Subclases | Para qué sirve |
|---|---|---|---|
| `IdentifiedEntity` | ninguno | 9 entidades (Product, Sale, User…) | Da a todo lo que se guarda su ID y la comparación por ID. |
| `InventoryMovement` | `aplicarA(Product)`, `getPrefijoId()` | `StockEntry`, `StockExit` | Cada movimiento dice cómo cambia el stock; el servicio no pregunta de qué tipo es. |
| `JdbcRepository<T>` | `tabla()`, `columnas()`, `valores(T)`, `leer(ResultSet)` | 10 repositorios de base de datos | Escribe una sola vez cómo se consulta y se guarda en una tabla. |
| `EntityService<T>` | `validar(T)`, `generarId()`, `nombreEntidad()` | `InventoryService`, `CategoryService`, `SupplierService` | Guardar sigue siempre los mismos pasos; cada servicio completa los suyos. |

### 4.2 Interfaces

| Interfaz | Métodos principales | La implementan | Para qué sirve |
|---|---|---|---|
| `Repository<T>` | listarTodos, buscarPorId, agregar, actualizar, eliminar | `JsonRepository<T>`, `JdbcRepository<T>` | Los servicios guardan igual en Oracle o dentro de una copia de seguridad. |
| `StorageProvider` | perfiles, usuarios, productos, ventas, abonos… | `DatabaseStorageProvider`, `SinConexionStorageProvider`, `JsonStorageProvider` | Entrega todos los repositorios: los de Oracle, los vacíos mientras no hay conexión o los de una copia de seguridad. |
| `DatabaseRoutines` | registrarVenta, anularVenta, registrarAbono, saldoCliente… | `OracleRoutines` | Las operaciones que hace Oracle con sus procedimientos y funciones. |
| `BackupStore` | listar, guardar, leer, eliminar | `DatabaseBackupRepository` | Las copias se guardan dentro de Oracle, en la tabla RESPALDO. |
| `DataSnapshot` | prepararCarpeta, restaurarDesde | `DatabaseSnapshot` | Saca y repone los datos para una copia de seguridad. |
| `Deshabilitable` | isActivo, setActivo | `Product`, `Category`, `Supplier`, `Profile`, `User` | Lo que se deshabilita en lugar de borrarse. |
| `Anulable` | anular, isAnulada | `Sale`, `InventoryMovement`, `CreditPayment` | Lo que se anula con motivo en lugar de borrarse. |
| `Camera` | abrir, capturar, cerrar | `WebcamCamera`, `PhoneCamera` | La venta por QR funciona igual con la webcam o con el celular. |
| `CameraProvider` | listar, crear | clase anónima en `JavaBridge` | Dice qué cámaras hay en el computador. |
| `ViewNotifier` | ejecutarScript | `WebViewNotifier`, `SseNotifier` | Avisarle a la pantalla, en la ventana de JavaFX o en la de Edge. |
| `FileOpener` | abrir | `DesktopFileOpener` | Abrir un archivo o una carpeta con el programa del sistema. |
| `LanguageModel` | generar | `GeminiClient` | El asistente no depende de un proveedor de IA en particular. |
| `ScreenTransition` | desvanecerZona | `SnapshotTransition` | Transiciones de la ventana de JavaFX. |

## 5. Diseño de la interfaz (mockups)

Las imágenes son capturas de la interfaz real de la versión v3.5.0 con datos de ejemplo de una ferretería.

| Pantalla | Imagen |
|---|---|
| Inicio de sesión | ![Inicio de sesión](img/login.png) |
| Hoy (resumen del día) | ![Hoy](img/dashboard.png) |
| Productos | ![Productos](img/productos.png) |
| Formulario de producto | ![Formulario de producto](img/producto-form.png) |
| Etiqueta QR de un producto | ![Etiqueta QR](img/etiqueta-qr.png) |
| Entradas | ![Entradas](img/entradas.png) |
| Ventas (cuaderno de caja por día) | ![Ventas](img/ventas.png) |
| Nueva venta | ![Nueva venta](img/nueva-venta.png) |
| Venta por QR | ![Venta por QR](img/venta-por-qr.png) |
| Fiados | ![Fiados](img/fiados.png) |
| Cuenta de un cliente con sus abonos | ![Cuenta de un cliente](img/fiado-cliente.png) |
| Informes | ![Informes](img/informes.png) |
| Informe en PDF | ![Informe en PDF](img/informe-pdf.png) |
| Costos | ![Costos](img/costos.png) |
| Mis negocios | ![Mis negocios](img/perfiles.png) |
| Usuarios | ![Usuarios](img/usuarios.png) |
| Historial | ![Historial](img/historial.png) |
| Copias de seguridad y base de datos | ![Copias de seguridad](img/respaldos.png) |
| Asistente | ![Asistente](img/asistente.png) |

Todas las pantallas comparten la misma distribución: a la izquierda, el menú con las secciones que permite el rol y el interruptor de modo oscuro; arriba, el asistente y el negocio abierto. El color de los botones cambia según el tipo de negocio (en las capturas, naranja de ferretería). Cada sección empieza con una frase que dice lo importante ("Hoy van $ 32.900 en 1 venta", "Por cobrar: $ 6.835.100"). Los formularios se abren en ventanas con fondo desenfocado.

## 6. Patrones GRASP aplicados

| Patrón | Clase | Razón |
|---|---|---|
| **Creador** | `Sale.agregarItem(...)` | La venta contiene sus renglones, por eso es ella la que crea cada `SaleItem`. |
| **Creador** | `ProfileServicesFactory` | Tiene lo necesario (el negocio y el almacenamiento) para crear los servicios de un negocio. |
| **Creador** | `CreditService.listarCuentas()` | Tiene las ventas fiadas y los abonos, por eso arma la cuenta (`CustomerDebt`) de cada cliente. |
| **Controlador** | `JavaBridge` | Es el único objeto que recibe lo que el usuario hace en la página y se lo pasa al controlador indicado. |
| **Controlador** | `InventoryController`, `TransactionController`, `CreditController`, `SessionController` y los demás del paquete `bridge` | Cada uno atiende un grupo de acciones (productos, ventas, fiados, sesión…) y llama a los servicios; no tiene reglas del negocio. |
| **Experto en información** | `Product` | Tiene el stock, el mínimo y la fecha de vencimiento: él decide si está agotado, si está por vencer y si puede vender cierta cantidad. |
| **Experto en información** | `Sale` | Conoce sus renglones: calcula su total y revisa su medio de pago (un fiado necesita nombre y cédula). |
| **Experto en información** | `CustomerDebt` | Tiene lo fiado y lo abonado de un cliente: calcula cuánto debe. |
| **Experto en información** | `Rol` | Sabe qué permisos tiene cada rol y responde si una persona puede hacer algo. |
| **Bajo acoplamiento** | `Repository<T>` | Los servicios dependen de esta interfaz y no saben cómo se guardan los datos. Por eso se pasó de archivos a Oracle sin tocar los servicios. |
| **Bajo acoplamiento** | `Camera`, `LanguageModel`, `ViewNotifier` | La venta por QR, el asistente y los controladores no dependen de una cámara, de un proveedor de IA ni de una ventana en particular. |
| **Alta cohesión** | Controladores del paquete `bridge` | Cada controlador se ocupa de un solo tema; en Programación II un solo `JavaBridge` hacía todo. |
| **Alta cohesión** | `AccessPolicy` y `AuditTrail` | Uno solo revisa permisos y el otro solo anota el historial; ningún controlador tiene que hacerlo. |
| **Alta cohesión** | `CreditService` | Todo lo de fiados y abonos está en un solo servicio. |

## 7. Principios SOLID aplicados

| Principio | Clase | Argumento |
|---|---|---|
| **SRP** (una sola responsabilidad) | `Product` / `InventoryService` / `ProductJdbcRepository` | El producto cumple sus reglas, el servicio coordina el caso de uso y el repositorio guarda. Cada uno cambia por una sola razón. |
| **SRP** | `AccessPolicy` / `AuditTrail` / controladores | Revisar permisos, anotar el historial y atender la acción del usuario están en clases distintas. |
| **SRP** | `ReportService` / `ReportExportService` / `ReportExcelWriter` | Calcular el informe, decidir dónde se guarda y escribir el archivo de Excel están separados. |
| **SRP** | `UserService` / `SessionService` / `PasswordHasher` | Administrar usuarios, recordar quién inició sesión y cifrar contraseñas están separados. |
| **OCP** (abierto a extensión, cerrado a modificación) | `Repository<T>` con `JsonRepository<T>` y `JdbcRepository<T>` | Para guardar en Oracle se agregaron clases nuevas; los servicios no se modificaron. |
| **OCP** | `StorageProvider` con `DatabaseStorageProvider` y `SinConexionStorageProvider` | El estado "sin conexión" se agregó como otra implementación, sin cambiar los servicios. |
| **OCP** | `InventoryMovement.aplicarA(Product)` | Un tipo de movimiento nuevo sería otra subclase; `MovementService` no cambia. |
| **OCP** | `Camera` con `WebcamCamera` y `PhoneCamera` | El celular como lector se agregó con una clase nueva; la venta por QR no cambió. |

La tabla completa con enlaces al código está en [TRAZABILIDAD.md](TRAZABILIDAD.md).
