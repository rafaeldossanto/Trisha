---
tags: [servico, backend, geo]
porta: 8082
banco: trilha_localizacao
repo: loc
atualizado: 2026-07-25
---

# Serviço Localização

**Porta 8082 · banco `trilha_localizacao` (com PostGIS) · pacote `com.trisha.Loc.loc`**

Telemetria GPS: sessões de rastreamento, pontos, acompanhamento ao vivo e descoberta
de trilhas por área. É o serviço com o perfil de carga mais diferente — escrita
intensa e consulta geoespacial.

## Endpoints (`/localizacao`)

### Sessão
| Método | Rota | Observação |
|---|---|---|
| POST | `/sessao` | inicia o rastreamento de um caminho |
| PATCH | `/sessao/{id}/finalizar` | só o dono |
| PATCH | `/sessao/{id}/cancelar` | só o dono |
| PATCH | `/sessao/{id}/visibilidade` | altera **durante** a sessão ativa |
| GET | `/sessao/{id}` | gateado por visibilidade |
| GET | `/sessao/{id}/acesso` | uso interno do [[BFF]] |
| GET | `/sessao/caminho/{caminhoId}` | |
| GET | `/sessao/{sessaoId}/progresso` | |

### Pontos
| Método | Rota | Observação |
|---|---|---|
| POST | `/ponto` | grava **e** publica no Redis (ver [[Tempo Real]]) |
| GET | `/pontos/sessao/{sessaoId}` | |
| GET | `/pontos/caminho/{caminhoId}` | gateado via `/caminho/{id}/acesso` do APP |
| GET | `/pontos/bbox` | descoberta por área — PostGIS |
| GET | `/sessoes/ao-vivo` | sessões visíveis para o observador |

## Entidades

- **`TrackingSession`** — `pathId`, `userId`, status, **visibilidade** (padrão
  `PRIVADO`), término automático (raio configurável, padrão 5m), distância total
- **`GpsPoint`** — lat/lng/altitude, ordem, timestamp, ligado à sessão

## PostGIS e a consulta por bbox

O `GET /pontos/bbox` é **query nativa** com índice **GiST funcional**
(`idx_ponto_gps_geo`). O caminho até aqui:

1. começou como `BETWEEN` em lat/lng com índice composto;
2. virou PostGIS com índice geoespacial na migration `V4`.

A resposta é **decimada**: agrupa por caminho, reduz a `limitePorCaminho` (padrão
200) preservando primeiro e último ponto, com teto de **50 caminhos** por consulta.
Sem isso, um zoom-out no mapa traria centenas de milhares de pontos. Ver
[[Escalabilidade]].

## Dependência do domínio social

O loc **não** guarda amizade. Ele pergunta ao [[Serviço APP]]
(`AppFriendshipClient`), propagando o Bearer do usuário:

- `sao-amigos`, `segue` — checagem individual
- `amigos-ids`, `seguindo-ids` — **em lote**, usadas ao filtrar muitas sessões
  ao vivo de uma vez

## Ingestão MQTT

O app publica em `localizacao/ponto` no Mosquitto; o `LocationMqttHandler` consome.
Em produção o broker **exige autenticação** — `MQTT_USERNAME`/`MQTT_PASSWORD`.
Vazio significa conexão anônima (dev). Detalhes em [[Tempo Real]].

## Migrations

Único serviço que já tinha schema base versionado (`V4__schema_base_e_indice_bbox`)
antes da rodada de julho/2026. As migrations anteriores são guardadas por
`to_regclass` para não estourarem em banco novo. Ver [[Banco de Dados e Migrations]].

> [!note] Dependência do Flyway no Boot 4
> O Boot 4 modularizou o Flyway: sem o starter `spring-boot-flyway`, o
> `flyway-core` sozinho **não roda migration nenhuma**. Este serviço foi o primeiro
> a esbarrar nisso.

## Dependências

Postgres + PostGIS · Redis (Pub/Sub) · Mosquitto (MQTT) · [[Serviço APP]] (HTTP)
