-- =====================================================================
-- TBalance - PostgreSQL
-- Cambio v6: abonos de los clientes que compraron fiado.
--
-- Solo agrega una tabla: no cambia ni renombra nada de lo anterior, así
-- que las consultas, vistas y funciones existentes siguen funcionando.
-- TBalance lo aplica solo al conectarse (una sola vez). Si prefiere hacerlo
-- a mano: psql -U tbalance -d tbalance -f v6_abonos.sql, después de v5.
-- =====================================================================

-- Lo que debe cada cliente se calcula: ventas fiadas vigentes (VENTA con
-- METODO_PAGO = 'Fiado') menos sus abonos vigentes. CLIENTE_DOCUMENTO es la
-- cédula de la venta fiada.
CREATE TABLE ABONO (
  PERFIL_ID          VARCHAR(20)   NOT NULL,
  ID                 VARCHAR(30)   NOT NULL,
  CLIENTE_DOCUMENTO  VARCHAR(160)  NOT NULL,
  CLIENTE            VARCHAR(150),
  MONTO              NUMERIC(14,2) NOT NULL,
  METODO_PAGO        VARCHAR(40)   NOT NULL,
  REFERENCIA_PAGO    VARCHAR(60),
  NOTA               VARCHAR(300),
  FECHA              TIMESTAMP     NOT NULL,
  USUARIO_ID         VARCHAR(30),
  REGISTRADO_POR     VARCHAR(100),
  ESTADO             VARCHAR(20)   DEFAULT 'COMPLETADA' NOT NULL,
  FECHA_ANULACION    TIMESTAMP,
  MOTIVO_ANULACION   VARCHAR(300),
  ANULADA_POR        VARCHAR(100),
  CONSTRAINT PK_ABONO PRIMARY KEY (PERFIL_ID, ID),
  CONSTRAINT FK_ABONO_PERFIL FOREIGN KEY (PERFIL_ID)
    REFERENCES PERFIL (ID) ON DELETE CASCADE,
  CONSTRAINT CK_ABONO_MONTO CHECK (MONTO > 0),
  CONSTRAINT CK_ABONO_ESTADO CHECK (ESTADO IN ('COMPLETADA', 'ANULADA'))
);
CREATE INDEX IX_ABONO_CLIENTE ON ABONO (PERFIL_ID, CLIENTE_DOCUMENTO);
