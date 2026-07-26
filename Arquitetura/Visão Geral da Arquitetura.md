---
tags: [arquitetura]
atualizado: 2026-07-25
---

# Visão Geral da Arquitetura

Cinco microserviços Spring Boot atrás de um **Backend for Frontend**. Cada serviço
tem repositório, banco e ciclo de vida próprios; o front nunca fala com eles
diretamente — só com o [[BFF]].

```
                    ┌──────────────┐
   App Mobile ─────►│              │
                    │     BFF      │  :8090  ← única porta pública
   App Web    ─────►│              │
                    └──────┬───────┘
                           │ HTTP interno (Bearer propagado)
        ┌──────────────┬───┴────────┬──────────────┐
        ▼              ▼            ▼              ▼
    Cadastro         APP       Localização       Mídia
      :8080         :8081         :8082          :8083
        │              │            │              │
    Postgres      Postgres      Postgres       Postgres
    + Zeebe                     + PostGIS      + MinIO
    + Redis                     + Redis
                                + Mosquitto
```

Fronts: [[App Mobile]] · [[App Web]] — Serviços: [[Serviço Cadastro]] ·
[[Serviço APP]] · [[Serviço Localização]] · [[Serviço Mídia]] · [[BFF]]

## Por que cinco serviços

A divisão segue **fronteiras de responsabilidade**, não tamanho:

- **Identidade** é o único que guarda senha e assina token → isolado no
  [[Serviço Cadastro]].
- **Domínio social** (aventura, caminho, amizade, seguidor, região) é o coração do
  produto e muda toda semana → [[Serviço APP]].
- **Telemetria GPS** tem carga e padrão de acesso completamente diferentes (escrita
  intensa, consulta geoespacial) → [[Serviço Localização]], com PostGIS.
- **Binários** não pertencem a banco relacional → [[Serviço Mídia]] com MinIO.
- **Composição para tela** não é domínio → [[BFF]].

## O papel do BFF

O [[BFF]] não é um proxy burro. Ele:

1. **É a única superfície pública.** Em produção só ele publica porta (ver
   [[Deploy em Produção]]). Isso torna o rate limit incontornável — não dá para
   bater direto no serviço interno.
2. **Agrega.** Uma tela costuma precisar de dois ou três serviços; o BFF junta e
   devolve um DTO só. Exemplo: trilhas descobertas no mapa vêm do
   [[Serviço Localização]] (geometria) + [[Serviço APP]] (visibilidade e autor).
3. **Protege.** Rate limit por IP, circuit breaker e cache por observador.

## O que atravessa todos

- [[Autenticação e Identidade]]: todos são *resource server*; só o Cadastro emite.
- [[Visibilidade e Controle de Acesso]]: nenhum dado social vaza sem checagem.
- **traceId**: gerado na borda e propagado no header `X-Trace-Id`, aparece no log de
  todos (`%5p [trace:%X{traceId:-}]`) e é gravado nas entidades principais.

## Stack

- **Java 21**, **Spring Boot 4**, Gradle
- **PostgreSQL 18** (um banco por serviço, mesma instância — ver
  [[Decisões Deliberadas]])
- **Redis** (cache do BFF, lockout de login, Pub/Sub do tempo real)
- **MinIO** (S3-compatível, binários)
- **Mosquitto** (MQTT, ingestão GPS)
- **Camunda 8 / Zeebe** (orquestra o cadastro — ver [[Serviço Cadastro]])
- **Flutter** nos dois fronts

Detalhes de infraestrutura em [[Ambientes]]; limites de crescimento em
[[Escalabilidade]].
