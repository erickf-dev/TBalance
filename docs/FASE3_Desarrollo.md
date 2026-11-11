# Fase 3 — Desarrollo e implementación

Universidad Popular del Cesar · Programación de Computadores III (SS462) · Docente: Ing. Esp. Alfredo Bautista<br>
Integrantes: Kevin Jhosep Mercado · Reinaldo Padilla Pedroza · Dair José Quintero

[README](../README.md) · [Fase 1](FASE1_Especificacion.md) · [Fase 2](FASE2_Arquitectura_y_Diseno.md) · [Fase 3](FASE3_Desarrollo.md) · [Fase 4](FASE4_Documentacion_Tecnica.md) · [Trazabilidad](TRAZABILIDAD.md)

---

## 1. Clases básicas

### 1.1 Encapsulamiento

Todos los atributos de las clases son `private` (o `protected` en `EntityService.repositorio`, para sus subclases). El acceso se hace con getters y setters o con métodos que aplican reglas; por ejemplo, el stock de un producto se cambia con `aumentarStock(int)` y `disminuirStock(int)`, que validan la cantidad, en lugar de `setStock(getStock() - n)`.

### 1.2 Constructores

Las entidades del modelo tienen un constructor sin parámetros porque Gson lo usa para leer el JSON. Los servicios y controladores reciben sus dependencias en el constructor. Ejemplos:

| Clase | Constructores |
|---|---|
| `Product` | `Product()`<br>`Product(String id, String nombre, String descripcion, String categoria, String categoriaId, String proveedorId, double precioCompra, double precioVenta, int stock, int stockMin, String tipo)` |
| `Sale` | `Sale()`<br>`Sale(String id, String cliente, String metodoPago, String observacion, String fecha)` |
| `SaleItem` | `SaleItem()`<br>`(paquete) SaleItem(String productoId, String nombreProducto, int cantidad, double precioUnitario)` |
| `SaleService` | `SaleService(InventoryService inventario, Repository<Sale> ventas)` |
| `ScanController` | `ScanController(AppContext contexto, Supplier<Camera> fabricaCamara, ViewNotifier vista)` |

El constructor con datos de `SaleItem` es de paquete para que solo `Sale.agregarItem` cree ítems. El de `AppContext` es privado porque es un Singleton (`AppContext.get()`).

### 1.3 Getters y setters

Las entidades del modelo exponen sus atributos con getters y setters (listados en la [descripción de cada clase](FASE2_Arquitectura_y_Diseno.md#descripción-de-cada-clase)). Los setters de listas guardan una copia de la lista recibida (`Sale.setItems`, `GlobalConfig.setPerfiles`) para que no se comparta con otro objeto.

## 2. Herencia y polimorfismo

### 2.1 Jerarquías

```
IdentifiedEntity (abstracta)
├── Product
├── Category
├── Supplier
├── Profile
├── Sale
└── InventoryMovement (abstracta)
    ├── StockEntry
    └── StockExit

Repository<T> (interfaz)
└── JsonRepository<T> (abstracta)
    ├── ProductRepository
    ├── CategoryRepository
    ├── SupplierRepository
    ├── StockEntryRepository
    ├── StockExitRepository
    ├── SaleRepository
    └── ProfileRepository

EntityService<T> (abstracta)
├── InventoryService
├── CategoryService
└── SupplierService
```

### 2.2 Métodos sobrescritos

El proyecto tiene **34 métodos con `@Override`**:

| Clase | Métodos sobrescritos |
|---|---|
| `Main` | `start()` |
| `Category` | `getEtiqueta()` |
| `IdentifiedEntity` | `equals()`, `hashCode()` |
| `Product` | `getEtiqueta()` |
| `Profile` | `getEtiqueta()` |
| `StockEntry` | `aplicarA()`, `getPrefijoId()` |
| `StockExit` | `aplicarA()`, `getPrefijoId()` |
| `Supplier` | `getEtiqueta()`, `toString()` |
| `JsonRepository` | `listarTodos()`, `buscarPorId()`, `existe()`, `filtrar()`, `agregar()`, `actualizar()`, `eliminar()`, `siguienteIdNumerico()` |
| `CategoryService` | `validar()`, `generarId()`, `nombreEntidad()` |
| `InventoryService` | `validar()`, `generarId()`, `nombreEntidad()` |
| `SupplierService` | `validar()`, `generarId()`, `nombreEntidad()` |
| `DesktopFileOpener` | `abrir()` |
| `WebViewNotifier` | `ejecutarScript()` |
| `WebcamCamera` | `abrir()`, `capturar()`, `cerrar()` |

Ejemplo de polimorfismo: `MovementService` valida y aplica cualquier `InventoryMovement` llamando a `aplicarA(producto)`; si es una `StockEntry` el stock sube y si es una `StockExit` baja, sin que el servicio pregunte el tipo.

### 2.3 Interfaces

`Repository<T>`, `Camera`, `ViewNotifier` y `FileOpener` (detalle en la [Fase 2](FASE2_Arquitectura_y_Diseno.md#42-interfaces)).

### 2.4 Colecciones

| Colección | Usos (`new`) | Clases | Para qué se usa |
|---|---|---|---|
| `ArrayList` | 16 | `GlobalConfig`, `JsonRepository`, `JsonUtil`, `LabelService`, `Sale`, `SaleService`, `ZipArchiver` | Caché de cada repositorio y las copias que devuelve, ítems de una venta, lista de perfiles, hojas de etiquetas. |
| `LinkedHashMap` | 9 | `BusinessType`, `InventoryController`, `LabelController`, `Profile`, `Sale`, `ScanController` | Mapas que conservan el orden: estadísticas del dashboard, configuración de cada perfil, unidades por producto de una venta, respuestas a la vista. |
| `EnumMap` | 2 | `QrGenerator`, `QrReader` | Opciones de ZXing para leer (`QrReader`) y para generar (`QrGenerator`) códigos QR. |

Las listas se declaran con la interfaz `List<T>` y los mapas con `Map<K, V>`. El proyecto no usa `HashMap` simple: se prefirió `LinkedHashMap` para conservar el orden de inserción al convertir a JSON.

## 3. Persistencia

### 3.1 Implementación actual: archivos JSON con el patrón Repository

| Elemento | Implementación |
|---|---|
| Contrato | `Repository<T extends IdentifiedEntity>`: listar, buscar por ID, filtrar, agregar, actualizar, eliminar y siguiente ID. |
| Implementación | `JsonRepository<T>`: mantiene una caché `List<T>` y la guarda completa en el archivo en cada cambio. |
| Escritura en disco | `JsonFileStore`: lee y escribe con UTF-8; escribe en un `.tmp` y luego reemplaza el archivo. |
| Conversión | `JsonUtil` (Gson). |
| Ubicación | `~/.inventariopro/data/configuracion-global.json`, `perfiles.json` y `perfiles/<ID>/` con `productos.json`, `categorias.json`, `proveedores.json`, `entradas.json`, `salidas.json` y `ventas.json`. |
| Errores | `PersistenceException`. |

El diccionario de los datos guardados está en la [Fase 4](FASE4_Documentacion_Tecnica.md#4-modelo-de-datos).

### 3.2 Base de datos relacional y DAO

**Estado: pendiente.** El proyecto todavía no usa una base de datos relacional. Los servicios dependen solo de la interfaz `Repository<T>`, por lo que la clase DAO con base de datos se agregará como otra implementación de esa interfaz y se conectará en `ProfileServicesFactory`, sin cambiar los servicios ni la interfaz.

## 4. Interfaz gráfica

### 4.1 Tecnología

La interfaz no se construyó con formularios Swing de NetBeans. Se desarrolló en **IntelliJ IDEA** con **JavaFX 21**: `WebViewMain` crea una ventana con un `WebView` que carga `src/main/resources/html/SistemaInventario.html` (HTML, Tailwind CSS y JavaScript, incluidos en el proyecto). La hoja `css/tbalance.css` se carga después de Tailwind y agrega el diseño propio de TBalance (menú con pestaña activa, tarjetas en forma de carpeta, colores naranja y blanco, fuente Bricolage Grotesque para los títulos) y los estilos de la utilidad sin recompilar Tailwind.

### 4.2 Componentes

| Componente | Cantidad / ubicación |
|---|---|
| Ventana JavaFX (`Stage`, `Scene`, `WebView`) | `WebViewMain` (1300 × 800, mínimo 1000 × 650) |
| Secciones | 9: Dashboard, Perfiles, Productos, Categorías, Proveedores, Entradas, Salidas, Ventas, Respaldos |
| Botones (`<button>`) | 91 |
| Campos de texto (`<input>`) | 26 |
| Listas desplegables (`<select>`) | 10 |
| Áreas de texto (`<textarea>`) | 1 |
| Tablas (`<table>`) | 6 |
| Ventanas modales | Formularios de perfil, producto, categoría, proveedor, entrada, salida, venta y etiqueta QR |

### 4.3 Manejo de eventos

| Sentido | Mecanismo | Ejemplo |
|---|---|---|
| Vista → Java | Atributos `onclick` (88), `onchange` (6) y `oninput` (5) llaman funciones JavaScript que invocan `window.javaBridge.<método>()`. | Guardar producto → `javaBridge.guardarProducto(json)` → `InventoryController` → `InventoryService.guardar` |
| Java → Vista | `ViewNotifier.ejecutarScript` ejecuta una función JavaScript con `Platform.runLater`. | `onPerfilesActualizados()`, `onPerfilSeleccionado(id)`, `cargarEstadoDesdeJava()`, `showToast(...)` |
| Cámara → Vista | `ScanController` llama desde su hilo a las funciones `tbCamaraLista`, `tbCuadroCamara`, `tbProductoEscaneado`, `tbAvisoEscaneo`, `tbErrorCamara` y `tbCamaraDetenida`. | Cada producto leído se agrega al carrito de Venta por QR. |

Cada método del bridge devuelve `OK`, un JSON o `ERROR: mensaje` (`BridgeResponse`), y la vista muestra el error como notificación.

### 4.4 Separación en tres capas

| Capa | Qué hace en un evento |
|---|---|
| Presentación (`html`, `view`, `bridge`) | Captura el evento, convierte el JSON y llama al servicio. No tiene reglas de negocio. |
| Lógica de negocio (`service`, `model`) | Valida y aplica las reglas (stock, vencimiento, totales). |
| Acceso a datos (`repository`, `dispositivo`) | Guarda y lee los archivos; maneja la cámara. |

## 5. Buenas prácticas

| Práctica | Evidencia en el código |
|---|---|
| Código limpio | Métodos cortos con nombres que dicen lo que hacen (`disminuirStock`, `cantidadesPorProducto`, `resolverPerfilInicial`); se eliminó el código muerto de P2 (módulo de usuarios, `InventoryService.hoy()`, `diasParaVencer()`, `Sale.addItem()`; `IdGenerator.generar()` se reemplazó por `conPrefijo()`). |
| Javadoc | Todas las clases y los métodos públicos relevantes están documentados con `/** ... */`. |
| Convenciones de nombres | `PascalCase` en clases, `camelCase` en métodos y atributos, `UPPER_SNAKE_CASE` en constantes (`Sale.ESTADO_COMPLETADA`, `StockEntry.PREFIJO_ID`). |
| Constantes en lugar de valores mágicos | Prefijos de ID, estados, nombres de archivo, medidas de la ventana y de las etiquetas. |
| Validación de datos | `validar()` en `Product`, `Category`, `Supplier`, `Profile`, `InventoryMovement`, `SaleItem` y `Sale`; nombres de archivo validados en `BackupRepository` y `LabelRepository`; rutas validadas en `ZipArchiver.extraer`. |
| Manejo de excepciones | `ValidationException` para reglas y `PersistenceException` para errores de archivos; `BridgeResponse.ejecutar` las convierte en `ERROR: mensaje` en un solo lugar. |

La tabla de trazabilidad GRASP / SOLID / Código limpio está en [TRAZABILIDAD.md](TRAZABILIDAD.md).

## 6. Gestión del proyecto con Git

Datos tomados del historial del repositorio hasta la etiqueta `v3.2.0` (2026-10-28).

### 6.1 Resumen de commits por integrante

| Integrante | Usuario GitHub | Commits | Fusiones de PR aprobadas |
|---|---|---|---|
| Kevin Jhosep Mercado | @kjhosepmercadom | 15 | 4 |
| Reinaldo Padilla Pedroza | @rjpadillap | 14 | 5 |
| Dair José Quintero | @djosequintero | 13 | 4 |

Además está el commit `Initial commit` con el que GitHub creó el repositorio. Las fusiones las hace el integrante que revisa y aprueba el Pull Request, nunca su autor.

### 6.2 Evidencia de commits

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

### 6.3 Ramas y fusiones

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

Las ramas `feature/*` se borraron después de fusionarse; en el repositorio quedan `main` y `dev`.

### 6.4 Estado de las tareas

| Integrante | Tarea | Estado | Versión |
|---|---|---|---|
| Kevin Mercado | Importar el código de P2 y configurar el repositorio | Completada | v3.0.0-avance1 |
| Dair Quintero | Corregir `pom.xml` y crear utilidades (`TextUtil`, `DateUtil`, `IdGenerator`) y `ValidationException` | Completada | v3.0.0-avance1 |
| Dair Quintero | Modelo de dominio: Experto en `Product` y `Sale`, jerarquía `InventoryMovement`, `IdentifiedEntity` | Completada | v3.0.0-avance1 |
| Reinaldo Padilla | `Repository<T>`, `JsonRepository<T>`, `JsonFileStore`, `ProfileDirectory`, eliminar módulo de usuarios sin uso | Completada | v3.0.0-avance1 |
| Reinaldo Padilla | Incluir Tailwind CSS y las fuentes para funcionar sin internet | Completada | v3.0.0-avance1 |
| Kevin Mercado | `EntityService<T>` (Template Method), inyección de dependencias, `ProfileServicesFactory`, `AppContext` | Completada | v3.0.0-avance1 |
| Kevin Mercado | `JavaBridge` como fachada con controladores, `BridgeResponse`, `ViewNotifier`, `Launcher` | Completada | v3.0.0-avance1 |
| Reinaldo Padilla | `ZipArchiver`, `Backup`, `BackupRepository` | Completada | v3.1.0 |
| Kevin Mercado | `BackupService`, recarga del `AppContext`, `BackupController` | Completada | v3.1.0 |
| Dair Quintero | Sección Respaldos de la interfaz y notificaciones | Completada | v3.1.0 |
| Reinaldo Padilla | Dependencias ZXing y webcam-capture, `QrGenerator`, `QrReader`, `LabelRepository`, `Camera`, `WebcamCamera` | Completada | v3.2.0 |
| Kevin Mercado | QR en `Product`, `LabelService`, `QrSaleScanner`, `LabelController`, `ScanController` | Completada | v3.2.0 |
| Dair Quintero | Etiquetas QR en Productos y pantalla Venta por QR | Completada | v3.2.0 |
| Kevin Mercado | Nombres de las clases en inglés en todas las capas | Completada | v3.3.0 |
| Kevin Mercado | Utilidad por producto y del inventario (`Product`, `InventoryService`, estadísticas) | Completada | v3.3.0 |
| Dair Quintero | Rediseño de la interfaz: menú con pestaña activa, tarjetas en forma de carpeta, dashboard nuevo e íconos propios (`css/tbalance.css`) | Completada | v3.3.0 |
| Dair Quintero | Precio de compra y de venta en el formulario, columna de utilidad y tarjeta en el dashboard | Completada | v3.3.0 |
| Reinaldo Padilla | Documentación organizada según la estructura del proyecto de aula (Fases 1 a 4) | Completada | v3.3.0 |
| Por asignar | Persistencia en base de datos relacional con DAO (implementación de `Repository<T>`) | Pendiente | — |
| Por asignar | MER y diccionario de datos de la base de datos relacional | Pendiente | — |

