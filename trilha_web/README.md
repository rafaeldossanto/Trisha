# trilha_web

Front web (desktop) do **Trisha** — a mesma base visual do app mobile
(`trilha_app`), adaptada para o browser: side nav estilo Instagram em janelas
largas, conteudo central com largura maxima por tela e o mapa colaborativo
full-bleed como home.

## Rodar em dev

Suba o back com o profile `dev` (Cadastro `:8080`, BFF `:8090`, loc `:8082`,
midia `:8083`) e:

```
flutter run -d chrome
```

O `.env` ja aponta para `localhost` (o CORS do BFF e da midia libera
`http://localhost:*` em dev).

## Diferencas em relacao ao trilha_app

- **Web-only**: so a plataforma `web/` e gerada.
- `MainShell` responsivo: `>=900px` side nav (rotulos a partir de `1150px`);
  abaixo disso, a bottom bar original.
- Telas empurradas no root navigator usam o wrapper `WebPage` (coluna central
  com largura maxima); telas de mapa/rastreio seguem em tela cheia.
- Upload de midia por bytes (`XFile.readAsBytes`) — na web nao existe caminho
  de arquivo.
- `scrollBehavior` habilita arrastar listas/carrosseis com mouse e trackpad.

## Build de producao

O `.env` e um **asset do bundle**: o valor entra no build e nao da para trocar
depois. Por isso a configuracao de producao vive em `.env.prod`, que e copiado
por cima do `.env` antes do build — sem essa troca, o site publicado chamaria o
`localhost` de quem o abrisse.

```
cp .env.prod .env && flutter build web --release
```

Artefato em `build/web/` (estatico).

## Deploy (Firebase Hosting)

1. Preencher o dominio real da API em `.env.prod` (o mesmo `API_DOMAIN` do
   `.env` da `trilha-infra`). As tres URLs apontam para o mesmo host: o Caddy
   roteia por path (`/bff` → BFF, `/arquivo` → Midia, `/ws-localizacao` → loc).
2. Criar os secrets no GitHub (Settings > Secrets and variables > Actions):
   - `FIREBASE_SERVICE_ACCOUNT` — JSON da service account com papel *Firebase
     Hosting Admin* (Console do Firebase > Configuracoes do projeto > Contas de servico)
   - `FIREBASE_PROJECT_ID` — id do projeto
3. Aba **Actions > Deploy web > Run workflow**.

O dominio publicado (custom ou o `*.web.app` do projeto) precisa estar em
`CORS_ALLOWED_ORIGINS` no `.env` da `trilha-infra`, senao o browser bloqueia as
chamadas. E a API precisa estar em **https**: uma pagina servida pelo Firebase
(sempre https) nao consegue chamar `http://` — o browser barra por mixed content.
