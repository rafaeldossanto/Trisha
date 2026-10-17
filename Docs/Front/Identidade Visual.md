---
tags: [front, design]
atualizado: 2026-07-25
---

# Identidade Visual

Tema escuro compartilhado entre [[App Mobile]] e [[App Web]], definido em
`core/theme/app_theme.dart` nos dois repos.

## Paleta

| Token | Valor | Uso |
|---|---|---|
| Fundo | `#000000` | preto puro, estilo Instagram dark |
| Superfície | `#121212` | cards, campos, sheets |
| Verde de marca | `#4CAF7D` | botões, destaques, seed do `ColorScheme` |
| Divisores | `white12` | linhas e bordas |
| Gradiente de story | `#FEDA75 → #FA7E1E → #D62976 → #962FBF → #4F5BD5` | anel do avatar |

Paleta das trilhas no mapa (cíclica por aventura): verde, roxo, ciano, âmbar,
vermelho, verde-água, rosa, azul.

## Wordmark

A marca exibida é **Trisha** (os identificadores no código continuam `Trilha` —
ver [[Decisões Deliberadas]]).

**Fonte: Yellowtail**, script de pincel, licença SIL OFL, **embarcada** em
`assets/fonts/Yellowtail-Regular.ttf` (~34 KB) nos dois repos. Escolhida em
25/07/2026 entre 10 candidatas apresentadas visualmente; substituiu um
`fontFamily: 'serif'` itálico genérico.

> [!danger] Duas regras que o desenho impõe
> Estão comentadas no `AppTheme` — não remover:
> 1. **Nunca** `fontWeight` ou `fontStyle`. A fonte tem peso único e já vem
>    inclinada; pedir bold ou itálico faz o Flutter **sintetizar** e deformar o
>    traço.
> 2. **`letterSpacing: 0`**. As letras são conectadas; qualquer espaçamento abre
>    buraco na emenda. (O estilo antigo usava `0.5`.)

Tokens:
- `AppTheme.wordmark` — 30px, barras do feed/mapa/side nav
- `AppTheme.wordmarkLarge` — 52px, tela de entrar

Ambos derivam da const privada `_wordmarkFamily`.

> [!tip] Como baixar a fonte de novo, se precisar
> A API de CSS do Google serve formatos diferentes conforme o **User-Agent**. Um UA
> de MSIE devolve **EOT** (assinatura = tamanho do arquivo em little-endian, não
> `00 01 00 00`), que o Flutter não lê. Use um UA de Android antigo para receber TTF
> de verdade. O caminho `raw.githubusercontent.com/google/fonts/main/ofl/yellowtail/`
> retorna 404.

## Logo

Picos de montanha desenhados em `CustomPaint` (`_PeakLogo`), traço branco sem base.

> [!bug] Bug corrigido em 25/07/2026
> Dentro de uma `Column` com `CrossAxisAlignment.stretch`, o `CustomPaint` recebia a
> largura inteira e o painter — que escala por `size.width / 24` — desenhava o logo
> por cima da tela toda (1352×56 em vez de 56×56). Corrigido envolvendo em `Center`,
> com teste de regressão nos dois repos.

## Componentes recorrentes

- **`StoryAvatar`** — avatar circular com anel de gradiente opcional; sem foto,
  mostra a inicial do nome
- **`AdventureGrid`** — grade 3 colunas com a primeira mídia como capa
- **`StatusPin`** — pin de mapa desenhado, com a ponta no centro inferior

## Referência visual

Existem dois artifacts com as telas reconstruídas a partir do código:
um do mobile (22 telas) e um do web (9 pranchas + regras de layout). Os links estão
na memória do projeto.
