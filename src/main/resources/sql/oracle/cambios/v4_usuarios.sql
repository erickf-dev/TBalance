-- =====================================================================
-- TBalance - Oracle 18c Express Edition
-- Cambio v4: personas que usan TBalance, sus negocios y su rol, y quién
-- vende y quién anula.
--
-- TBalance lo aplica solo al conectarse (una sola vez). Si prefiere hacerlo
-- a mano: F5 conectado como TBALANCE, después de v3.
-- =====================================================================

-- La contraseña nunca se guarda: CLAVE tiene su resumen PBKDF2 con sal
CREATE TABLE USUARIO (
  ID                 VARCHAR2(30 CHAR)  NOT NULL,
  NOMBRE             VARCHAR2(100 CHAR) NOT NULL,
  USUARIO            VARCHAR2(30 CHAR)  NOT NULL,
  CLAVE              VARCHAR2(200 CHAR) NOT NULL,
  ACTIVO             NUMBER(1) DEFAULT 1 NOT NULL,
  INTENTOS_FALLIDOS  NUMBER(3) DEFAULT 0 NOT NULL,
  BLOQUEADO_HASTA    TIMESTAMP,
  FECHA_CREACION     TIMESTAMP,
  ULTIMO_INGRESO     TIMESTAMP,
  CONSTRAINT PK_USUARIO PRIMARY KEY (ID),
  CONSTRAINT UQ_USUARIO_USUARIO UNIQUE (USUARIO),
  CONSTRAINT CK_USUARIO_ACTIVO CHECK (ACTIVO IN (0, 1)),
  CONSTRAINT CK_USUARIO_INTENTOS CHECK (INTENTOS_FALLIDOS >= 0)
);

-- Qué negocios ve cada persona y con qué rol (muchos a muchos con PERFIL)
CREATE TABLE NEGOCIO_USUARIO (
  USUARIO_ID  VARCHAR2(30 CHAR) NOT NULL,
  PERFIL_ID   VARCHAR2(20 CHAR) NOT NULL,
  ROL         VARCHAR2(20 CHAR) NOT NULL,
  CONSTRAINT PK_NEGOCIO_USUARIO PRIMARY KEY (USUARIO_ID, PERFIL_ID),
  CONSTRAINT FK_NEGOCIO_USUARIO_USUARIO FOREIGN KEY (USUARIO_ID)
    REFERENCES USUARIO (ID) ON DELETE CASCADE,
  CONSTRAINT FK_NEGOCIO_USUARIO_PERFIL FOREIGN KEY (PERFIL_ID)
    REFERENCES PERFIL (ID) ON DELETE CASCADE,
  CONSTRAINT CK_NEGOCIO_USUARIO_ROL CHECK (ROL IN ('DUENO', 'ADMINISTRADOR', 'CAJERO'))
);
CREATE INDEX IX_NEGOCIO_USUARIO_PERFIL ON NEGOCIO_USUARIO (PERFIL_ID);

-- Quién vendió (el cajero solo anula sus propias ventas) y quién anuló.
-- Son texto y no llave foránea: es historial y se conserva tal cual.
ALTER TABLE VENTA ADD (
  USUARIO_ID      VARCHAR2(30 CHAR),
  VENDEDOR        VARCHAR2(100 CHAR),
  FECHA_REGISTRO  TIMESTAMP,
  ANULADA_POR     VARCHAR2(100 CHAR));
ALTER TABLE ENTRADA ADD (ANULADA_POR VARCHAR2(100 CHAR));
ALTER TABLE SALIDA ADD (ANULADA_POR VARCHAR2(100 CHAR));
