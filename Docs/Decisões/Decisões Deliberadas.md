---
tags: [decisao]
atualizado: 2026-10-17
---

# Decisões Deliberadas

Escolhas que **parecem** descuido para quem chega agora, mas foram feitas de
propósito. Não "corrigir" sem conversar.

## Arquitetura

### `GeoUtils` duplicado entre serviços
O cálculo de distância existe no [[Serviço APP]] e no [[Serviço Localização]].
Extrair para uma lib compartilhada criaria acoplamento de build entre serviços que
hoje têm ciclo de vida independente. Duplicação pequena e estável é mais barata que
uma dependência comum.

### Cadastro e APP dividem o banco; o resto é separado, tudo na mesma instância
[[Serviço Cadastro]] e [[Serviço APP]] usam o mesmo banco, `trisha`. O Cadastro é o
dono da tabela `usuario`; o APP a lê como entidade `@Immutable`. Decisão de
29/07/2026: com bancos separados, a cópia do lado do APP nascia vazia (nada a
sincronizava) e a busca de usuário, o seguir por código e o nome do autor no feed
paravam de funcionar. Um webhook Cadastro → APP foi avaliado e descartado. O APP
guarda o histórico do Flyway em `flyway_schema_history_app`, porque as versões das
migrations dos dois se cruzam.

[[Serviço Localização]] e [[Serviço Mídia]] têm cada um o próprio banco. Todos
ficam na mesma instância Postgres; instâncias separadas viriam quando a carga
pedir. Ver [[Banco de Dados e Migrations]] e [[Escalabilidade]].

### Código organizado por área, com uma regra entre áreas
Decisão de 17/10/2026, para Cadastro e APP: as pastas seguem a área de negócio
(`adventure/`, `path/`, `social/`... no APP; `account/`, `registration/`,
`authentication/`, `token/` no Cadastro), não a camada técnica. Os serviços
menores (loc, midia, bff) ficaram como estavam, por serem pequenos demais para
ganhar algo com isso.

A regra entre áreas: **escrever** em dado de outra área só pelo service dela;
**ler** pode ser direto no repositório da outra área. Proibir também a leitura
criaria ciclos entre beans (aventura ↔ coleção, aventura ↔ caminho), que o Spring
Boot recusa, e exigiria camadas extras só para contorná-los. As entidades JPA já se
referenciam entre áreas de qualquer forma.

### Padrões só onde há ganho real
No mesmo refactor de 17/10/2026, cada padrão foi pesado contra a complexidade que
traria:
- **Entraram:** um único orquestrador entre áreas (`AdventureMembershipService`:
  sair, remover participante, excluir aventura); o Strategy que já existia no login
  social (`SocialTokenVerifier`); regras na entidade quando são sobre ela mesma (nível
  de confiança do ponto, transições de status do usuário); e o trecho único de JPQL
  da visibilidade da aventura.
- **Ficaram de fora:** Factory de usuário (embrulharia uma linha), uma classe de use
  case por endpoint (triplicaria os arquivos sem regra nova), Strategy de visibilidade
  (um `switch` de 3 casos é mais claro), Specification/Criteria (mais verboso que o
  JPQL) e eventos entre as áreas do APP (a regra acima já resolve os ciclos).

### `trilha_web` como projeto separado do mobile
Decisão de 25/07/2026: a V1 web sai primeiro, sem arrastar o [[App Mobile]]. O
`lib/` foi copiado, e a duplicação é assumida — mesmo espírito do `GeoUtils`.
Nasceu como repositório próprio. Com o monorepo virou uma pasta irmã de
`trilha_app`, e a duplicação continua: estar no mesmo repo não obriga a
compartilhar código.

### Monorepo, mas sem build agregador
Decisão de 17/10/2026: os nove repositórios viraram um só (`rafaeldossanto/Trisha`)
para que uma feature que atravessa serviço, [[BFF]] e front caiba num commit só, e
para haver um único lugar de CI, de histórico e de documentação.

O que **não** mudou é a independência: não existe `settings.gradle` na raiz, cada
serviço mantém o próprio `gradlew`, o próprio CI (filtrado pela pasta) e a própria
imagem. Um build agregador acoplaria o ciclo de vida que o `GeoUtils` duplicado
existe para proteger. Ver [[CI e CD]].

O histórico entrou **sem reescrita** (merge de cada repo após um commit de
renomeação para a pasta). Reescrever com `filter-repo` deixaria o `git log` de
arquivos com nome repetido mais limpo, mas trocaria todo SHA, e os SHAs antigos
são citados na [[Linha do Tempo]] e em mensagens de commit.

### Sem fila entre domínios
Comunicação entre serviços é HTTP síncrono. O acoplamento é assumido e controlado
com circuit breaker. A única assincronia é o tempo real (MQTT + Redis). Ver
[[Comunicação entre Serviços]].

### Os 5 serviços continuam 5
Avaliado em 01/08/2026 fundir Cadastro+APP e Localização+Mídia para baratear a VPS.
**Rejeitado**: economiza só o overhead fixo de JVM, não muda o tier da máquina, e
fundir Cadastro+APP faria todo deploy do feed derrubar o emissor de JWT. Pelo mesmo
motivo o [[BFF]] continua em Java — se a RAM apertar, a saída é GraalVM native image,
não trocar de linguagem. Ver [[Escalabilidade]] e [[Remoção do Camunda]].

### Sem cache na descoberta por bbox
A área muda a cada gesto de mapa **e** por usuário. O *hit rate* seria quase zero e
o custo de memória alto. Ver [[BFF]].

## Domínio

### A pasta nunca vaza a aventura
Coleção `PUBLICA` não torna públicas as aventuras dentro dela — cada uma respeita a
própria visibilidade. Organização não é permissão. Ver
[[Visibilidade e Controle de Acesso]].

### Amizade exige seguimento mútuo
Seguir é a porta de entrada; amizade é o passo seguinte. Mas **deixar de seguir não
desfaz** a amizade.

### Bloquear apaga os follows dos dois lados
Bloqueio corta o vínculo social, não só esconde.

### Sessão ao vivo nasce `PRIVADO`
O padrão mais restritivo. Localização em tempo real só é exposta por escolha
explícita.

### Ao sair da aventura, só os próprios dados vão junto
O "escolher de quem manter os registros" foi **adiado**. Quem sai leva o que
registrou (ou descarta); o que os outros registraram permanece na aventura em grupo.

### Timestamps de negócio não são automatizados
`@CreatedDate`/`@LastModifiedDate` cuidam de `criado_em`/`atualizado_em`. Mas
`Path.startedAt/finishedAt`, `Media.capturedAt`, `Friendship.requestedAt` e
`AdventureParticipant.joinedAt` continuam manuais — são eventos do domínio, não
auditoria.

## Convenções de código

- **DTO direto no controller**, sem `ResponseEntity`
- **`Objects.isNull`** em vez de `== null`
- **UUID gerado no mapper** (`toEntity`), não no service
- **`@UtilityClass`** nos mappers — por isso o `UserMapper` recebe a senha já
  encodada, em vez de injetar o `PasswordEncoder`. O mapper fica na pasta da área,
  junto da entidade, e não numa pasta `mapper/`
- **`codigoUsuario` é público**; o `id` interno não circula no contrato

## Produto

### Nome exibido "Trisha", identificadores "Trilha"
O rebrand de 23/07/2026 mudou só o texto visível. Renomear pacotes, tabelas e repos
teria custo alto e risco desnecessário. Ver [[Identidade Visual]].

## Revogadas

| Decisão | Quando | Substituída por |
|---|---|---|
| Senha em texto plano | 10/07/2026 | **BCrypt** ([[Segurança]]) |
| `COUNT(*) + 1` para `codigoUsuario` | — | **sequence atômica** |
| Wrapper de exceção no MinIO | 21/07/2026 | propaga o erro real ([[Serviço Mídia]]) |
| `fontFamily: 'serif'` no wordmark | 25/07/2026 | **Yellowtail** embarcada |
| Camunda 8 / Zeebe no cadastro | 01/08/2026 | máquina de estado no banco ([[Remoção do Camunda]]) |
