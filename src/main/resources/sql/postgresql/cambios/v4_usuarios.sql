-- =====================================================================
-- TBalance - PostgreSQL
-- Cambio v4: personas que usan TBalance, sus negocios y su rol, y quién
-- vende y quién anula.
--
-- TBalance lo aplica solo al conectarse (una sola vez). Si prefiere hacerlo
-- a mano: psql -U tbalance -d tbalance -f v4_usuarios.sql, después de v3.
-- =====================================================================

-- La contraseña nunca se guarda: CLAVE tiene su resumen PBKDF2 con sal
CREATE TABLE USUARIO (
  ID                 VARCHAR(30)  NOT NULL,
  NOMBRE             VARCHAR(100) NOT NULL,
  USUARIO            VARCHAR(30)  NOT NULL,
  CLAVE              VARCHAR(200) NOT NULL,
  ACTIVO             SMALLINT DEFAULT 1 NOT NULL,
  INTENTOS_FALLIDOS  SMALLINT DEFAULT 0 NOT NULL,
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
  USUARIO_ID  VARCHAR(30) NOT NULL,
  PERFIL_ID   VARCHAR(20) NOT NULL,
  ROL         VARCHAR(20) NOT NULL,
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
ALTER TABLE VENTA
  ADD COLUMN USUARIO_ID      VARCHAR(30),
  ADD COLUMN VENDEDOR        VARCHAR(100),
  ADD COLUMN FECHA_REGISTRO  TIMESTAMP,
  ADD COLUMN ANULADA_POR     VARCHAR(100);
ALTER TABLE ENTRADA ADD COLUMN ANULADA_POR VARCHAR(100);
ALTER TABLE SALIDA ADD COLUMN ANULADA_POR VARCHAR(100);
