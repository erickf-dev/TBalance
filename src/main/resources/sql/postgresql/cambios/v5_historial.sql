-- =====================================================================
-- TBalance - PostgreSQL
-- Cambio v5: historial de acciones (quién hizo qué y cuándo) y cédula del
-- cliente en las ventas fiadas.
--
-- Solo agrega: no cambia ni renombra nada de lo anterior, así que las
-- consultas, vistas y funciones existentes siguen funcionando.
-- TBalance lo aplica solo al conectarse (una sola vez). Si prefiere hacerlo
-- a mano: psql -U tbalance -d tbalance -f v5_historial.sql, después de v4.
-- =====================================================================

-- Historial: solo crece. USUARIO y PERFIL se guardan como texto (no llave
-- foránea) para que el registro se conserve tal cual aunque cambien después.
CREATE TABLE AUDITORIA (
  ID          VARCHAR(40)  NOT NULL,
  FECHA       TIMESTAMP    NOT NULL,
  USUARIO_ID  VARCHAR(30),
  USUARIO     VARCHAR(100),
  ROL         VARCHAR(20),
  PERFIL_ID   VARCHAR(20),
  PERFIL      VARCHAR(100),
  ACCION      VARCHAR(60)  NOT NULL,
  DETALLE     VARCHAR(500),
  CONSTRAINT PK_AUDITORIA PRIMARY KEY (ID)
);
CREATE INDEX IX_AUDITORIA_FECHA ON AUDITORIA (FECHA);
CREATE INDEX IX_AUDITORIA_PERFIL ON AUDITORIA (PERFIL_ID, FECHA);

-- Cédula de quien queda debiendo (obligatoria en fiado, la revisa TBalance)
ALTER TABLE VENTA ADD COLUMN CLIENTE_DOCUMENTO VARCHAR(20);
