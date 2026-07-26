---
tags: [servico, backend]
porta: 8080
banco: trilha_cadastro
repo: Cadastro
atualizado: 2026-07-25
---

# Serviço Cadastro

**Porta 8080 · banco `trilha_cadastro` · pacote `com.trail.Cadastro`**

Dono da identidade. É o único serviço que guarda senha e o único que **assina**
token — ver [[Autenticação e Identidade]].

## Endpoints

### Conta
| Método | Rota | Observação |
|---|---|---|
| POST | `/usuario` | **Inicia o processo Camunda**, não é insert simples |
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

## O processo Camunda (Zeebe)

`POST /usuario` cria a conta **e** inicia o processo `cadastro-usuario`:

```
início ──► envio-email ──► [espera] email-confirmado
                                 │
                                 ├─► termos-aceitos ──► liberacao-conta ──► fim
                                 │
                                 └─► (timer ~10min) ──► delecao-dados
```

- Workers: `envio-email`, `liberacao-conta`, `delecao-dados`
- Mensagens correlacionadas por `usuarioId`
- O deploy do BPMN é feito por `@Deployment` na classe `Application`

> [!danger] Sem Zeebe, ninguém se cadastra
> Se o `RegistrationService` não conseguir iniciar o processo, ele **apaga a conta
> recém-criada** e lança exceção — de propósito, para liberar o e-mail para nova
> tentativa. Portanto o Zeebe é dependência **dura**. O endereço vem de
> `ZEEBE_GATEWAY_ADDRESS`; em [[Ambientes]] há como subir o broker.

## Regras que valem conhecer

- **Reaproveitamento de conta INATIVA**: um cadastro cujo prazo expirou volta a
  `PENDENTE` preservando `id` e `codigoUsuario`. Isso corrige o e-mail "preso" após
  timeout.
- **Login social normaliza o status**: se a conta está `PENDENTE`, publica
  `email-confirmado` + `termos-aceitos` (encerrando o processo, senão o timer
  apagaria uma conta em uso) e ativa.
- **`codigoUsuario`** (`nome#numero`) usa uma **sequence atômica**. Antes era
  `COUNT(*) + 1`, que tinha corrida e gerava código duplicado.
- **Lockout**: N falhas de login travam a conta por um tempo (Redis, *fail-open*).

## Dependências

Postgres · Redis (lockout) · Zeebe (obrigatório) · SMTP (e-mail)

Consumido por [[BFF]]; a chave pública é consumida por [[Serviço APP]],
[[Serviço Localização]] e [[Serviço Mídia]].
