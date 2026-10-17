---
tags: [servico, backend, borda]
porta: 8090
banco: nenhum
repo: Trisha
pasta: bff
atualizado: 2026-10-17
---

# BFF

**Porta 8090 · sem banco próprio · pacote `com.trisha.bff`**

*Backend for Frontend*: a única superfície pública do sistema. Agrega respostas de
vários serviços, protege a borda e adapta os contratos ao que a tela precisa.

> [!note] Repositório antigo: `bfff`, depois `TrishaBff`
> Até o monorepo (17/10/2026) o BFF tinha repositório próprio, criado como `bfff`
> (três `f`, erro de digitação) e renomeado para `TrishaBff`. Existe um
> `rafaeldossanto/BFF` de outro projeto, que não tem relação com este.

## Prefixos

Todas as rotas vivem sob `/bff`:

| Prefixo | Downstream |
|---|---|
| `/bff/auth` | [[Serviço Cadastro]] |
| `/bff/usuarios` | [[Serviço Cadastro]] + [[Serviço APP]] |
| `/bff/aventuras` | [[Serviço APP]] |
| `/bff/caminhos` | [[Serviço APP]] + [[Serviço Localização]] |
| `/bff/pontos-interesse` | [[Serviço APP]] |
| `/bff/regioes` | [[Serviço APP]] |
| `/bff/amizades`, `/bff/seguidores` | [[Serviço APP]] |
| `/bff/localizacao` | [[Serviço Localização]] |
| `/bff/midias` | [[Serviço Mídia]] |

## Proteções de borda

### Rate limit por IP
Janela fixa no Redis, com teto menor para autenticação:

| Configuração | Padrão |
|---|---|
| `RATE_LIMIT_DEFAULT` | 600 / janela |
| `RATE_LIMIT_AUTH` | 30 / janela |
| `RATE_LIMIT_WINDOW_SECONDS` | 60 |

> [!warning] `forward-headers-strategy` e IP real
> Fica em `none` enquanto o BFF estiver exposto direto — senão o cliente forja
> `X-Forwarded-For` e escapa da trava. **Só** troque para `framework` quando houver
> reverse proxy na frente. Ver [[Deploy em Produção]].

### CORS
Origens vêm de `cors.allowed-origins` (`CORS_ALLOWED_ORIGINS`), com default
`localhost` para dev. Sem isso o [[App Web]] é bloqueado pelo browser.

### Resiliência e cache
Circuit breaker, retry e cache Redis por observador — detalhes em
[[Comunicação entre Serviços]].

## Agregações que valem conhecer

**`GET /bff/caminhos/descobrir`** — junta a geometria do [[Serviço Localização]]
(bbox) com a visibilidade e o autor do [[Serviço APP]]. **Sem cache de propósito**:
a área muda a cada gesto de mapa.

**`GET /bff/aventuras/feed`** — feed real (minhas + visíveis de quem sigo), com o
autor resolvido via `/usuario/resumo`.

**`GET /bff/localizacao/sessoes-ao-vivo`** — sessões em andamento visíveis para o
observador. Sem cache.

## Endpoints públicos (sem token)

Cadastro, logins, aceite de termos, confirmação e reenvio de e-mail — o usuário
`PENDENTE` ainda não tem token. Mais swagger e `/actuator/health`.

O `dev-login` do BFF é isolado em `@Profile("dev")`.

## Dependências

Redis (rate limit + cache) · todos os quatro serviços de domínio
