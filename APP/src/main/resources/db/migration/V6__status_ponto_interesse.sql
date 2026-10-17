-- Marcacao pessoal do usuario sobre um ponto de interesse: status de progressao
-- (NO_RADAR/NA_MIRA/CONQUISTADO) e a flag de objetivo, independente do status.
-- A linha so existe enquanto ha alguma marcacao (zerou, o service remove).
-- Em dev o Hibernate (ddl-auto) cria o schema; esta migration garante o mesmo
-- em prod (validate).
CREATE TABLE IF NOT EXISTS status_ponto_interesse (
    id            VARCHAR(255) PRIMARY KEY,
    ponto_id      VARCHAR(255) NOT NULL,
    usuario_id    VARCHAR(255) NOT NULL,
    status        VARCHAR(20),
    objetivo      BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em     TIMESTAMP,
    atualizado_em TIMESTAMP,
    CONSTRAINT uk_status_ponto_usuario UNIQUE (ponto_id, usuario_id)
);

CREATE INDEX IF NOT EXISTS idx_status_ponto_usuario ON status_ponto_interesse (usuario_id);
CREATE INDEX IF NOT EXISTS idx_status_ponto_ponto ON status_ponto_interesse (ponto_id);
