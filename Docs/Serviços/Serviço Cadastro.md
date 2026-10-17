---
tags: [servico, backend]
porta: 8080
banco: trilha_cadastro
repo: Trisha
pasta: Cadastro
atualizado: 2026-10-17
---

# Serviço Cadastro

**Porta 8080 · banco `trilha_cadastro` · pacote `com.trail.Cadastro`**

Dono da identidade. É o único serviço que guarda senha e o único que **assina**
token — ver [[Autenticação e Identidade]].

## Endpoints

### Conta
| Método | Rota | Observação |
|---|---|---|
| POST | `/usuario` | cria em `PENDENTE` e dispara o e-mail de confirmação (assíncrono) |
| GET | `/usuario/{id}` | exige `sub == id` (anti-IDOR) |
| PUT | `/usuario/{id}` | idem; não altera senha |
| DELETE | `/usuario/{id}` | idem |

### Autenticação
| Método | Rota | Observação |
|---|---|---|
| POST | `/auth/login` | e-mail + senha (BCrypt); exige status `ATIVO` |
| POST | `/auth/social` | valida ID token do Google/Apple |
| POST | `/auth/dev-login` | **só no profile `dev`** |
| GET | `/oauth2/jwks` | chave pública — consumida por todos os serviços |

### Confirmação
| Método | Rota | Observação |
|---|---|---|
| GET | `/auth/confirmar-email` | público (usuário ainda sem token) |
| POST | `/auth/reenviar-email` | cooldown de 30s |
| POST | `/auth/aceitar-termos` | público |

## Entidades

- `User` — id, nome, email, senha (BCrypt), `codigo_usuario`, status, timestamps
- `EmailConfirmation` — token, status, `expira_em`, `enviado_em`
- `TermsAcceptance` — aceite e versão dos termos
- `LinkedAccount` — vínculo com provedor social

> [!warning] `status` é gravado como ORDINAL
> `User.status` e `EmailConfirmation.status` **não** têm `@Enumerated(STRING)`,
> então o JPA persiste o número do enum. A migration de schema base usa `SMALLINT`
> nessas colunas. Mudar a ordem dos valores do enum corromperia os dados
> existentes. Ver [[Banco de Dados e Migrations]].

## O fluxo de cadastro

Até 01/08/2026 isso era um processo BPMN no Camunda 8 / Zeebe. Hoje é resolvido no
próprio banco — o porquê está em [[Remoção do Camunda]].

```
POST /usuario          ──► grava PENDENTE, publica evento ──► @Async manda e-mail
GET  /confirmar-email  ──► marca CONFIRMADO + tryActivate()   ┐ mesma transação
POST /aceitar-termos   ──► grava o aceite  + tryActivate()    ┘
@Scheduled (1 min)     ──► desativa PENDENTE vencido
```

`tryActivate(userId)` ativa **só** quando as duas etapas estão cumpridas. As etapas
correm em qualquer ordem; a última a chegar é quem ativa.

> [!important] A ativação é um `UPDATE` condicional
> `UserRepository.activateIfPending` roda
> `UPDATE ... WHERE id = ? AND status = PENDENTE` e devolve as linhas afetadas.
> Confirmar e-mail e aceitar termos podem chegar **simultâneos** — o link do e-mail
> costuma ser aberto em outro dispositivo, então ordenar as telas no front não
> impede a corrida. Quem arbitra é o banco: a segunda chamada afeta zero linhas.

> [!note] O corte de expiração usa `updatedAt`, não `createdAt`
> Uma conta reaproveitada (ver abaixo) volta a `PENDENTE` mantendo a data de criação
> original. Por `createdAt`, todo recadastro seria expirado no primeiro ciclo.

Peças: `ConfirmationService` (ativação) · `RegistrationCleanupJob` (prazo) ·
`UserRegisteredListener` + `ConfirmationMailer` (e-mail com `@Retryable`).

Configuração: `app.cadastro.expiracao-minutos` (10) e
`app.cadastro.intervalo-limpeza-ms` (60000).

## Regras que valem conhecer

- **Reaproveitamento de conta INATIVA**: um cadastro cujo prazo expirou volta a
  `PENDENTE` preservando `id` e `codigoUsuario`. Isso corrige o e-mail "preso" após
  timeout.
- **Login social normaliza o status**: se a conta está `PENDENTE`, o provedor já
  confirmou o e-mail, então ela é ativada direto — sem exigir confirmação nem aceite
  de termos. Sem isso a limpeza desativaria uma conta em uso.
- **`codigoUsuario`** (`nome#numero`) usa uma **sequence atômica**. Antes era
  `COUNT(*) + 1`, que tinha corrida e gerava código duplicado.
- **Lockout**: N falhas de login travam a conta por um tempo (Redis, *fail-open*).

## Dependências

Postgres · Redis (lockout) · SMTP (e-mail)

Não há mais dependência de broker externo para o cadastro funcionar.

Consumido por [[BFF]]; a chave pública é consumida por [[Serviço APP]],
[[Serviço Localização]] e [[Serviço Mídia]].
