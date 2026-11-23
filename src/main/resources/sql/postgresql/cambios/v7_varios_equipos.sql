-- =====================================================================
-- TBalance - PostgreSQL
-- Cambio v7: varios equipos sobre la misma base (por ejemplo, en la nube).
--
-- Solo agrega una tabla con un contador. Cada vez que un equipo guarda algo,
-- el contador sube; los demás equipos lo consultan cada pocos segundos y, si
-- cambió, vuelven a leer los datos. Así el dueño ve en su computador lo que
-- vende el cajero en el suyo. No cambia nada de lo anterior.
-- TBalance lo aplica solo al conectarse (una sola vez), después de v6.
-- =====================================================================

CREATE TABLE CAMBIO_VERSION (
  CLAVE    VARCHAR(20) NOT NULL,
  VERSION  BIGINT DEFAULT 0 NOT NULL,
  CONSTRAINT PK_CAMBIO_VERSION PRIMARY KEY (CLAVE)
);
INSERT INTO CAMBIO_VERSION (CLAVE, VERSION) VALUES ('TODO', 0);
