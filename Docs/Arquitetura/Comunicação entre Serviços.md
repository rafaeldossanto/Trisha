---
tags: [arquitetura]
atualizado: 2026-07-25
---

# Comunicação entre Serviços

Tudo é **HTTP síncrono** entre serviços, exceto o tempo real (ver [[Tempo Real]]).
Não há fila de mensagens entre domínios — o acoplamento é assumido e controlado.

## Quem chama quem

```
BFF ──► Cadastro   (login, usuário, confirmação)
BFF ──► APP        (aventura, caminho, social, região)
BFF ──► Localização(sessão, pontos GPS, bbox, ao vivo)
BFF ──► Mídia      (listagem de mídias)

Localização ──► APP  (visibilidade: sao-amigos, segue, acesso ao caminho)
```

O [[Serviço APP]] e o [[Serviço Cadastro]] **não chamam ninguém**. A dependência do
[[Serviço Localização]] no APP é a única entre serviços de domínio, e existe porque
localização precisa saber de amizade sem duplicar a regra.

## Resiliência (Resilience4j, no BFF)

Cada client downstream tem circuit breaker e retry:

- **Circuit breaker**: janela de 10 chamadas, abre com 50% de falha, espera 10s,
  3 chamadas de teste no meio-aberto.
- **Retry**: 2 tentativas, 200ms de espera, só para `IOException` e
  `ResourceAccessException` — nunca para erro de negócio.

**Fallback com semântica correta:** `ClientFallbacks.unavailable` propaga o status
real quando o downstream responde 4xx. Só falha de infraestrutura vira 503. Sem
isso, um "404 não encontrado" viraria "503 indisponível" e o front trataria errado.

> [!warning] Checagem de acesso falha fechada
> O fallback de circuit breaker para as chamadas de **visibilidade** nega o acesso.
> Ver [[Visibilidade e Controle de Acesso]].

## Cache (Redis, no BFF)

TTL padrão de 60s, configurável por `CACHE_TTL_SEGUNDOS`.

Duas regras não óbvias:

1. **A chave inclui o observador.** Caso contrário o cache vazaria dado entre
   usuários.
2. **Descoberta por bbox não é cacheada de propósito** — a área varia a cada gesto
   de mapa e por usuário; o *hit rate* seria quase zero e o custo de memória, alto.

## Propagação do token e do trace

O BFF **repassa o mesmo Bearer** que recebeu. Não há token de serviço nem
credencial técnica: o downstream vê o usuário final e aplica as próprias regras.
Junto vai o `X-Trace-Id`, que amarra o log das quatro pontas.

## Detalhes de infraestrutura HTTP

- O BFF usa `JdkClientHttpRequestFactory`. O `SimpleClientHttpRequestFactory` **não
  suporta PATCH**, e várias rotas usam PATCH.
- Os serviços resolvem URLs por variável de ambiente (`SERVICO_APP_URL`,
  `SERVICO_LOCALIZACAO_URL`, ...), com default `localhost:<porta>` para dev.
