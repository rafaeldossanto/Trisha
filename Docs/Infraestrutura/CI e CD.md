---
tags: [infra, ci]
atualizado: 2026-10-17
---

# CI e CD

GitHub Actions no monorepo `rafaeldossanto/Trisha`, em `.github/workflows/` na
raiz (o Actions não lê workflows dentro de subpastas). O deploy é **manual por
clique** e está dormente — ver [[Deploy em Produção]].

## Branches

- **`main`**: a principal. Só push nela publica imagem no ghcr.io, APK e build
  web, e só a partir dela os dois deploys aceitam rodar.
- **`dev`**: o trabalho do dia a dia. Roda os mesmos testes, não publica nada e
  chega na `main` por merge.

PR roda os testes da pasta tocada, para qualquer branch.

## Filtro por pasta

Cada workflow só dispara quando a pasta dele (ou o próprio workflow) muda. Um
commit só no `APP/` roda só o CI do APP: os outros quatro serviços não são
testados nem republicados à toa.

| Workflow | Dispara com |
|---|---|
| `app.yml`, `cadastro.yml`, `loc.yml`, `midia.yml`, `bff.yml` | a pasta do serviço ou `_java-service.yml` |
| `mobile.yml` | `trilha_app/` |
| `web.yml` | `trilha_web/` |
| `deploy.yml`, `deploy-web.yml` | só clique manual |

## Serviços backend

Os cinco chamam o mesmo workflow reutilizável, `_java-service.yml`, passando a
pasta e o nome da imagem. Antes do monorepo eram cinco `ci.yml` idênticos exceto
pela imagem.

```
push/PR ──► build + testes (unidade + Testcontainers)
                  │
            push na main ──► publica ghcr.io/rafaeldossanto/trilha-<svc>:latest
                                                                :sha-<commit>
```

Detalhes:
- **Dockerfile multi-stage**: `jarmode tools` separa a camada `lib/`, usuário
  non-root, JRE 21 alpine. O contexto do build é a pasta do serviço.
- O jar é buildado **uma vez** no job de teste e reaproveitado
- O `-plain.jar` é filtrado
- `gradlew` precisou de `chmod +x` (100644 → 100755) — o CI roda em Linux

> [!warning] Imagens criadas antes do monorepo
> Cada pacote do ghcr.io continua ligado ao repositório que o publicou primeiro
> (`trilha-app` ao antigo `APP`, etc.). Sem dar acesso ao `Trisha`, o job de
> imagem leva **403**. Em Packages > (imagem) > Package settings > Manage Actions
> access, adicione o repo `Trisha` com papel **Write**.

## Fronts

- **[[App Mobile]]** (`mobile.yml`): `flutter analyze` + `flutter test`; na `main`,
  APK debug como artefato
- **[[App Web]]** (`web.yml`): `flutter analyze` + `flutter test`; na `main`,
  `flutter build web --release` como artefato

> [!warning] `flutter analyze` sai com código 1 até em `info`
> Uma simples depreciação (`activeColor`) já deixa o CI vermelho. Vale rodar
> localmente antes de empurrar.

## Deploy

`deploy.yml`, disparado só por `workflow_dispatch` e só na `main`:

1. confere se os secrets da VPS existem (falha com mensagem clara se não)
2. **guard de CI**: para cada serviço alvo, pega o último run de push na `main`
   do workflow dele e bloqueia se quebrou, se ainda está rodando, se não existe,
   ou se a pasta do serviço mudou depois daquele run
3. copia compose e configs de `trilha-infra/` por SCP (`strip_components`
   mantém `~/trilha` igual na VPS)
4. `docker compose pull` + `up -d` na VPS, por SSH

> [!note] Por que "a pasta mudou depois do run" e não "o run é do HEAD"
> Com o filtro por pasta, o último run do loc pode ser de um commit anterior ao
> HEAD da `main` e ainda assim ser exatamente a `:latest` publicada. Exigir o run
> no HEAD travaria todo deploy depois de um commit que não tocou o loc.

Secrets necessários: `VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY`. O guard usa o
`GITHUB_TOKEN` do próprio repo (`actions: read`). Antes do monorepo a consulta
cruzava repositórios e tinha que ser anônima, com limite de 60 req/h.

`deploy-web.yml` publica o [[App Web]] no Firebase Hosting, também só na `main`.
Secrets: `FIREBASE_SERVICE_ACCOUNT` e `FIREBASE_PROJECT_ID`.

## Estado real do pipeline

> [!danger] As suítes estavam vermelhas há semanas
> Descoberto em 25/07/2026: a suíte do [[Serviço APP]] **nem compilava** desde
> 21/07, o que mascarava 49 falhas de contexto. O [[Serviço Cadastro]] tinha teste
> desatualizado desde a rodada de IDOR (15/07) e o [[Serviço Mídia]] desde a
> refatoração do MinIO.
>
> Como o job que publica a imagem **depende** do job de teste, é provável que
> **nenhuma imagem válida tenha sido publicada** no ghcr.io até então.
>
> Todas foram corrigidas nessa data; os cinco serviços passaram a ter
> `./gradlew test` com saída 0.

> [!note] Pacotes novos no ghcr.io nascem privados
> Na primeira publicação de cada imagem é preciso dar acesso de leitura à VPS — ou
> tornar público, ou gerar um PAT `read:packages` para o `docker login` no servidor.

## Rodar localmente

```bash
# backend (exige JDK 21 — ver Ambientes), de dentro da pasta do serviço
./gradlew test              # unidade
./gradlew integrationTest   # Testcontainers, sourceSet separado

# fronts, de dentro de trilha_app/ ou trilha_web/
flutter analyze && flutter test
```

> [!note] `integrationTest` é um sourceSet separado
> Não roda junto com `test`, mas o `./gradlew build` do CI roda os dois. Os testes
> de integração rodaram localmente pela primeira vez em 27/07/2026, verdes nos
> cinco serviços.
