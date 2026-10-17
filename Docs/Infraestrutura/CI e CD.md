---
tags: [infra, ci]
atualizado: 2026-07-25
---

# CI e CD

GitHub Actions em cada repositório. O deploy é **manual por clique** e está
dormente — ver [[Deploy em Produção]].

## Serviços backend (5 repos)

`.github/workflows/ci.yml` em cada um:

```
push/PR ──► build + testes (Testcontainers)
                  │
            na master ──► publica ghcr.io/rafaeldossanto/trilha-<svc>:latest
                                                              :sha-<commit>
```

Detalhes:
- **Dockerfile multi-stage**: `jarmode tools` separa a camada `lib/`, usuário
  non-root, JRE 21 alpine
- O jar é buildado **uma vez** no job de teste e reaproveitado
- O `-plain.jar` é filtrado
- `gradlew` precisou de `chmod +x` (100644 → 100755) — o CI roda em Linux

## Fronts

- **[[App Mobile]]**: `flutter analyze` + `flutter test` + APK debug como artefato
- **[[App Web]]**: `flutter analyze` + `flutter test` + `flutter build web --release`
  como artefato

> [!warning] `flutter analyze` sai com código 1 até em `info`
> Uma simples depreciação (`activeColor`) já deixa o CI vermelho. Vale rodar
> localmente antes de empurrar.

## Deploy

`deploy.yml` no repo `TrishaInfra`, disparado só por `workflow_dispatch`:

1. confere se os secrets da VPS existem (falha com mensagem clara se não)
2. copia compose e configs por SCP
3. `docker compose pull` + `up -d` na VPS, por SSH

Secrets necessários: `VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY`.

## Estado real do pipeline

> [!danger] As suítes estavam vermelhas há semanas
> Descoberto em 25/07/2026: a suíte do [[Serviço APP]] **nem compilava** desde
> 21/07, o que mascarava 49 falhas de contexto. O [[Serviço Cadastro]] tinha teste
> desatualizado desde a rodada de IDOR (15/07) e o [[Serviço Mídia]] desde a
> refatoração do MinIO.
>
> Como o job que publica a imagem **depende** do job de teste, é provável que
> **nenhuma imagem válida tenha sido publicada** no ghcr.io até então. Vale conferir
> a aba Actions de cada repo.
>
> Todas foram corrigidas nessa data; os cinco serviços passaram a ter
> `./gradlew test` com saída 0.

> [!note] Pacotes novos no ghcr.io nascem privados
> Na primeira publicação de cada imagem é preciso dar acesso de leitura à VPS — ou
> tornar público, ou gerar um PAT `read:packages` para o `docker login` no servidor.

## Rodar localmente

```bash
# backend (exige JDK 21 — ver Ambientes)
./gradlew test          # unidade
./gradlew integrationTest   # Testcontainers, sourceSet separado

# fronts
flutter analyze && flutter test
```

> [!note] `integrationTest` é um sourceSet separado
> Não roda junto com `test`. Os testes existem e compilam, mas **nunca foram
> executados** localmente.
