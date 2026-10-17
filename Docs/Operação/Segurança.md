---
tags: [operacao, seguranca]
atualizado: 2026-07-25
---

# Segurança

Postura atual, o que já foi endurecido e o que continua aberto.

## Camadas

```
1. Rede      → só BFF e MQTT publicam porta (Deploy em Produção)
2. Borda     → rate limit por IP, CORS restrito
3. Token     → JWT RSA validado por JWKS em todo serviço
4. Autorização → visibilidade + anti-IDOR em cada handler
5. Dados     → senha com BCrypt, chave privada montada como segredo
```

## Hardening já feito

### Rodada de visibilidade (05/07/2026)
Leituras REST passaram a ser gateadas por visibilidade em todo o
[[Serviço APP]]; o [[BFF]] passou a cachear **por observador** (cache só por id
vazaria entre usuários); checagens de acesso **falham fechadas** no circuit breaker.

### Rodada de hardening do APP (09/07/2026)
Validação completa encontrou e corrigiu:
- `POST /aventura/{id}/participante` permitia que **qualquer autenticado** se
  adicionasse a uma aventura `SO_GRUPO` alheia e ganhasse leitura — furava todo o
  resto;
- pontos de interesse vazavam nome, descrição e coordenadas de aventura privada; o
  erro de distância do `addEvidence` permitia **trilaterar** um ponto privado;
- `respond` de amizade aceitava `BLOQUEADA`, criando bloqueio sem `blockedBy` —
  indesbloqueável;
- `block` passou a apagar os follows dos dois lados.

### Rodada 403/IDOR (15/07/2026)
`ForbiddenException` (HTTP 403) nos cinco serviços. IDOR fechado em `/usuario`
(`ensureSelf` compara o `sub` do token com o id da rota, **antes** de qualquer
consulta — não revela se o id alvo existe). O `dev-login` do BFF foi isolado em
`@Profile("dev")`.

### Anti-DDoS (23/07/2026)
Rate limit por IP no BFF (teto menor para autenticação), lockout de login por conta
no Cadastro e a rede segmentada do compose.

### Senha com BCrypt (10/07/2026)
Revoga a decisão antiga de senha em texto plano. O `UserMapper` recebe a senha **já
encodada** (o mapper é `@UtilityClass` e não injeta bean).

## O que continua aberto

> [!danger] Sem TLS
> O [[BFF]] fica exposto em **HTTP puro** na 8090. Token e senha trafegam em claro.
> É o item mais urgente antes de qualquer usuário real — um reverse proxy com TLS
> resolve. Ver [[Deploy em Produção]].

> [!warning] Onboarding público
> Cadastro, login e confirmação são `permitAll` por necessidade (o usuário ainda não
> tem token). O rate limit é a única proteção. Pendência antiga.

> [!note] Consultas sociais internas abertas
> `/amizade/sao-amigos` e `/seguidor/segue` são consultáveis por qualquer
> autenticado — são de uso interno do [[Serviço Localização]]. Aceito no MVP; um
> atacante autenticado pode mapear o grafo social.

> [!note] Visibilidade só vale no próximo SUBSCRIBE
> Quem já assinava um tópico que ficou mais restrito continua recebendo até
> reassinar. Ver [[Tempo Real]].

- Sem rotação de chave JWT
- Sem auditoria de acesso (só log com `traceId`)
- Sem verificação de conteúdo no upload (o [[Serviço Mídia]] aceita qualquer
  binário de até 50 MB de um usuário autenticado)
- Sem 2FA
- Pontos GPS ficam órfãos no loc quando o usuário sai da aventura descartando dados

## Auditoria de bbox e metadados (22/07/2026)

Corrigidos: vazamento por bbox de GPS, exposição de metadados de sessão e corridas
em `count() + 1` (substituídas por sequence — ver
[[Banco de Dados e Migrations]]).

Ver [[Visibilidade e Controle de Acesso]] para o modelo de permissão em si.
