# Guía: base de datos en la nube

[README](../README.md) · [Fase 1](FASE1_Especificacion.md) · [Fase 2](FASE2_Arquitectura_y_Diseno.md) · [Fase 3](FASE3_Desarrollo.md) · [Fase 4](FASE4_Documentacion_Tecnica.md) · [Trazabilidad](TRAZABILIDAD.md)

Con la base de datos en la nube, cada persona usa TBalance en su propio computador y todos trabajan sobre los mismos datos: el cajero vende en la caja y el dueño, desde su casa o desde otro negocio, ve la venta en su pantalla unos segundos después, sin reiniciar nada.

TBalance no necesita cambios para esto: solo hay que crear la base en la nube y escribir su dirección en **Copias de seguridad → Dónde se guardan los datos**. Al pulsar **Guardar y usar**, TBalance crea las tablas y copia los negocios que había.

Se puede usar cualquiera de estas dos opciones gratuitas. Todos los computadores del negocio deben usar **la misma** base.

## Opción A: PostgreSQL en Neon (la más rápida)

1. Entre a https://neon.com y cree una cuenta (no pide tarjeta de crédito).
2. Cree un proyecto (por ejemplo, `tbalance`). Como región, elija la más cercana (por ejemplo, una de Estados Unidos).
3. En el proyecto, pulse **Connect**, elija **Java** y copie la cadena de conexión. Tiene esta forma:

   ```
   jdbc:postgresql://ep-xxxx.us-east-2.aws.neon.tech/neondb?user=neondb_owner&password=XXXX&sslmode=require&channelBinding=require
   ```

4. En TBalance (como dueño): **Copias de seguridad → Dónde se guardan los datos**.
   - Motor: **PostgreSQL**.
   - Dirección: la cadena **sin** la parte de `user` y `password`, por ejemplo `jdbc:postgresql://ep-xxxx.us-east-2.aws.neon.tech/neondb?sslmode=require`.
   - Usuario y contraseña: los que venían en la cadena.
5. Pulse **Probar conexión** y luego **Guardar y usar**.

El plan gratuito trae 1 GB y se "duerme" tras 5 minutos sin uso; la primera consulta después tarda un poco más.

## Opción B: Oracle Autonomous Database (Oracle Cloud, Always Free)

1. Cree una cuenta en https://www.oracle.com/cloud/free/ (pide verificar una tarjeta, pero lo *Always Free* no se cobra).
2. En la consola: **Oracle AI Database → Autonomous AI Database → Create**. Marque **Always Free**, tipo **Transaction Processing**, y escriba la contraseña de ADMIN.
3. Acceso de red: elija **Secure access from allowed IPs and VCNs only** y pulse **Add my IP address** (agrega la IP del computador desde el que está). Agregue también la IP pública de cada lugar donde se use TBalance.
4. Cuando esté **Available**, en la sección **Network**, junto a **Mutual TLS (mTLS) Authentication**, pulse **Edit**, desmarque **Require mutual TLS (mTLS) authentication** y pulse **Update**. Así no se necesita descargar el *wallet*.
5. Abra **Database Actions → SQL** como ADMIN y ejecute el script `src/main/resources/sql/oracle/00_crear_usuario_nube.sql` (cambie la contraseña antes).
6. Pulse **Database connection**, en **TLS Authentication** elija **TLS** y copie la cadena del servicio `_low` o `_tp`. Tiene esta forma:

   ```
   (description=(retry_count=20)(retry_delay=3)(address=(protocol=tcps)(port=1521)(host=adb.sa-bogota-1.oraclecloud.com))(connect_data=(service_name=xxxx_tbalance_tp.adb.oraclecloud.com))(security=(ssl_server_dn_match=yes)))
   ```

7. En TBalance (como dueño): **Copias de seguridad → Dónde se guardan los datos**.
   - Motor: **Oracle**.
   - Dirección: `jdbc:oracle:thin:@` seguido de la cadena copiada.
   - Usuario: `TBALANCE` y la contraseña del script.
8. Pulse **Probar conexión** y luego **Guardar y usar**.

La base gratuita trae 20 GB. Si pasa 7 días sin uso, Oracle la detiene (los datos se conservan) y hay que pulsar **Start** en la consola.

## Los demás computadores

En cada computador que vaya a usar la misma base (la caja, el computador del dueño, otro negocio):

1. Instale TBalance como siempre y ábralo.
2. En la pantalla **Cree la cuenta del dueño**, no la cree: pulse el enlace de abajo, **"¿El negocio ya usa TBalance en otro computador con base de datos…? Conectar a esa base"**.
3. Elija el motor y escriba la misma dirección, usuario y contraseña de la base. Pulse **Conectar**.
4. Aparece la pantalla para entrar: cada persona entra con **su** usuario de TBalance. El dueño crea los usuarios en **Usuarios** desde cualquier computador; aparecen en los demás sin reiniciar.

## Qué pasa cuando hay varios computadores

- Cada 5 segundos, TBalance pregunta a la base si otro computador guardó algo (una consulta muy corta). Si hubo cambios, vuelve a leer los datos y avisa: *"Se actualizaron los datos: hubo cambios desde otro equipo"*. Si la persona tiene una ventana abierta (por ejemplo, una venta a medias), espera a que la cierre.
- Antes de vender o sacar mercancía, TBalance revisa el stock real en la base, para que dos cajas no vendan la misma unidad.
- Si la conexión se corta (por ejemplo, la base se "durmió"), TBalance se vuelve a conectar solo.
- Lo que se elige en cada computador (negocio abierto, modo oscuro, clave del asistente, imagen del QR de cobro) es de ese computador.

## Seguridad

- La contraseña de la base se guarda solo en `C:\Users\<usuario>\.inventariopro\basedatos.json` de cada computador. **No la ponga en el código ni la suba a GitHub.**
- Dé la contraseña de la base solo a quien instale TBalance; los empleados entran con su usuario de TBalance, no con el de la base.
