-- Regiao virou "pasta" do usuario: ganha dono, visibilidade e cidades (sub-tabela);
-- perde o bounding box. Dev, sem dados de prod (em prod, popular usuario_id antes
-- de exigir NOT NULL).
-- Guardado por to_regclass: em banco NOVO nada disso se aplica — o V8 ja cria
-- regioes e regiao_cidades no formato final.
DO $$
BEGIN
    IF to_regclass('regioes') IS NOT NULL THEN
        ALTER TABLE regioes ADD COLUMN IF NOT EXISTS usuario_id VARCHAR(255);
        ALTER TABLE regioes ADD COLUMN IF NOT EXISTS visibilidade VARCHAR(20);
        UPDATE regioes SET visibilidade = 'PRIVADA' WHERE visibilidade IS NULL;
        ALTER TABLE regioes ALTER COLUMN visibilidade SET DEFAULT 'PRIVADA';
        ALTER TABLE regioes ADD COLUMN IF NOT EXISTS criado_em TIMESTAMP;
        ALTER TABLE regioes ADD COLUMN IF NOT EXISTS atualizado_em TIMESTAMP;
        ALTER TABLE regioes DROP COLUMN IF EXISTS lat_min;
        ALTER TABLE regioes DROP COLUMN IF EXISTS lat_max;
        ALTER TABLE regioes DROP COLUMN IF EXISTS lng_min;
        ALTER TABLE regioes DROP COLUMN IF EXISTS lng_max;

        CREATE TABLE IF NOT EXISTS regiao_cidades (
            regiao_id VARCHAR(255) NOT NULL REFERENCES regioes(id) ON DELETE CASCADE,
            nome      VARCHAR(255) NOT NULL,
            latitude  DOUBLE PRECISION,
            longitude DOUBLE PRECISION,
            altitude  DOUBLE PRECISION
        );
    END IF;
END $$;
