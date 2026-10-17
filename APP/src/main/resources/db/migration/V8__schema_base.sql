-- Schema base versionado do servico APP (mesmo papel do V4 do loc): ate aqui
-- as tabelas so nasciam pelo ddl-auto=update do Hibernate, e um banco NOVO em
-- prod (ddl-auto=validate) nao subia. IF NOT EXISTS mantem compatibilidade com
-- bancos que o Hibernate ja criou; as migrations anteriores viram no-op em
-- banco novo (guardadas por to_regclass) porque aqui ja nasce tudo atualizado.

-- Tabela 'usuario': o DONO e o servico Cadastro, que escreve nela. Aqui ela e
-- lida como view read-only (entidade User @Immutable, que mapeia so id, nome,
-- email e codigo_usuario) — o Hibernate valida a entidade mesmo read-only, entao
-- a tabela precisa existir quando o APP sobe.
--
-- A definicao abaixo e IDENTICA a do Cadastro (V4__schema_base.sql) DE PROPOSITO:
-- os dois servicos compartilham o banco 'trisha' e qualquer um pode subir
-- primeiro num banco vazio. Se o APP criasse aqui uma versao reduzida, o
-- ddl-auto=validate do Cadastro quebraria por falta de senha/status. Com as duas
-- definicoes iguais e o IF NOT EXISTS, a ordem de boot deixa de importar.
-- Colunas novas continuam sendo responsabilidade do Cadastro (via ALTER nas
-- migrations dele, que rodam depois).
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

CREATE TABLE IF NOT EXISTS regioes (
    id            VARCHAR(255) PRIMARY KEY,
    usuario_id    VARCHAR(255) NOT NULL,
    nome          VARCHAR(255) NOT NULL,
    descricao     VARCHAR(255),
    capa_url      VARCHAR(1024),
    visibilidade  VARCHAR(255) NOT NULL DEFAULT 'PRIVADA',
    criado_em     TIMESTAMP,
    atualizado_em TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_regiao_usuario ON regioes (usuario_id);

CREATE TABLE IF NOT EXISTS regiao_cidades (
    regiao_id VARCHAR(255) NOT NULL REFERENCES regioes (id) ON DELETE CASCADE,
    nome      VARCHAR(255) NOT NULL,
    latitude  DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    altitude  DOUBLE PRECISION
);

CREATE TABLE IF NOT EXISTS aventuras (
    id            VARCHAR(255) PRIMARY KEY,
    usuario_id    VARCHAR(255) NOT NULL,
    regiao_id     VARCHAR(255) REFERENCES regioes (id),
    destino       VARCHAR(255) NOT NULL,
    status        VARCHAR(255) NOT NULL,
    visibilidade  VARCHAR(255) NOT NULL,
    criado_em     TIMESTAMP,
    atualizado_em TIMESTAMP,
    trace_id      VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_aventura_usuario ON aventuras (usuario_id);

CREATE TABLE IF NOT EXISTS participantes_aventura (
    id          VARCHAR(255) PRIMARY KEY,
    aventura_id VARCHAR(255) NOT NULL REFERENCES aventuras (id),
    usuario_id  VARCHAR(255) NOT NULL,
    entrado_em  TIMESTAMP,
    CONSTRAINT uk_participante_aventura UNIQUE (aventura_id, usuario_id)
);

CREATE TABLE IF NOT EXISTS caminhos (
    id                 VARCHAR(255) PRIMARY KEY,
    aventura_id        VARCHAR(255) NOT NULL REFERENCES aventuras (id),
    usuario_id         VARCHAR(255) NOT NULL,
    cor                VARCHAR(255),
    numero             INTEGER,
    iniciado_em        TIMESTAMP,
    finalizado_em      TIMESTAMP,
    distancia_total_km DOUBLE PRECISION,
    trace_id           VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_caminho_aventura ON caminhos (aventura_id);
CREATE INDEX IF NOT EXISTS idx_caminho_usuario ON caminhos (usuario_id);

CREATE TABLE IF NOT EXISTS pontos_interesse (
    id         VARCHAR(255) PRIMARY KEY,
    caminho_id VARCHAR(255) NOT NULL REFERENCES caminhos (id),
    usuario_id VARCHAR(255) NOT NULL,
    tipo       VARCHAR(255) NOT NULL,
    nome       VARCHAR(255),
    descricao  VARCHAR(255),
    latitude   DOUBLE PRECISION NOT NULL,
    longitude  DOUBLE PRECISION NOT NULL,
    criado_em  TIMESTAMP,
    trace_id   VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_ponto_caminho ON pontos_interesse (caminho_id);

CREATE TABLE IF NOT EXISTS evidencias (
    id                  VARCHAR(255) PRIMARY KEY,
    ponto_id            VARCHAR(255) NOT NULL REFERENCES pontos_interesse (id),
    usuario_id          VARCHAR(255) NOT NULL,
    foto_url            VARCHAR(255) NOT NULL,
    tipo_evidencia      VARCHAR(255),
    lat_captura         DOUBLE PRECISION,
    lng_captura         DOUBLE PRECISION,
    distancia_doponto_m DOUBLE PRECISION,
    capturada_no_app    BOOLEAN NOT NULL DEFAULT FALSE,
    validada            BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em           TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_evidencia_ponto ON evidencias (ponto_id);

CREATE TABLE IF NOT EXISTS midias (
    id                      VARCHAR(255) PRIMARY KEY,
    aventura_id             VARCHAR(255) NOT NULL REFERENCES aventuras (id),
    caminho_id              VARCHAR(255) REFERENCES caminhos (id),
    usuario_id              VARCHAR(255) NOT NULL,
    tipo                    VARCHAR(255) NOT NULL,
    url                     VARCHAR(255) NOT NULL,
    lat_captura             DOUBLE PRECISION,
    lng_captura             DOUBLE PRECISION,
    distancia_na_captura_km DOUBLE PRECISION,
    percentual_no_caminho   DOUBLE PRECISION,
    capturada_em            TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_midia_aventura ON midias (aventura_id);
CREATE INDEX IF NOT EXISTS idx_midia_caminho ON midias (caminho_id);

CREATE TABLE IF NOT EXISTS amizades (
    id              VARCHAR(255) PRIMARY KEY,
    solicitante_id  VARCHAR(255) NOT NULL,
    receptor_id     VARCHAR(255) NOT NULL,
    status          VARCHAR(255) NOT NULL,
    solicitado_em   TIMESTAMP,
    respondido_em   TIMESTAMP,
    bloqueado_por   VARCHAR(255),
    CONSTRAINT uk_amizade_par UNIQUE (solicitante_id, receptor_id)
);

CREATE INDEX IF NOT EXISTS idx_amizade_solicitante ON amizades (solicitante_id);
CREATE INDEX IF NOT EXISTS idx_amizade_receptor ON amizades (receptor_id);

-- seguidores e status_ponto_interesse ja nascem nos V5/V6 (CREATE IF NOT
-- EXISTS, seguros em banco vazio) — nao precisam ser repetidos aqui.
