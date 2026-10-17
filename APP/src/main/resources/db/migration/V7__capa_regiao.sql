-- Foto de capa da regiao (colecao de aventuras): URL do binario ja armazenado
-- no servico de midia. Opcional — sem capa a UI mostra o icone padrao.
-- Guardado por to_regclass: em banco novo o V8 ja cria a coluna no schema.
DO $$
BEGIN
    IF to_regclass('regioes') IS NOT NULL THEN
        ALTER TABLE regioes ADD COLUMN IF NOT EXISTS capa_url VARCHAR(1024);
    END IF;
END $$;
