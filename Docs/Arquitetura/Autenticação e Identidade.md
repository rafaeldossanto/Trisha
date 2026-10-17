---
tags: [arquitetura, seguranca]
atualizado: 2026-07-30
---

# Autenticação e Identidade

Um emissor, muitos validadores. O [[Serviço Cadastro]] é o **único** que assina
token; todos os outros validam pela chave pública.

## O fluxo

```
1. Front → POST /bff/auth/login (e-mail + senha)
2. BFF   → POST /auth/login no Cadastro
3. Cadastro valida (BCrypt) e assina um JWT com a chave RSA privada
4. Token volta ao front, que guarda em armazenamento seguro
5. Toda requisição seguinte leva Authorization: Bearer <token>
6. BFF valida o token e o PROPAGA para o serviço downstream
7. O serviço downstream valida o mesmo token pelo JWKS do Cadastro
```

## Chave e JWKS

- O Cadastro expõe a chave **pública** em `GET /oauth2/jwks`.
- Cada serviço configura `jwk-set-uri` apontando para lá (`JWKS_URI`).
- A chave **privada** é um PEM PKCS#8:
  - **dev**: uma chave embarcada em `classpath:keys/dev-private-key.pem` — não é
    segredo real;
  - **prod**: montada em `/keys/jwt-private.pem` pelo compose. Sem ela o Cadastro
    **aborta o boot de propósito** (ver [[Deploy em Produção]]).

## O subject é a identidade

O `sub` do token é o **id interno da conta**. Isso é o que permite fechar IDOR:
qualquer rota que receba um id de usuário compara com o `sub` antes de qualquer
consulta. Ver [[Visibilidade e Controle de Acesso]] e [[Segurança]].

## Formas de entrar

| Forma | Endpoint | Estado |
|---|---|---|
| E-mail + senha | `POST /auth/login` | Funciona; senha com **BCrypt** |
| Social (Google/Apple) | `POST /auth/social` | Valida o ID token pelo JWKS do provedor |
| Dev-login | `POST /auth/dev-login` | Só no profile `dev`; cria/reusa conta sem senha |

> [!note] O App Web ainda entra por dev-login
> O [[App Mobile]] passou a usar as três formas em 2026-07-30: login por senha,
> criação de conta e login social (Google; Apple só no iOS, que ainda não existe
> como plataforma no projeto). O dev-login continua lá, mas só em build de debug.
> O [[App Web]] segue sem telas de cadastro e entrando por dev-login.
>
> Recuperação de senha não existe em lugar nenhum — nem backend, nem front.

## Cadastro por e-mail

O `POST /usuario` cria a conta em `PENDENTE` e dispara o e-mail de confirmação fora
da requisição. A conta só vira `ATIVO` quando o e-mail é confirmado **e** os termos
são aceitos — em qualquer ordem. Passado o prazo sem as duas etapas, uma varredura
periódica desativa o cadastro e libera o e-mail. Detalhes em [[Serviço Cadastro]];
o porquê de não ser mais um processo Camunda, em [[Remoção do Camunda]].

## Proteções

- **Lockout por conta**: após N falhas de login, a conta trava por um tempo (contador
  no Redis, com *fail-open* se o Redis cair).
- **Rate limit por IP** na borda: teto menor para rotas de autenticação. Ver [[BFF]].
