# Trisha

Rede social open source para registrar e compartilhar aventuras em trilhas: o
usuário grava o percurso por GPS, marca pontos de interesse com foto como
evidência e compartilha num mapa colaborativo validado pela comunidade.

Este é o monorepo do projeto: os cinco microserviços, o BFF, os dois fronts, a
infraestrutura e o cofre de documentação vivem aqui.

## Estrutura

| Pasta | O que é | Stack | Porta |
|---|---|---|---|
| [`Cadastro/`](Cadastro) | Contas, login, confirmação de e-mail, emissão do JWT | Spring Boot 4 · Java 21 | 8080 |
| [`APP/`](APP) | Domínio: aventuras, caminhos, pontos de interesse, social | Spring Boot 4 · Java 21 | 8081 |
| [`loc/`](loc) | Rastreamento GPS, sessões, trilhas no mapa (PostGIS) | Spring Boot 4 · Java 21 | 8082 |
| [`midia/`](midia) | Upload e leitura de fotos (MinIO) | Spring Boot 4 · Java 21 | 8083 |
| [`bff/`](bff) | Backend for Frontend: a única porta pública da API | Spring Boot 4 · Java 21 | 8090 |
| [`trilha_app/`](trilha_app) | App mobile (Android) | Flutter | — |
| [`trilha_web/`](trilha_web) | App web (desktop) | Flutter web | — |
| [`trilha-infra/`](trilha-infra) | Compose de produção e de dev, Caddy, Mosquitto, Postgres | Docker Compose | — |
| [`Docs/`](Docs) | Cofre Obsidian com a documentação do projeto | Markdown | — |

Cada projeto continua **independente**: tem o próprio `gradlew` ou `pubspec.yaml`,
o próprio `.gitignore` e o próprio ciclo de deploy. Não existe build agregador na
raiz. O monorepo junta histórico, CI e revisão num lugar só; ele não acopla os
serviços.

Comece pela documentação: [`Docs/Trilha.md`](Docs/Trilha.md) é o mapa do cofre.

## Branches

- **`main`**: a principal. É o que vai para produção: só ela publica imagens no
  ghcr.io e só a partir dela os workflows de deploy aceitam rodar.
- **`dev`**: onde o trabalho do dia a dia acontece. Roda os mesmos testes, mas não
  publica nada. Chega na `main` por merge.

## CI/CD

Os workflows ficam em [`.github/workflows/`](.github/workflows), cada um filtrado
pela pasta que testa. Um commit que mexe só no `APP/` roda só o CI do APP.

| Workflow | Pasta | Em push na `main` |
|---|---|---|
| `app.yml`, `cadastro.yml`, `loc.yml`, `midia.yml`, `bff.yml` | o serviço | publica `ghcr.io/rafaeldossanto/trilha-<svc>:latest` e `:sha-<commit>` |
| `mobile.yml` | `trilha_app/` | APK de debug como artefato |
| `web.yml` | `trilha_web/` | build web de release como artefato |
| `deploy.yml` | `trilha-infra/` | manual: sobe a stack na VPS (dormente, sem VPS) |
| `deploy-web.yml` | `trilha_web/` | manual: publica no Firebase Hosting |

Os cinco serviços Java compartilham o mesmo corpo de CI,
[`_java-service.yml`](.github/workflows/_java-service.yml) (build, testes de
unidade e de integração com Testcontainers, imagem Docker). Detalhes em
[`Docs/Infraestrutura/CI e CD.md`](Docs/Infraestrutura/CI%20e%20CD.md).

## Rodando localmente

```bash
# serviços (JDK 21 no JAVA_HOME)
cd APP && ./gradlew test               # unidade
cd APP && ./gradlew integrationTest    # Testcontainers

# fronts
cd trilha_app && flutter analyze && flutter test

# infra de apoio para dev (Redis, MinIO, Mosquitto; o Postgres roda nativo)
docker compose -f trilha-infra/docker-compose.dev.yml up -d
```

## Histórico

Até 17/10/2026 cada pasta era um repositório próprio no GitHub. Eles entraram
aqui por merge, **sem reescrita de histórico**: todo commit antigo continua com o
mesmo SHA e pode ser aberto direto (`git show cbb7afd`). Cada import é um commit
de renomeação pura para a pasta, então `git blame` atravessa a mudança sem perder
a autoria original.

| Pasta | Repositório de origem |
|---|---|
| `Cadastro/` | `rafaeldossanto/Cadastro` |
| `APP/` | `rafaeldossanto/APP` |
| `loc/` | `rafaeldossanto/loc` |
| `midia/` | `rafaeldossanto/midia` |
| `bff/` | `rafaeldossanto/TrishaBff` (antigo `bfff`) |
| `trilha_app/` | `rafaeldossanto/frontMobile` |
| `trilha_web/` | `rafaeldossanto/TrishaWeb` |
| `trilha-infra/` | `rafaeldossanto/TrishaInfra` |
| `Docs/` | `rafaeldossanto/TrishaDocs` |

> `git log --follow` segue renomeações pelo **nome** do arquivo, não pela
> linhagem. Para arquivos com nome único (as classes Java, por exemplo) ele
> funciona normalmente. Para nomes que se repetem entre projetos (`build.gradle`,
> `Dockerfile`, `README.md`), ele mistura o histórico dos nove repositórios. Nesses
> casos use `git blame`, ou parta do último commit do repositório antigo com o
> caminho antigo: `git log aa38396 -- build.gradle` mostra só o histórico do
> `build.gradle` do APP. O SHA de cada um está na mensagem do commit de import.
