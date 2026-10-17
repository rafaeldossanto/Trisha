---
tags: [arquitetura, tempo-real]
atualizado: 2026-07-25
---

# Tempo Real

Três tecnologias, cada uma com um papel distinto. Todas vivem no
[[Serviço Localização]].

## 1. MQTT — ingestão resiliente

O app publica pontos GPS no **Mosquitto**. A escolha é sobre o ambiente: em trilha o
sinal cai, e o cliente MQTT **bufferiza offline** e despeja ao reconectar. QoS 1
garante entrega.

- Tópico: `localizacao/ponto`
- O `LocationMqttHandler` consome e grava
- **Produção exige autenticação** (`allow_anonymous false`); o loc autentica com
  `MQTT_USERNAME`/`MQTT_PASSWORD`. Em dev, anônimo.

## 2. WebSocket/STOMP — quem assiste ao vivo

- Endpoint: `/ws-localizacao` (com fallback SockJS)
- O app **assina** `/topic/sessao/{id}` para receber posições
- Publica em `/app/sessao/ponto`

Dois interceptors no canal de entrada:

| Interceptor | Momento | O que faz |
|---|---|---|
| `ConnectAuthenticationInterceptor` | CONNECT | valida o Bearer |
| `SubscribeAuthorizationInterceptor` | SUBSCRIBE | aplica [[Visibilidade e Controle de Acesso]] |

> [!note] Mudança de visibilidade no meio da trilha
> Quem **já** assinava um tópico que ficou mais restrito continua recebendo até o
> próximo SUBSCRIBE. Aceito no MVP.

## 3. Redis Pub/Sub — fan-out entre instâncias

O broker STOMP do Spring é **simples e local a cada instância**. Com duas réplicas,
quem está conectado na réplica A não receberia o ponto que chegou na réplica B. O
Redis Pub/Sub resolve: quem recebe publica no Redis, e todas as instâncias
repassam aos seus assinantes.

> [!bug] Bug histórico que vale conhecer
> Por um tempo **só o MQTT** publicava no Redis. Quem rastreava pelo app (fluxo
> HTTP `POST /localizacao/ponto`) nunca aparecia ao vivo. O `LocationEventPublisher`
> passou a ser chamado também no caminho HTTP. Ver [[Linha do Tempo]].

## Descoberta de trilhas por área (bbox)

Não é tempo real, mas é o outro consumo pesado de GPS. O mapa da home busca **só a
área visível**:

- `GET /localizacao/pontos/bbox` — consulta nativa **PostGIS** com índice GiST
- Agrupa por caminho e **decima** os pontos (padrão 200 por caminho), preservando
  sempre o primeiro e o último
- Teto de 50 caminhos por consulta (`MAX_PATHS_PER_BBOX`)
- No front: debounce de 700ms e zoom mínimo 10

Os detalhes de custo estão em [[Escalabilidade]].
