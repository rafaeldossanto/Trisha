-- Schema base versionado do servico de Midia (mesmo papel do V4 do loc): ate
-- aqui a tabela so nascia pelo ddl-auto=update do Hibernate, e um banco NOVO
-- em prod (ddl-auto=validate) nao subia. IF NOT EXISTS mantem compatibilidade
-- com bancos que o Hibernate ja criou.
CREATE TABLE IF NOT EXISTS arquivo_midia (
    id              VARCHAR(255) PRIMARY KEY,
    nome_original   VARCHAR(255) NOT NULL,
    nome_armazenado VARCHAR(255) NOT NULL,
    tipo            VARCHAR(255) NOT NULL,
    content_type    VARCHAR(255) NOT NULL,
    tamanho_bytes   BIGINT NOT NULL,
    bucket          VARCHAR(255) NOT NULL,
    url             VARCHAR(255) NOT NULL,
    proprietario_id VARCHAR(255),
    criado_em       TIMESTAMP NOT NULL,
    trace_id        VARCHAR(255)
);
