# Trilha — App (Flutter)

App mobile da rede social de trilhas. Consome o **BFF** (porta 8090), que orquestra
os microserviços (Cadastro, APP, loc, midia). Tema escuro, foco outdoor.

## Stack

| Camada | Pacote |
|---|---|
| Rede | `dio` (+ interceptor que injeta o `Bearer`) |
| Estado | `provider` (ChangeNotifier) |
| Navegação | `go_router` (com guard de autenticação) |
| Sessão | `flutter_secure_storage` (token + usuarioId) |
| Config | `flutter_dotenv` (`.env`) |
| Mapa | `flutter_map` + `latlong2` + `geolocator` (tiles dark CartoDB) |

## Arquitetura

Organização **feature-first** com **3 camadas** por feature — o equivalente Flutter de
um projeto Spring em camadas:

```
lib/
├── main.dart          # entrada: carrega .env, monta a injeção de dependências, runApp
├── app.dart           # MaterialApp.router (tema + rotas)
│
├── core/              # infra transversal (≈ config/util)
│   ├── env/           # leitura tipada do .env
│   ├── network/       # Dio, AuthInterceptor, PaginaResponse<T> genérico
│   ├── router/        # go_router + guard (redirect)
│   ├── storage/       # secure storage do token
│   └── theme/         # tema escuro
│
├── features/          # uma pasta por funcionalidade
│   └── <feature>/
│       ├── data/         # APIs e repositórios (falam com o BFF)  ≈ @Repository / client
│       ├── domain/       # modelos + fromJson                     ≈ entity / DTO
│       └── presentation/ # telas e providers (estado)             ≈ controller + view
│
└── shared/            # widgets reutilizáveis entre features
```

**Regra de dependência (a que mantém o código organizado):** sempre aponta para dentro —
`presentation` → `data` → `domain`. O `domain` é modelo puro e não conhece ninguém; o
`data` fala com a rede; o `presentation` só orquestra estado e UI. Cada feature é uma
fatia fechada.

Features: `auth` (login dev), `home`, `aventura` (listar/criar), `mapa` (localização),
`ponto` (pontos de interesse com nível de confiança).

## Configuração (`.env`)

```
API_BASE_URL=http://10.0.2.2:8090
```

`10.0.2.2` é o `localhost` da máquina host visto de dentro do emulador Android. O arquivo
é registrado como asset no `pubspec.yaml` e carregado no `main`.

## Rodando

Pré-requisitos: Flutter instalado, um emulador Android e o **backend no ar com o profile
`dev`** (pelo menos Cadastro `8080` e BFF `8090`, para o `/auth/dev-login` existir).

```bash
flutter pub get
flutter emulators --launch trilha_pixel   # ou abra um emulador pela IDE
flutter run
```

Fluxo: login (nome + e-mail) → home → Minhas aventuras (listar/criar) → toque numa
aventura → mapa escuro com a localização e os pontos coloridos por nível de confiança.

> Criar aventura exige um `regiaoId` que exista no APP (ainda não há cadastro de regiões;
> use um id de seed), senão o backend recusa com 400.

## Convenções

- Imports relativos dentro de `lib`.
- Um modelo de dados = uma classe em `domain/` com `fromJson`.
- Estado de tela = um `ChangeNotifier` em `presentation/` (`loading` / `error` / dados).
- Erros de rede viram mensagem amigável no provider, não estouram na UI.

## Testes

```bash
flutter analyze   # estático (deve ficar limpo)
flutter test      # testes de widget/unidade
```
