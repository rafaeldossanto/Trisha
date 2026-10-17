-- Schema base versionado do Cadastro (mesmo papel do V4 do loc): ate aqui as
-- tabelas so nasciam pelo ddl-auto=update do Hibernate, e um banco NOVO em
-- prod (ddl-auto=validate) nao subia. IF NOT EXISTS mantem compatibilidade com
-- bancos que o Hibernate ja criou.
--
-- Atencao aos dois `status` SMALLINT: User.status e EmailConfirmation.status
-- nao tem @Enumerated(STRING), entao o JPA persiste o ORDINAL do enum (numero),
-- nao o nome. O schema espelha o que o Hibernate espera no validate.

CREATE TABLE IF NOT EXISTS usuario (
    id               VARCHAR(255) PRIMARY KEY,
    nome             VARCHAR(255),
    email            VARCHAR(255),
    senha            VARCHAR(255),
    codigo_usuario   VARCHAR(255),
    status           SMALLINT,
    data_criacao     TIMESTAMP,
    data_atualizacao TIMESTAMP,
    trace_id         VARCHAR(255),
    CONSTRAINT uk_usuario_codigo UNIQUE (codigo_usuario)
);

CREATE INDEX IF NOT EXISTS idx_usuario_email ON usuario (email);

CREATE TABLE IF NOT EXISTS email_confirmation (
    id               VARCHAR(255) PRIMARY KEY,
    usuario_id       VARCHAR(255) REFERENCES usuario (id),
    token            VARCHAR(255),
    status           SMALLINT,
    expira_em        TIMESTAMP,
    enviado_em       TIMESTAMP,
    data_confirmacao TIMESTAMP
);

CREATE TABLE IF NOT EXISTS terms_acceptance (
    id          VARCHAR(255) PRIMARY KEY,
    usuario_id  VARCHAR(255) REFERENCES usuario (id),
    aceito      BOOLEAN,
    versao      VARCHAR(255),
    data_aceite TIMESTAMP
);

CREATE TABLE IF NOT EXISTS conta_vinculada (
    id                  VARCHAR(255) PRIMARY KEY,
    usuario_id          VARCHAR(255) NOT NULL REFERENCES usuario (id),
    provedor            VARCHAR(255) NOT NULL,
    provedor_usuario_id VARCHAR(255),
    email               VARCHAR(255),
    vinculado_em        TIMESTAMP NOT NULL,
    CONSTRAINT uk_conta_vinculada_provedor UNIQUE (provedor, provedor_usuario_id)
);

CREATE INDEX IF NOT EXISTS idx_conta_vinculada_usuario ON conta_vinculada (usuario_id);
