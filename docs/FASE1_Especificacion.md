# Fase 1 — Especificación y planificación

Universidad Popular del Cesar · Programación de Computadores III (SS462) · Docente: Ing. Esp. Alfredo Bautista<br>
Integrantes: Kevin Jhosep Mercado · Reinaldo Padilla Pedroza · Dair José Quintero

[README](../README.md) · [Fase 1](FASE1_Especificacion.md) · [Fase 2](FASE2_Arquitectura_y_Diseno.md) · [Fase 3](FASE3_Desarrollo.md) · [Fase 4](FASE4_Documentacion_Tecnica.md) · [Trazabilidad](TRAZABILIDAD.md)

---

## Portada

| | |
|---|---|
| **Proyecto** | TBalance — Sistema de Inventario |
| **Universidad** | Universidad Popular del Cesar |
| **Asignatura** | Programación de Computadores III (SS462) — grupo 3 |
| **Docente** | Ing. Esp. Alfredo Bautista |
| **Integrantes** | Kevin Jhosep Mercado · Reinaldo Padilla Pedroza · Dair José Quintero |
| **Repositorio** | https://github.com/kjhosepmercadom/TBalance |
| **Versión documentada** | v3.4.0 |
| **Lugar y año** | Valledupar, 2026 |

## Resumen

TBalance es una aplicación de escritorio para llevar el inventario de pequeños negocios. Permite tener varios perfiles de negocio independientes y, en cada uno, administrar productos, categorías y proveedores, registrar entradas, salidas y ventas, consultar un dashboard con alertas de stock y vencimiento, ver informes de ventas con la utilidad real y exportarlos a Excel o PDF, hacer copias de seguridad, vender leyendo etiquetas QR con la webcam del computador y pedirle cosas a un asistente con inteligencia artificial. Está desarrollada en Java 17; la interfaz web se muestra en una ventana de Edge (o en JavaFX) y los datos se guardan en PostgreSQL, Oracle o archivos JSON locales. El proyecto parte del sistema construido en Programación II y en Programación III se reorganizó en capas aplicando patrones GRASP, principios SOLID y código limpio, y se le agregaron las copias de seguridad, la venta por QR, los informes, los métodos de pago y el asistente.

## 1. Introducción

### 1.1 Contextualización

Muchas tiendas de barrio, farmacias, ferreterías y negocios pequeños controlan su mercancía en un cuaderno o en una hoja de cálculo. Así es difícil saber en el momento qué productos se están agotando, cuáles están por vencer o cuánto se ha vendido, y registrar cada venta a mano toma tiempo.

### 1.2 Importancia

Un control de inventario confiable evita perder ventas por falta de producto, reduce las pérdidas por mercancía vencida y deja un registro de cada movimiento. Para un negocio pequeño es importante que la herramienta sea gratuita, sencilla y que funcione sin internet.

### 1.3 Breve descripción

TBalance es un programa de escritorio que funciona sin conexión (solo el asistente necesita internet). El usuario crea un perfil por negocio (eligiendo el tipo: boutique, farmacia, ferretería, supermercado, tienda de barrio, tecnología u otro), registra sus productos y a partir de ahí cada entrada, salida o venta actualiza el stock automáticamente. El dashboard muestra las alertas, la sección Informes resume las ventas y la utilidad de cualquier período, se pueden hacer copias de seguridad de todos los datos y cada producto puede tener una etiqueta QR que se lee con la webcam para vender más rápido. Con conexión a internet, un asistente responde preguntas como “¿qué le falta de Postobón?” cuando llega el proveedor y prepara ventas o entradas para que el usuario las confirme.

## 2. Planteamiento del problema

### 2.1 Identificación del problema

Los negocios pequeños no tienen una forma sencilla y económica de saber en todo momento cuánto stock tienen, qué productos están en bajo stock, agotados o por vencer, y cuánto han vendido. El registro manual es lento, se presta a errores y, si se pierde el cuaderno o el archivo, se pierde la información.

### 2.2 Justificación

Se desarrolla TBalance porque responde a esa necesidad con una herramienta gratuita, de escritorio y sin internet, que actualiza el stock con cada movimiento, avisa de los productos en riesgo, protege la información con copias de seguridad, agiliza la venta con códigos QR y muestra cuánto se vende y se gana. Desde lo académico, el proyecto permite aplicar los temas de la asignatura: programación orientada a objetos, herencia y polimorfismo, interfaces, colecciones, arquitectura en capas, patrones GRASP y principios SOLID.

### 2.3 Estado del arte / vigilancia tecnológica

| Solución | Tipo | Características | Diferencia con TBalance |
|---|---|---|---|
| Cuaderno u hoja de cálculo (Excel) | Manual | Sin costo adicional, pero cada cálculo y alerta se hace a mano. | TBalance actualiza el stock y genera las alertas automáticamente. |
| Loyverse POS | Punto de venta en la nube | Gratuito, funciona en celulares y tabletas, inventario y reportes. | Depende de la nube y de dispositivos móviles; TBalance es de escritorio y local. |
| Alegra | Software contable y POS en la nube | Facturación, inventario y lector de código de barras; pago por suscripción. | TBalance es gratuito y no requiere cuenta ni suscripción. |
| Odoo Inventory | ERP (módulo de inventario) | Muy completo, escáner de código de barras y QR; gratis para una sola aplicación. | Requiere instalación y configuración de un ERP; TBalance está pensado para un negocio pequeño. |
| **TBalance** | Aplicación de escritorio | Gratuita, sin internet, varios perfiles de negocio, alertas de stock y vencimiento, informes con utilidad real, métodos de pago colombianos, copias de seguridad, venta por QR con la webcam y asistente con IA. | — |

## 3. Objetivos

### 3.1 Objetivo general

Desarrollar una aplicación de escritorio en Java para gestionar el inventario de pequeños negocios, organizada en capas y aplicando patrones GRASP, principios SOLID y buenas prácticas de programación.

### 3.2 Objetivos específicos

1. Reorganizar el sistema de Programación II en capas (presentación, lógica de negocio y acceso a datos) sin perder la compatibilidad con los datos existentes.
2. Modelar el dominio con clases que apliquen encapsulamiento, herencia, polimorfismo, clases abstractas e interfaces.
3. Implementar la gestión de perfiles, productos, categorías, proveedores, entradas, salidas y ventas con validación de los datos.
4. Implementar copias de seguridad para proteger la información del usuario.
5. Implementar etiquetas QR y la venta por QR con la webcam del computador.
6. Implementar informes de ventas con utilidad real y exportación a Excel y PDF, y registrar el método de pago de cada venta.
7. Integrar un asistente con un modelo de lenguaje gratuito que consulte el inventario y proponga cambios que el usuario confirma.
8. Gestionar el desarrollo en equipo con Git y GitHub, usando ramas por integrante y funcionalidad y Pull Requests revisados.

## 4. Descripción de la solución

### 4.1 Descripción general

TBalance se compone de una interfaz web (HTML, CSS y JavaScript) que se muestra en una ventana de Microsoft Edge en modo aplicación; un servidor local (`LocalWebServer`, solo en `127.0.0.1`) entrega la página y atiende sus llamadas. Si no hay Edge ni Chrome, la misma interfaz se muestra en una ventana JavaFX con un `WebView`. En los dos casos la interfaz llama a los métodos de Java a través del objeto `window.javaBridge`; el bridge delega en los servicios, que aplican las reglas de negocio usando las clases del modelo, y los repositorios guardan los datos en PostgreSQL u Oracle (con JDBC) o en archivos JSON. La arquitectura se describe en la [Fase 2](FASE2_Arquitectura_y_Diseno.md).

Secciones de la interfaz: Hoy, Productos, Categorías, Proveedores, Entradas, Salidas, Ventas (con Venta por QR), Informes, Mis negocios (perfiles) y Copias de seguridad, más el asistente flotante disponible en todas.

### 4.2 Requerimientos funcionales

| Código | Requerimiento | Descripción |
|---|---|---|
| RF01 | Gestionar perfiles de negocio | Crear, editar, eliminar y seleccionar perfiles. Cada perfil tiene un tipo de negocio (`BusinessType`) que trae categorías predefinidas y textos de ejemplo, y guarda sus datos en una carpeta propia. |
| RF02 | Gestionar productos | Registrar, editar y eliminar productos con nombre, código, descripción, categoría, proveedor, precio de compra y de venta, stock, stock mínimo y fecha de vencimiento. |
| RF03 | Buscar y filtrar productos | Buscar por nombre, descripción, código o ID y filtrar por categoría. |
| RF04 | Gestionar categorías | Crear, editar y eliminar las categorías del perfil. |
| RF05 | Gestionar proveedores | Crear, editar y eliminar proveedores (nombre, contacto, teléfono, email, dirección, categoría y estado). |
| RF06 | Registrar entradas | Registrar el ingreso de mercancía de un producto; el stock aumenta automáticamente. |
| RF07 | Registrar salidas | Registrar la salida de unidades con su motivo; el stock disminuye y no se permite sacar más de lo disponible. |
| RF08 | Registrar ventas | Registrar una venta con uno o varios productos, cliente, método de pago y observación; se valida el stock de todos los ítems antes de descontarlo y se guarda el costo de cada producto en ese momento. |
| RF09 | Consultar el resumen del día (Hoy) | Ver en una frase lo vendido hoy, el número de ventas y la ganancia, comparado con el mismo día de la semana anterior; lo que está por resolver ordenado por urgencia (productos vencidos o por vencer en 7 días, agotados, con stock bajo, ventas fiadas y productos sin precio de compra); las ventas del día y los pedidos sugeridos por proveedor, que se pueden copiar. |
| RF10 | Copias de seguridad | Crear una copia `.zip` de todos los datos, listarlas, restaurar una (guardando antes una copia del estado actual) y eliminarlas. |
| RF11 | Etiquetas QR | Ver y guardar la etiqueta con código QR de un producto y generar hojas tamaño carta de 18 etiquetas (3 × 6) con todos los productos. |
| RF12 | Venta por QR | Encender la webcam del computador, leer las etiquetas QR y agregar cada producto leído a la venta; ajustar cantidades y registrar la venta. |
| RF13 | Estimar la utilidad | Al registrar o editar un producto se piden el precio de compra y el de venta y se muestra la utilidad por unidad y el margen; la tabla de productos muestra la utilidad de cada uno y el dashboard la utilidad estimada de todo el stock. |
| RF14 | Métodos de pago colombianos | En Nueva venta y Venta por QR se elige el método de pago (`PaymentMethod`): efectivo, Nequi, Daviplata, Bre-B, transferencia Bancolombia, tarjeta débito, tarjeta crédito o fiado. Los pagos digitales admiten el número de comprobante y el fiado exige el nombre del cliente. El método se guarda en la venta y se muestra en la tabla de Ventas. |
| RF15 | Informes de ventas | Sección Informes del menú lateral: ventas de hoy, la semana, el mes, el año o un rango, comparadas con el período anterior; número de ventas, ticket promedio, utilidad real y margen; gráfica de ventas y utilidad por día, semana o mes; productos más vendidos (por unidades, dinero o utilidad) y ventas por método de pago. |
| RF16 | Exportar informes | Exportar el informe a Excel (`.xlsx` de cuatro hojas: resumen, ventas por período, productos y detalle de ventas, con totales, filtros y formato de impresión) en `Documentos/TBalance`, y a PDF con un documento A4 propio (encabezado, indicadores, gráfica, tablas y número de página). |
| RF17 | Asistente con inteligencia artificial | Chat flotante (botón o `Ctrl+K`) con un modelo gratuito de Google Gemini que consulta el inventario con funciones: qué reponer y qué pedirle a cada proveedor, buscar productos, resumen de ventas y vencimientos. También prepara ventas, entradas, salidas, cambios de precio y productos nuevos, que solo se registran cuando el usuario pulsa Confirmar en la tarjeta propuesta. |
| RF18 | Elegir dónde se guardan los datos | En Copias de seguridad, elegir archivos de este computador, PostgreSQL u Oracle; escribir la URL JDBC, el usuario y la clave; probar la conexión y empezar a usarla. La primera vez se crean las tablas y los disparadores (validación de productos e historial de precios) y se copian los negocios existentes. |

### 4.3 Requerimientos no funcionales

| Código | Requerimiento | Descripción |
|---|---|---|
| RNF01 | Plataforma | Aplicación de escritorio en Java 17 o superior con JavaFX 21; se construye y ejecuta con Maven. La interfaz se abre en una ventana de Microsoft Edge en modo aplicación (incluido en Windows 10 y 11); si no hay Edge ni Chrome se usa el visor de JavaFX. |
| RNF02 | Funcionamiento sin internet | La interfaz, las fuentes, Tailwind CSS y las librerías se incluyen en el proyecto; todo funciona sin conexión excepto el asistente, que es la única parte que usa internet. |
| RNF03 | Persistencia en base de datos | Los datos se pueden guardar en PostgreSQL (11 o superior) u Oracle (18c XE o superior) mediante JDBC, o en archivos JSON en `~/.inventariopro/data` con el mismo formato de la versión de Programación II. Al conectarse a una base vacía se crean las tablas y los disparadores y se copian los datos de los archivos. |
| RNF04 | Integridad de los datos | Cada archivo se escribe primero en un temporal y luego se reemplaza (`JsonFileStore.escribir`), para no dejar archivos a medias. |
| RNF05 | Validación y mensajes | Las reglas de negocio se validan en el modelo y los servicios; los errores llegan a la vista como `ERROR: mensaje` y se muestran como notificación. |
| RNF06 | Mantenibilidad | Arquitectura en capas, principios GRASP y SOLID, Javadoc y convenciones de nombres de Java. |
| RNF07 | Rendimiento de la cámara | La cámara funciona en un hilo propio (pausa de 90 ms entre cuadros), por lo que la ventana no se congela; una etiqueta quieta se cuenta una sola vez (2 s). |
| RNF08 | Seguridad de archivos | Solo se aceptan nombres de copia `.zip` y de etiqueta `.png` válidos, y al extraer un `.zip` se rechazan rutas fuera de la carpeta destino (*zip slip*). |
| RNF09 | Usabilidad | Diseño plano con base neutra y un color de acento según el tipo de negocio (tienda de barrio amarillo, supermercado rojo, ferretería naranja, farmacia verde azulado, boutique vino, tecnología azul y personalizado verde #315D53), modo oscuro y claro, logo de la T, íconos Lucide y fuente Outfit; las cifras importantes se dicen en frases, el dinero se muestra en pesos colombianos ($ 1.250.000), las ventas se ven como un cuaderno de caja por día y cada operación muestra una notificación; la búsqueda solo aparece en Productos y los botones de venta en Hoy y Ventas. |
| RNF10 | Fluidez | La ventana de Edge dibuja la página con la tarjeta gráfica (unos 60 cuadros por segundo en las transiciones); el indicador del menú, la entrada de las tarjetas, los números y las ventanas modales tienen animaciones propias. En el visor de JavaFX las transiciones grandes se hacen con imágenes de JavaFX (`SnapshotTransition`). |
| RNF11 | Seguridad del asistente y de la ventana | La clave del asistente se guarda solo en el computador (`~/.inventariopro/asistente.json`), fuera de los respaldos y del repositorio. El asistente no cambia datos sin confirmación del usuario. El servidor local de la ventana solo escucha en `127.0.0.1` y exige una clave aleatoria por sesión. |
| RNF12 | Concurrencia | El asistente consulta el modelo en su propio hilo; cuando lee o cambia el inventario lo hace con el mismo candado (`JavaBridge.CANDADO_DATOS`) o en el hilo de la interfaz, para no cruzarse con lo que hace el usuario. |

## 5. Gestión del proyecto con Git

### 5.1 Repositorio

| | |
|---|---|
| **Nombre** | TBalance |
| **URL** | https://github.com/kjhosepmercadom/TBalance |
| **Descripción** | Sistema de inventario de escritorio en Java 17 para pequeños negocios, con perfiles, ventas, informes, copias de seguridad, venta por QR y asistente con IA. Proyecto de aula de Programación III. |

### 5.2 Roles del equipo

| Integrante | Usuario GitHub | Rol | Responsabilidades |
|---|---|---|---|
| Kevin Jhosep Mercado | @kjhosepmercadom | Líder e integrador | Dueño del repositorio; capa de servicios, `AppContext` y controladores del bridge; Pull Requests de `dev` a `main` y etiquetas de versión. |
| Reinaldo Padilla Pedroza | @rjpadillap | Persistencia, dispositivos y documentación | Capa de acceso a datos (repositorios JSON, respaldos, etiquetas), librerías QR y cámara; documentación por fases y diagramas. |
| Dair José Quintero | @djosequintero | Modelo e interfaz | Modelo de dominio, interfaz web (HTML/CSS/JS), ventana de Edge y capturas. |

Cada Pull Request es revisado y aprobado por otro integrante antes de fusionarse.

### 5.3 Estrategia de ramas

| Rama | Uso |
|---|---|
| `main` | Versiones entregadas; solo recibe fusiones desde `dev` y cada fusión se marca con una etiqueta (`v3.0.0-avance1`, `v3.1.0`, `v3.2.0`, `v3.3.0`, `v3.4.0`). |
| `dev` | Integración del equipo; recibe los Pull Requests de las ramas de funcionalidad. |
| `feature/<integrante>-<funcionalidad>` | Una rama por integrante y funcionalidad (por ejemplo `feature/reinaldo-persistencia-json`). Se borra después de fusionarse. |

Convención de commits: `tipo(alcance): descripción`, con los tipos `feat`, `refactor`, `fix`, `docs`, `build` y `chore`.

### 5.4 Tareas por integrante

| Integrante | Tarea | Rama | Versión |
|---|---|---|---|
| Kevin Mercado | Importar el código de P2 y configurar el repositorio | `main` | v3.0.0-avance1 |
| Dair Quintero | Modelo de dominio: `Product` y `Sale` como expertos, jerarquía `InventoryMovement`, `IdentifiedEntity` encapsulada | `feature/dair-modelo-dominio` | v3.0.0-avance1 |
| Reinaldo Padilla | Persistencia: `Repository<T>`, `JsonRepository<T>`, `JsonFileStore`, `ProfileDirectory`; quitar código muerto | `feature/reinaldo-persistencia-json` | v3.0.0-avance1 |
| Kevin Mercado | Servicios con `EntityService<T>`, inyección de dependencias, `AppContext` y `JavaBridge` con controladores | `feature/kevin-servicios-bridge` | v3.0.0-avance1 |
| Reinaldo Padilla | Respaldos: `ZipArchiver`, `Backup`, `BackupRepository` | `feature/reinaldo-backup-storage` | v3.1.0 |
| Kevin Mercado | `BackupService`, recarga del `AppContext` y `BackupController` | `feature/kevin-backup-service` | v3.1.0 |
| Dair Quintero | Sección Respaldos en la interfaz y documentación | `feature/dair-backup-ui` | v3.1.0 |
| Reinaldo Padilla | Documentación de arquitectura de respaldos | `feature/reinaldo-architecture-docs` | v3.1.0 |
| Reinaldo Padilla | Dependencias ZXing y webcam-capture, `QrGenerator`, `QrReader`, `LabelRepository`, `Camera` | `feature/reinaldo-qr-reader-camera` | v3.2.0 |
| Kevin Mercado | QR en `Product`, `LabelService`, `QrSaleScanner`, `LabelController`, `ScanController` | `feature/kevin-qr-sale-service` | v3.2.0 |
| Dair Quintero | Etiquetas QR en Productos y pantalla Venta por QR | `feature/dair-qr-sale-ui` | v3.2.0 |
| Kevin Mercado | Nombres de las clases en inglés y utilidad por producto | `feature/kevin-english-names-profit` | v3.3.0 |
| Dair Quintero | Precio de compra y venta, columna de utilidad e interfaz rediseñada | `feature/dair-profit-ui-redesign` | v3.3.0 |
| Reinaldo Padilla | Documentación por fases (Fase 1 a 4) | `feature/reinaldo-docs-v3.3.0` | v3.3.0 |
| Kevin Mercado | Métodos de pago (`PaymentMethod`), informes (`ReportService`, Excel) y asistente (`ia`, `AssistantService`) | `feature/kevin-payments-reports-assistant` | v3.4.0 |
| Dair Quintero | Ventana de Edge, interfaz plana con color por tipo de negocio, pantalla Hoy, cuaderno de caja, Informes, asistente y métodos de pago, capturas | `feature/dair-edge-window-new-ui` | v3.4.0 |
| Reinaldo Padilla | Documentación v3.4.0: diagramas, requerimientos, clases nuevas y trazabilidad | `feature/reinaldo-docs-v3.4.0` | v3.4.0 |

El estado de cada tarea y la evidencia de los commits están en la [Fase 3](FASE3_Desarrollo.md#6-gestión-del-proyecto-con-git).

## 6. Conclusiones

1. Separar el sistema en capas permitió agregar las copias de seguridad y la venta por QR creando clases nuevas en cada capa, sin cambiar el formato de los datos ni romper lo que ya funcionaba.
2. Aplicar Experto en Información hizo que las reglas (stock, vencimiento, total de una venta) queden en la clase que tiene los datos, en lugar de repetirse en los servicios.
3. Depender de interfaces (`Repository<T>`, `Camera`, `ViewNotifier`, `FileOpener`) reduce el acoplamiento y deja preparado el cambio a una base de datos relacional. Gracias a `ViewNotifier`, la interfaz pudo pasar del visor de JavaFX a una ventana de Edge agregando `SseNotifier`, sin cambiar los controladores.
4. La interfaz `LanguageModel` aísla al asistente del proveedor: cuando Google retiró un modelo solo cambió `GeminiClient`, y el asistente nunca registra nada sin la confirmación del usuario.
5. Trabajar con ramas por integrante y Pull Requests aprobados por otro compañero dejó un historial claro de quién hizo cada parte.

## 7. Bibliografía

- Larman, C. (2004). *UML y patrones: una introducción al análisis y diseño orientado a objetos y al proceso unificado* (2.ª ed.). Pearson.
- Martin, R. C. (2009). *Código limpio: manual de estilo para el desarrollo ágil de software*. Anaya Multimedia.
- Martin, R. C. (2018). *Arquitectura limpia: guía para especialistas en la estructura y el diseño de software*. Anaya Multimedia.
- Gamma, E., Helm, R., Johnson, R. y Vlissides, J. (2003). *Patrones de diseño*. Addison-Wesley.
- Chacon, S. y Straub, B. (2014). *Pro Git* (2.ª ed.). Apress. https://git-scm.com/book/es/v2
- Oracle. *Java SE 17 Documentation*. https://docs.oracle.com/en/java/javase/17/
- OpenJFX. *JavaFX 21 Documentation*. https://openjfx.io/
- Google. *Gson*. https://github.com/google/gson
- ZXing Project. *ZXing (“Zebra Crossing”)*. https://github.com/zxing/zxing
- Firyn, B. *Webcam Capture*. https://github.com/sarxos/webcam-capture
- Loyverse POS en Capterra. https://www.capterra.es/software/150632/loyverse-pos
- Alegra. *Cómo aprovechar al máximo Alegra si tienes un negocio de retail*. https://blog.alegra.com/colombia/como-aprovechar-al-maximo-alegra-si-tienes-un-negocio-de-retail/
- Odoo. *Inventory*. https://www.odoo.com/app/inventory
- Google. *Gemini API: llamadas a funciones*. https://ai.google.dev/gemini-api/docs/function-calling
- Google. *Guía para desarrolladores de Gemini 3*. https://ai.google.dev/gemini-api/docs/gemini-3
- Microsoft. *Microsoft Edge: opciones de línea de comandos (modo aplicación)*. https://learn.microsoft.com/es-es/deployedge/
- Ecma International. *ECMA-376 Office Open XML File Formats*. https://ecma-international.org/publications-and-standards/standards/ecma-376/
