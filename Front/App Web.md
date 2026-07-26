---
tags: [front, flutter, web]
repo: TrishaWeb
atualizado: 2026-07-25
---

# App Web

**Flutter 3.44.2 · pasta `trilha_web` · repo `TrishaWeb` · web apenas**

Criado em 25/07/2026 como **repositório separado** do [[App Mobile]] — decisão
explícita, para a V1 web sair primeiro sem arrastar o mobile. O `lib/` foi copiado
e adaptado; a duplicação é assumida (ver [[Decisões Deliberadas]]).

## O que muda em relação ao mobile

### Navegação responsiva
`MainShell` troca de forma conforme a largura da janela:

| Largura | Navegação |
|---|---|
| ≥ 1150px | side nav de **244px** com wordmark e rótulos |
| 900–1149px | side nav de **76px**, só ícones |
| < 900px | bottom bar original de 52px |

### Largura máxima por aba
| Rota | Máximo | Motivo |
|---|---|---|
| `/home` | full-bleed | o mapa é a tela |
| `/feed` | 630px | coluna de leitura |
| `/explorar`, `/perfil-meu` | 935px | grade respira |
| rotas empurradas | 800px | via wrapper `WebPage` |
| `/login` | 440px | formulário curto |

> [!note] Rotas empurradas cobrem a side nav
> Telas como `/aventuras/:id` são empurradas no root navigator e **escondem a
> navegação lateral** enquanto abertas. É fiel ao empilhamento do mobile, mas na web
> o normal é manter a nav visível. Decisão em aberto.

### Adaptações técnicas
- **Upload por bytes**: `MultipartFile.fromBytes` com `XFile.readAsBytes` —
  `fromFile` depende de `dart:io` e não existe na web
- **`scrollBehavior`** custom: libera arrastar listas e carrosséis com mouse e
  trackpad (o padrão do Flutter web só aceita toque)
- **Bottom sheets** com teto de 560px (senão esticam na janela inteira)
- **Tela de boot** no `index.html`: em modo debug o DDC carrega ~800 módulos e leva
  1–2 minutos; sem feedback a página preta parecia travada

### Configuração
`.env` aponta direto para `localhost` (não há emulador no meio):

```
API_BASE_URL=http://localhost:8090
MIDIA_BASE_URL=http://localhost:8083
LOC_WS_URL=http://localhost:8082/ws-localizacao
```

## Pré-requisito no backend

O browser bloqueia tudo sem **CORS**. O [[BFF]] e o [[Serviço Mídia]] precisam ter
`CORS_ALLOWED_ORIGINS` com a origem do front. Ver [[Segurança]].

## Rodar

```
flutter run -d chrome            # dev
flutter build web --release      # artefato estático em build/web/
```

> [!tip] Prefira `--release` para testar rápido
> Em debug o boot leva 1–2 minutos por causa do DDC. Em release compila para um JS
> único e sobe em segundos.

Identidade visual em [[Identidade Visual]].
