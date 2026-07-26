-- Guardado por to_regclass: em banco NOVO o Flyway roda antes do Hibernate
-- criar qualquer tabela, entao os ALTERs so se aplicam em bancos antigos
-- (criados pelo ddl-auto); em banco novo o V8 ja cria as colunas no schema.
DO $$
BEGIN
    IF to_regclass('aventuras') IS NOT NULL THEN
        ALTER TABLE aventuras ADD COLUMN IF NOT EXISTS trace_id VARCHAR(16);
    END IF;
    IF to_regclass('caminhos') IS NOT NULL THEN
        ALTER TABLE caminhos ADD COLUMN IF NOT EXISTS trace_id VARCHAR(16);
    END IF;
    IF to_regclass('pontos_interesse') IS NOT NULL THEN
        ALTER TABLE pontos_interesse ADD COLUMN IF NOT EXISTS trace_id VARCHAR(16);
    END IF;
END $$;
