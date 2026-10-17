-- Guardado por to_regclass: em banco NOVO o Flyway roda antes do Hibernate
-- criar as tabelas; em banco novo o V4 ja cria a coluna no schema.
DO $$
BEGIN
    IF to_regclass('usuario') IS NOT NULL THEN
        ALTER TABLE usuario ADD COLUMN IF NOT EXISTS trace_id VARCHAR(16);
    END IF;
END $$;
