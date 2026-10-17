---
tags: [front, flutter, mobile]
repo: frontMobile
atualizado: 2026-07-30
---

# App Mobile

**Flutter 3.44.2 · pasta `trilha_app` · repo `frontMobile` · Android apenas**

O cliente original. Só a plataforma `android/` é gerada — não há iOS, web nem
desktop aqui (para web, ver [[App Web]]).

## Arquitetura interna

Organização por **feature**, cada uma com `domain / data / presentation`:

```
lib/
├── core/          network (Dio + interceptors), storage, router, theme, env
├── features/      auth, adventure, path, point, map, tracking,
│                  friendship, follower, region, feed, explore
└── shared/        widgets reutilizáveis (StoryAvatar, AdventureGrid, MainShell)
```

- **Estado**: `provider` (ChangeNotifier)
- **Rotas**: `go_router` com guard de autenticação e `StatefulShellRoute`
- **HTTP**: `dio` com `AuthInterceptor` (injeta o Bearer, limpa a sessão no 401) e
  um retry para 503
- **Sessão**: `flutter_secure_storage` (Keystore no Android)
- **Mapa**: `flutter_map` + `latlong2`; tiles Carto dark, alternável para OpenTopoMap
- **GPS**: `geolocator`
- **Tempo real**: `stomp_dart_client` — ver [[Tempo Real]]

## Navegação (estilo Instagram)

`MainShell` com bottom bar de 5 itens:

| Posição | Rota | Tela |
|---|---|---|
| 1 | `/home` | mapa colaborativo em tela cheia |
| 2 | `/explorar` | busca de trilheiros + grade de coleções |
| 3 | — | abre o sheet **criar aventura** (não é aba) |
| 4 | `/feed` | stories + posts |
| 5 | `/perfil-meu` | contadores + grade 3×3 |

Telas de detalhe são empurradas no root navigator, cobrindo a shell.

## A home é o mapa

`HomeMapScreen` é a visão original do produto (referências: Strava e
helios.ratonet.com.br): mapa escuro em tela cheia com as trilhas rastreadas
plotadas em cores (paleta cíclica por aventura, polyline dupla criando *glow*), e
um carrossel de cards embaixo que centraliza o mapa na trilha ao deslizar.

As trilhas da comunidade chegam por **bbox** com debounce de 700ms e zoom mínimo 10.
Marcadores "AO VIVO" abrem a tela de acompanhamento.

## Entrada no app

A tela de login usa as formas de verdade — ver [[Autenticação e Identidade]]:

| Ação | Endpoint do BFF |
|---|---|
| Entrar | `POST /bff/auth/login` (e-mail + senha) |
| Criar conta (`/cadastro`) | `POST /bff/usuarios` → conta **PENDENTE** até confirmar o e-mail |
| Reenviar confirmação | `POST /bff/usuarios/{id}/reenviar-email` |
| Google / Apple | `POST /bff/usuarios/login-social` (`provedor` + `idToken`) |

O ID token do provedor é obtido no aparelho (`google_sign_in`,
`sign_in_with_apple`) e trocado pela sessão no backend. O botão da Apple só
aparece no iOS — que ainda não é uma plataforma gerada aqui. O `dev-login`
sobrou como atalho, visível apenas em build de debug (`kDebugMode`).

## Configuração

`.env` (carregado por `flutter_dotenv`, declarado como asset):

```
API_BASE_URL=http://10.0.2.2:8090     # 10.0.2.2 = localhost do host, no emulador
MIDIA_BASE_URL=http://10.0.2.2:8083
LOC_WS_URL=http://10.0.2.2:8082/ws-localizacao
GOOGLE_SERVER_CLIENT_ID=              # Web client ID do Google Cloud
```

`GOOGLE_SERVER_CLIENT_ID` é o **Web** client ID (não o do Android) e precisa ser
o mesmo valor do `GOOGLE_CLIENT_ID` do [[Serviço Cadastro]]: é ele que vira o
`aud` do ID token que o backend valida. Vazio, o botão avisa em vez de falhar mudo.

## Lacunas conhecidas

- Sem recuperação de senha (não existe nem no backend)
- Sem tela de participantes (o *kick* existe só na API)
- Sem trilhas offline, gamificação ou SOS (roadmap)

Identidade visual compartilhada com o [[App Web]] — ver [[Identidade Visual]].
