-- Proprietario do arquivo: quem fez o upload. Usado para validar a delecao
-- (so o dono pode apagar).
-- Guardado por to_regclass: em banco NOVO o Flyway roda antes do Hibernate
-- criar a tabela; em banco novo o V3 ja cria a coluna no schema.
DO $$
BEGIN
    IF to_regclass('arquivo_midia') IS NOT NULL THEN
        ALTER TABLE arquivo_midia ADD COLUMN IF NOT EXISTS proprietario_id VARCHAR(255);
    END IF;
END $$;
