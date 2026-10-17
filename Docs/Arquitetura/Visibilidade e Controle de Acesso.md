---
tags: [arquitetura, seguranca, dominio]
atualizado: 2026-07-25
---

# Visibilidade e Controle de Acesso

O modelo de privacidade é o conceito mais transversal do produto. Ele aparece no
[[Serviço APP]], é consultado pelo [[Serviço Localização]] e é respeitado pelo
[[BFF]] no cache.

## Os três eixos

### 1. Visibilidade da aventura

| Valor | Quem vê |
|---|---|
| `PRIVADA` | só o dono |
| `SO_GRUPO` | dono + participantes |
| `PUBLICA` | qualquer autenticado |

### 2. Visibilidade da coleção (região)

| Valor | Quem vê |
|---|---|
| `PRIVADA` | só o dono |
| `AMIGOS` | dono + amigos |
| `PUBLICA` | qualquer autenticado |

> [!important] A pasta nunca vaza a aventura
> Uma coleção `PUBLICA` **não** torna públicas as aventuras dentro dela. Ao listar
> `/regiao/{id}/aventuras`, cada aventura ainda é filtrada pela própria
> visibilidade. A pasta é organização, não permissão.

### 3. Visibilidade da sessão ao vivo

| Valor | Quem acompanha |
|---|---|
| `PRIVADO` (padrão) | ninguém |
| `SEGUIDORES` | quem segue o dono |
| `AMIGOS` | amigos confirmados |
| `PUBLICO` | qualquer autenticado |

O padrão é o mais restritivo de propósito: localização em tempo real só é exposta se
o usuário escolher. Ver [[Tempo Real]].

## Onde a regra é aplicada

O `AdventureAccessService` no [[Serviço APP]] centraliza:

- `canView...` — leitura (aventura, caminho, ponto, mídia)
- `canContribute` / `validateContribute` — escrita (dono **ou** participante)
- `canViewPath` — libera **o dono do próprio caminho** antes de olhar a
  visibilidade da aventura. Isso importa: quem sai de uma aventura, ou é movido para
  uma aventura privada, não perde acesso ao que registrou.

O [[Serviço Localização]] não conhece o domínio social; ele **pergunta** ao APP:

- `GET /caminho/{id}/acesso` — posso ver os pontos GPS deste caminho?
- `GET /amizade/sao-amigos`, `GET /seguidor/segue` — para a sessão ao vivo
- `GET /amizade/amigos-ids`, `GET /seguidor/seguindo-ids` — versões **em lote**,
  usadas quando é preciso filtrar muitas sessões de uma vez

## Duas regras que valem lembrar

**Falha fechada.** Se a checagem de acesso não puder ser feita (circuit breaker
aberto, serviço fora), o padrão é **negar**. Um erro de infraestrutura nunca vira
vazamento.

**Cache por observador.** O [[BFF]] cacheia respostas, mas a chave inclui **quem
está perguntando**. Um cache só por id vazaria dado entre usuários — por isso os
`@CacheEvict` viraram `allEntries`.

## Amizade e seguidor

São relações diferentes e a ordem importa:

- **Seguir** é unilateral, sem aceite.
- **Amizade** é bilateral, com aceite — e **exige seguimento mútuo** para ser
  solicitada.
- Deixar de seguir **não** desfaz a amizade.
- **Bloquear** apaga os follows dos dois lados (decisão em [[Decisões Deliberadas]]).

Ver [[Serviço APP]] para os endpoints.
