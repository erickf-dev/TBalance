-- =====================================================================
-- TBalance - Oracle 18c Express Edition
-- Cambio v5: historial de acciones (quién hizo qué y cuándo) y cédula del
-- cliente en las ventas fiadas.
--
-- Solo agrega: no cambia ni renombra nada de lo anterior, así que las
-- consultas, vistas y procedimientos existentes siguen funcionando.
-- TBalance lo aplica solo al conectarse (una sola vez). Si prefiere hacerlo
-- a mano: F5 conectado como TBALANCE, después de v4.
-- =====================================================================

-- Historial: solo crece. USUARIO y PERFIL se guardan como texto (no llave
-- foránea) para que el registro se conserve tal cual aunque cambien después.
CREATE TABLE AUDITORIA (
  ID          VARCHAR2(40 CHAR)  NOT NULL,
  FECHA       TIMESTAMP          NOT NULL,
  USUARIO_ID  VARCHAR2(30 CHAR),
  USUARIO     VARCHAR2(100 CHAR),
  ROL         VARCHAR2(20 CHAR),
  PERFIL_ID   VARCHAR2(20 CHAR),
  PERFIL      VARCHAR2(100 CHAR),
  ACCION      VARCHAR2(60 CHAR)  NOT NULL,
  DETALLE     VARCHAR2(500 CHAR),
  CONSTRAINT PK_AUDITORIA PRIMARY KEY (ID)
);
CREATE INDEX IX_AUDITORIA_FECHA ON AUDITORIA (FECHA);
CREATE INDEX IX_AUDITORIA_PERFIL ON AUDITORIA (PERFIL_ID, FECHA);

-- Cédula de quien queda debiendo (obligatoria en fiado, la revisa TBalance)
ALTER TABLE VENTA ADD (CLIENTE_DOCUMENTO VARCHAR2(20 CHAR));
