---
tags: [infra, banco]
atualizado: 2026-10-17
---

# Banco de Dados e Migrations

Três bancos na mesma instância PostgreSQL 18. O [[BFF]] não tem banco.

| Serviço | Banco | Extensão |
|---|---|---|
| [[Serviço Cadastro]] | `trisha` (compartilhado) | — |
| [[Serviço APP]] | `trisha` (compartilhado) | — |
| [[Serviço Localização]] | `trilha_localizacao` | **PostGIS** |
| [[Serviço Mídia]] | `trilha_midia` | — |

> [!important] Cadastro e APP dividem o `trisha` desde 29/07/2026
> O Cadastro é o dono da tabela `usuario`; o APP a lê como entidade `@Immutable`.
> Com bancos separados, a cópia do APP nascia vazia e nada a sincronizava. Para as
> migrations não colidirem, o APP grava o histórico em
> `flyway_schema_history_app`, e a tabela `usuario` tem definição idêntica nos dois
> schemas base, então a ordem em que os serviços sobem num banco vazio não importa.
> Ver [[Decisões Deliberadas]].

> [!note] Mesma instância é deliberado
> Instâncias separadas viriam depois, se a carga pedir. Ver [[Escalabilidade]].

## Como o schema nasce

Depende do ambiente — e essa diferença já causou um bloqueador de produção:

| Ambiente | `ddl-auto` | Quem cria as tabelas |
|---|---|---|
| dev | `update` | Hibernate, a partir das entidades |
| prod | `validate` | **Flyway, exclusivamente** |

> [!danger] O bloqueador de julho/2026
> Até 25/07/2026, só o [[Serviço Localização]] tinha migration criando o schema. Nos
> demais, a primeira migration era um `ALTER TABLE` numa tabela que **nenhuma
> migration criava** — em dev o Hibernate criava, e ninguém percebia. Num banco novo
> de produção com `validate`, o Flyway estouraria no primeiro `ALTER` e o serviço
> não subiria.

### A correção

Cada serviço ganhou uma **migration de schema base**:

| Serviço | Migration |
|---|---|
| [[Serviço APP]] | `V8__schema_base.sql` |
| [[Serviço Cadastro]] | `V4__schema_base.sql` |
| [[Serviço Mídia]] | `V3__schema_base.sql` |
| [[Serviço Localização]] | `V4__schema_base_e_indice_bbox.sql` (já existia) |

E as migrations **anteriores** foram guardadas para virar no-op em banco vazio:

```sql
DO $$
BEGIN
    IF to_regclass('aventuras') IS NOT NULL THEN
        ALTER TABLE aventuras ADD COLUMN IF NOT EXISTS trace_id VARCHAR(16);
    END IF;
END $$;
```

Assim a mesma sequência funciona nos dois casos: banco antigo (criado pelo
Hibernate) aplica os ALTERs; banco novo pula os ALTERs e nasce pronto pelo schema
base.

## Armadilhas conhecidas

> [!warning] O Boot 4 modularizou o Flyway
> `flyway-core` sozinho **não roda migration nenhuma**. É preciso também o starter
> `org.springframework.boot:spring-boot-flyway`. O loc descobriu isso primeiro; os
> outros três só ganharam o starter em 25/07/2026.

> [!warning] Enum como ORDINAL no Cadastro
> `User.status` e `EmailConfirmation.status` não têm `@Enumerated(STRING)`, então o
> JPA grava o **número** do enum. O schema base usa `SMALLINT` nessas colunas.
> Reordenar os valores do enum corromperia os dados.

## Índices que importam

- `idx_ponto_gps_geo` — **GiST funcional** no `ponto_gps`, base da consulta por bbox
- `idx_aventura_usuario`, `idx_caminho_aventura`, `idx_caminho_usuario`
- `uk_usuario_codigo` — unicidade do `codigoUsuario`
- `uk_seguidor`, `uk_status_ponto_usuario` — evitam duplicata em relação N:N

## Sequences

`usuario_codigo_seq` gera o número do `codigoUsuario`. Substituiu um
`COUNT(*) + 1` que tinha condição de corrida — dois cadastros simultâneos geravam o
mesmo código.
