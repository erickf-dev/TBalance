# TBalance — Sistema de Inventario

Aplicación de escritorio en **Java 17** para llevar el inventario de pequeños negocios con perfiles independientes, ventas con métodos de pago colombianos, informes con utilidad real (Excel y PDF), copias de seguridad, venta por QR con la webcam y un asistente con inteligencia artificial. La interfaz se abre en una ventana de **Microsoft Edge** (o en JavaFX), los datos se guardan en **PostgreSQL**, **Oracle** o archivos **JSON** locales (se elige en la aplicación) y, con archivos o una base local, todo funciona sin internet excepto el asistente.

Proyecto de aula de **Programación de Computadores III (SS462)** — Universidad Popular del Cesar. Docente: Ing. Esp. Alfredo Bautista. Evolución del sistema TBalance de Programación II.

## Equipo

| Integrante | GitHub | Rol |
|---|---|---|
| Kevin Jhosep Mercado | [@kjhosepmercadom](https://github.com/kjhosepmercadom) | Líder e integrador: servicios, `AppContext`, controladores del bridge, versiones |
| Reinaldo Padilla Pedroza | [@rjpadillap](https://github.com/rjpadillap) | Persistencia y dispositivos: repositorios JSON, respaldos, QR y cámara; documentación |
| Dair José Quintero | [@djosequintero](https://github.com/djosequintero) | Modelo de dominio, interfaz web y ventana de Edge |

## Documentación

| Fase | Documento | Contenido |
|---|---|---|
| 1 | [Especificación y planificación](docs/FASE1_Especificacion.md) | Resumen, introducción, planteamiento, estado del arte, objetivos, requerimientos, gestión con Git, conclusiones y bibliografía |
| 2 | [Arquitectura y diseño](docs/FASE2_Arquitectura_y_Diseno.md) | Diagrama de paquetes, diagrama de clases, descripción de cada clase, relaciones, herencia e interfaces, mockups, GRASP y SOLID |
| 3 | [Desarrollo e implementación](docs/FASE3_Desarrollo.md) | Clases, herencia y polimorfismo, colecciones, persistencia, interfaz gráfica, buenas prácticas y evidencia de Git |
| 4 | [Documentación técnica](docs/FASE4_Documentacion_Tecnica.md) | Arquitectura, diagramas finales, modelo de datos, diccionario de datos, tabla consolidada y reporte de Git |
| — | [Trazabilidad](docs/TRAZABILIDAD.md) | GRASP, SOLID y código limpio con enlaces al código |

## Cómo ejecutar

**IntelliJ IDEA:** `File > Open`, seleccionar `pom.xml` (abrir como proyecto), esperar a que Maven descargue las dependencias y ejecutar `sistemainventario.Launcher`.

**Terminal:**

```bash
mvn clean javafx:run
```

Requisitos: JDK 17 o superior (se recomienda **JDK 21**). JavaFX, Gson, ZXing y webcam-capture se descargan con Maven. La ventana usa Microsoft Edge (incluido en Windows 10 y 11); con `-Dtbalance.visor=javafx` se usa el visor de JavaFX.

### Asistente

El asistente usa la capa gratuita de Google Gemini y cada usuario necesita su propia clave:

1. Entrar a https://aistudio.google.com/apikey con una cuenta de Google y pulsar **Create API key**.
2. En TBalance, abrir el asistente (botón redondo abajo a la derecha o `Ctrl+K`), pegar la clave y pulsar **Guardar y probar**.

La clave se guarda solo en `~/.inventariopro/asistente.json`; **no se pone en el código ni se sube a GitHub**. Si algo falla, el detalle queda en `~/.inventariopro/asistente.log`.

## Estructura

```
src/main/java/sistemainventario/
├── AppContext, Main, Launcher   arranque y composición
├── model/        entidades del dominio
├── exception/    excepciones del proyecto
├── repository/   acceso a datos (JSON, PostgreSQL/Oracle con JDBC, respaldos, etiquetas, Excel)
├── ia/           cliente del modelo de lenguaje (Gemini)
├── service/      casos de uso (incluye informes y asistente)
├── bridge/       controladores que atienden la vista
├── view/         ventana de Edge (servidor local) y ventana JavaFX
├── dispositivo/  cámara
└── util/         utilidades (fechas, IDs, texto, JSON, QR)
src/main/resources/html/   interfaz (HTML, Tailwind CSS, css/tbalance.css, css y js/tbalance-vistas, fuentes)
docs/                      documentación por fases, diagramas e imágenes
```

## Datos

```
~/.inventariopro/data/configuracion-global.json   perfil activo y lista de perfiles
~/.inventariopro/data/perfiles.json               perfiles creados
~/.inventariopro/data/perfiles/<ID>/              productos, categorias, proveedores, entradas, salidas y ventas (.json)
~/.inventariopro/respaldos/                       copias de seguridad .zip
~/.inventariopro/etiquetas/                       etiquetas QR .png
~/.inventariopro/asistente.json                   clave y modelo del asistente (solo en este computador)
Documentos/TBalance/                              informes de Excel
```

## Ramas

`main` (versiones entregadas) ← `dev` (integración) ← `feature/<integrante>-<funcionalidad>` (una por tarea, con Pull Request aprobado por otro integrante). Detalle en la [Fase 1](docs/FASE1_Especificacion.md#53-estrategia-de-ramas).
