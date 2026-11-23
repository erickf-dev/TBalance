-- =====================================================================
-- TBalance - PostgreSQL
-- 00. Crear el usuario y la base de datos de TBalance
--
-- Ejecutar UNA sola vez conectado como postgres (pgAdmin > Query Tool
-- sobre la base "postgres", o: psql -U postgres -f 00_crear_base.sql).
-- Cambie la clave 'tbalance' si lo desea; la misma clave se escribe
-- después en TBalance > Copias de seguridad > Base de datos.
-- =====================================================================

CREATE USER tbalance WITH PASSWORD 'tbalance';
CREATE DATABASE tbalance OWNER tbalance ENCODING 'UTF8';
