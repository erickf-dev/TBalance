-- =====================================================================
-- TBalance - Oracle 18c Express Edition
-- Cambio v6: abonos de los clientes que compraron fiado.
--
-- Solo agrega una tabla: no cambia ni renombra nada de lo anterior, así
-- que las consultas, vistas y procedimientos existentes siguen funcionando.
-- TBalance lo aplica solo al conectarse (una sola vez). Si prefiere hacerlo
-- a mano: F5 conectado como TBALANCE, después de v5.
-- =====================================================================

-- Lo que debe cada cliente se calcula: ventas fiadas vigentes (VENTA con
-- METODO_PAGO = 'Fiado') menos sus abonos vigentes. CLIENTE_DOCUMENTO es la
-- cédula de la venta fiada.
CREATE TABLE ABONO (
  PERFIL_ID          VARCHAR2(20 CHAR)   NOT NULL,
  ID                 VARCHAR2(30 CHAR)   NOT NULL,
  CLIENTE_DOCUMENTO  VARCHAR2(160 CHAR)  NOT NULL,
  CLIENTE            VARCHAR2(150 CHAR),
  MONTO              NUMBER(14,2) NOT NULL,
  METODO_PAGO        VARCHAR2(40 CHAR)   NOT NULL,
  REFERENCIA_PAGO    VARCHAR2(60 CHAR),
  NOTA               VARCHAR2(300 CHAR),
  FECHA              TIMESTAMP     NOT NULL,
  USUARIO_ID         VARCHAR2(30 CHAR),
  REGISTRADO_POR     VARCHAR2(100 CHAR),
  ESTADO             VARCHAR2(20 CHAR)   DEFAULT 'COMPLETADA' NOT NULL,
  FECHA_ANULACION    TIMESTAMP,
  MOTIVO_ANULACION   VARCHAR2(300 CHAR),
  ANULADA_POR        VARCHAR2(100 CHAR),
  CONSTRAINT PK_ABONO PRIMARY KEY (PERFIL_ID, ID),
  CONSTRAINT FK_ABONO_PERFIL FOREIGN KEY (PERFIL_ID)
    REFERENCES PERFIL (ID) ON DELETE CASCADE,
  CONSTRAINT CK_ABONO_MONTO CHECK (MONTO > 0),
  CONSTRAINT CK_ABONO_ESTADO CHECK (ESTADO IN ('COMPLETADA', 'ANULADA'))
);
CREATE INDEX IX_ABONO_CLIENTE ON ABONO (PERFIL_ID, CLIENTE_DOCUMENTO);
