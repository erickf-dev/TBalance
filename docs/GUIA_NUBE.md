# Guía: base de datos en la nube

[README](../README.md) · [Fase 1](FASE1_Especificacion.md) · [Fase 2](FASE2_Arquitectura_y_Diseno.md) · [Fase 3](FASE3_Desarrollo.md) · [Fase 4](FASE4_Documentacion_Tecnica.md) · [Trazabilidad](TRAZABILIDAD.md)

Con la base de datos en la nube, cada persona usa TBalance en su propio computador y todos trabajan sobre los mismos datos: el cajero vende en la caja y el dueño, desde su casa o desde otro negocio, ve la venta en su pantalla unos segundos después, sin reiniciar nada.

TBalance no necesita cambios para esto: solo hay que crear la base de Oracle en la nube y escribir su dirección en TBalance. Al conectarse, TBalance crea las tablas, las funciones y los procedimientos.

Todos los computadores del negocio deben usar **la misma** base.

## Crear la base: Oracle Autonomous Database (Oracle Cloud, Always Free)

1. Cree una cuenta en https://www.oracle.com/cloud/free/ (pide verificar una tarjeta, pero lo *Always Free* no se cobra).
2. En la consola: **Oracle AI Database → Autonomous AI Database → Create**. Marque **Always Free**, tipo **Transaction Processing**, y escriba la contraseña de ADMIN.
3. Acceso de red: elija **Secure access from allowed IPs and VCNs only** y pulse **Add my IP address** (agrega la IP del computador desde el que está). Agregue también la IP pública de cada lugar donde se use TBalance.
4. Cuando esté **Available**, en la sección **Network**, junto a **Mutual TLS (mTLS) Authentication**, pulse **Edit**, desmarque **Require mutual TLS (mTLS) authentication** y pulse **Update**. Así no se necesita descargar el *wallet*.
5. Abra **Database Actions → SQL** como ADMIN y ejecute el script `src/main/resources/sql/oracle/00_crear_usuario_nube.sql` (cambie la contraseña antes).
6. Pulse **Database connection**, en **TLS Authentication** elija **TLS** y copie la cadena del servicio `_low` o `_tp`. Tiene esta forma:

   ```
   (description=(retry_count=20)(retry_delay=3)(address=(protocol=tcps)(port=1521)(host=adb.sa-bogota-1.oraclecloud.com))(connect_data=(service_name=xxxx_tbalance_tp.adb.oraclecloud.com))(security=(ssl_server_dn_match=yes)))
   ```

7. En TBalance, en la pantalla **Conectar con Oracle** (o, si ya estaba conectado, como dueño en **Copias de seguridad → Conexión con Oracle**):
   - Dirección: `jdbc:oracle:thin:@` seguido de la cadena copiada.
   - Usuario: `TBALANCE` y la contraseña del script.
8. Pulse **Conectar** (o **Probar conexión** y luego **Guardar y usar**).

La base gratuita trae 20 GB. Si pasa 7 días sin uso, Oracle la detiene (los datos se conservan) y hay que pulsar **Start** en la consola.

## Los demás computadores

En cada computador que vaya a usar la misma base (la caja, el computador del dueño, otro negocio):

1. Instale TBalance como siempre y ábralo.
2. Aparece la pantalla **Conectar con Oracle**: escriba la misma dirección, usuario y contraseña de la base y pulse **Conectar**.
3. Aparece la pantalla para entrar: cada persona entra con **su** usuario de TBalance. El dueño crea los usuarios en **Usuarios** desde cualquier computador; aparecen en los demás sin reiniciar.

## Qué pasa cuando hay varios computadores

- Cada 5 segundos, TBalance pregunta a la base si otro computador guardó algo (con la función `FN_VERSION_CAMBIOS`). Si hubo cambios, vuelve a leer los datos y avisa: *"Se actualizaron los datos: hubo cambios desde otro equipo"*. Si la persona tiene una ventana abierta (por ejemplo, una venta a medias), espera a que la cierre.
- Antes de vender o sacar mercancía, TBalance revisa el stock real en la base, para que dos cajas no vendan la misma unidad.
- Si la conexión se corta (por ejemplo, la base se "durmió"), TBalance se vuelve a conectar solo.
- Lo que se elige en cada computador (negocio abierto, modo oscuro, clave del asistente, imagen del QR de cobro) es de ese computador.

## Seguridad

- La contraseña de la base se guarda solo en `C:\Users\<usuario>\.inventariopro\basedatos.json` de cada computador. **No la ponga en el código ni la suba a GitHub.**
- Dé la contraseña de la base solo a quien instale TBalance; los empleados entran con su usuario de TBalance, no con el de la base.
