# TBalance — Sistema de Inventario

Aplicación de escritorio en **Java 17 + JavaFX** para llevar el inventario de pequeños negocios con perfiles independientes, ventas, copias de seguridad y venta por QR con la webcam. Los datos se guardan en archivos **JSON** locales y funciona sin internet.

Proyecto de aula de **Programación de Computadores III (SS462)** — Universidad Popular del Cesar. Docente: Ing. Esp. Alfredo Bautista. Evolución del sistema TBalance de Programación II.

## Equipo

| Integrante | GitHub | Rol |
|---|---|---|
| Kevin Jhosep Mercado | [@kjhosepmercadom](https://github.com/kjhosepmercadom) | Líder e integrador: servicios, `AppContext`, controladores del bridge, versiones |
| Reinaldo Padilla Pedroza | [@rjpadillap](https://github.com/rjpadillap) | Persistencia y dispositivos: repositorios JSON, respaldos, QR y cámara |
| Dair José Quintero | [@djosequintero](https://github.com/djosequintero) | Modelo de dominio, interfaz web y documentación |

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

Requisitos: JDK 17 o superior (se recomienda **JDK 21**). JavaFX, Gson, ZXing y webcam-capture se descargan con Maven.

## Estructura

```
src/main/java/sistemainventario/
├── AppContext, Main, Launcher   arranque y composición
├── model/        entidades del dominio
├── exception/    excepciones del proyecto
├── repository/   acceso a datos (JSON, respaldos, etiquetas)
├── service/      casos de uso
├── bridge/       controladores que atienden la vista
├── view/         ventana JavaFX con el WebView
├── dispositivo/  cámara
└── util/         utilidades (fechas, IDs, texto, JSON, QR)
src/main/resources/html/   interfaz (HTML, Tailwind CSS, css/tbalance.css, JavaScript y fuentes)
docs/                      documentación por fases, diagramas e imágenes
```

## Datos

```
~/.inventariopro/data/configuracion-global.json   perfil activo y lista de perfiles
~/.inventariopro/data/perfiles.json               perfiles creados
~/.inventariopro/data/perfiles/<ID>/              productos, categorias, proveedores, entradas, salidas y ventas (.json)
~/.inventariopro/respaldos/                       copias de seguridad .zip
~/.inventariopro/etiquetas/                       etiquetas QR .png
```

## Ramas

`main` (versiones entregadas) ← `dev` (integración) ← `feature/<integrante>-<funcionalidad>` (una por tarea, con Pull Request aprobado por otro integrante). Detalle en la [Fase 1](docs/FASE1_Especificacion.md#53-estrategia-de-ramas).
