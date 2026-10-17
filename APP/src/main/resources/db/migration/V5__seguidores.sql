CREATE TABLE IF NOT EXISTS seguidores (
    id          VARCHAR(255) PRIMARY KEY,
    seguidor_id VARCHAR(255) NOT NULL,
    seguido_id  VARCHAR(255) NOT NULL,
    criado_em   TIMESTAMP,
    CONSTRAINT uk_seguidor UNIQUE (seguidor_id, seguido_id)
);
CREATE INDEX IF NOT EXISTS idx_seguidor_seguidor ON seguidores (seguidor_id);
CREATE INDEX IF NOT EXISTS idx_seguidor_seguido ON seguidores (seguido_id);
