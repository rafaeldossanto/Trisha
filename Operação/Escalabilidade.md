---
tags: [operacao, escalabilidade]
atualizado: 2026-07-25
---

# Escalabilidade

O sistema hoje roda em **uma instância de cada serviço** num compose. Esta nota
separa o que **já** está preparado para crescer do que **vai quebrar primeiro**.

## O que já escala

### Serviços stateless
Cadastro, APP, Mídia e BFF não guardam estado em memória. Sessão é JWT — não há
*sticky session*. Subir réplica é só aumentar o `replicas` e colocar um balanceador.

### Fan-out do tempo real já é distribuído
O broker STOMP do Spring é local a cada instância, mas o **Redis Pub/Sub** repassa
entre elas. Isso significa que o [[Serviço Localização]] **pode** ter réplicas sem
que um assinante deixe de receber pontos. Ver [[Tempo Real]].

### Consulta geoespacial com teto
O `GET /pontos/bbox` já tem três defesas contra explosão de dados:
- índice **GiST** (não varredura sequencial);
- **decimação** a ~200 pontos por caminho, preservando extremos;
- teto de **50 caminhos** por consulta.

E no front: debounce de 700ms e zoom mínimo 10 — o servidor não recebe uma consulta
por frame de animação.

### Consultas em lote onde importava
- `AdventureMetricsAssembler.buildPage` faz 2 queries em lote em vez de N+1;
- as checagens sociais do loc têm variante em lote (`amigos-ids`, `seguindo-ids`).

### Ingestão desacoplada
O MQTT absorve pico e desconexão: o cliente bufferiza offline e despeja ao
reconectar. O serviço consome no seu ritmo.

## O que quebra primeiro

### 1. Postgres único (o gargalo mais próximo)
Quatro bancos, **uma instância**. A escrita de `ponto_gps` é a mais intensa do
sistema e compete com o resto.

**Caminho**: separar a instância do [[Serviço Localização]] primeiro — é o que tem
padrão de carga mais distinto. Depois, réplica de leitura para as consultas de
mapa. Ver [[Banco de Dados e Migrations]].

### 2. Crescimento sem limite de `ponto_gps`
Nenhuma política de retenção ou particionamento. Uma trilha longa gera milhares de
pontos, e eles ficam para sempre.

**Caminho**: particionar por tempo (`PARTITION BY RANGE` na data), e considerar
guardar a geometria simplificada do caminho finalizado (Douglas-Peucker) em vez de
todos os pontos brutos.

### 3. Rate limit de janela fixa
A implementação atual permite o dobro do teto na virada da janela (final de uma +
começo da outra).

**Caminho**: janela deslizante ou *token bucket*. Ver [[BFF]].

### 4. MinIO single-node
Sem réplica; o volume é o ponto único de falha para todas as fotos.

**Caminho**: MinIO distribuído ou trocar por S3 gerenciado — a API é compatível,
a mudança é de configuração.

### 5. Zeebe single-broker
Um broker sem exporter. O histórico do processo vive só nele, e ele é dependência
**dura** do cadastro ([[Serviço Cadastro]]).

**Caminho**: cluster Zeebe, ou reavaliar se o Camunda ainda paga o próprio custo
para um fluxo de e-mail relativamente simples.

### 6. Mosquitto sem cluster
Broker único, exposto na internet.

**Caminho**: EMQX/VerneMQ em cluster, ou broker gerenciado.

### 7. Cache com TTL fixo de 60s
Não há invalidação por evento; a chave inclui o observador, o que **multiplica** as
entradas por usuário. Com muitos usuários ativos, o Redis cresce rápido.

**Caminho**: TTL diferenciado por recurso e invalidação por evento nas escritas.

## Ordem sugerida

1. **Retenção/particionamento de `ponto_gps`** — o crescimento é contínuo e silencioso
2. **Separar a instância de Postgres do loc**
3. **Reverse proxy com TLS** na frente do BFF (também requisito de segurança)
4. **Réplicas do BFF e do APP** atrás do proxy
5. **MinIO distribuído ou S3**
6. Refinar rate limit e cache

## Sinais para monitorar

Não há observabilidade além de log com `traceId` — nem métrica, nem tracing
distribuído. O `/actuator/health,info,metrics` está exposto no BFF, mas nada o
coleta.

**O primeiro passo real de escala é conseguir enxergar**: Prometheus + Grafana nas
métricas do Actuator, e um tracing distribuído amarrado ao `traceId` que já existe.
