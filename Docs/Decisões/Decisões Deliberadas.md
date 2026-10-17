---
tags: [decisao]
atualizado: 2026-08-01
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

### Bancos separados, mesma instância
Cada serviço tem seu banco (isolamento lógico real — ninguém lê a tabela do outro),
mas todos na mesma instância Postgres. Instâncias separadas viriam quando a carga
pedir. Ver [[Escalabilidade]].

### `trilha_web` como repositório separado
Decisão de 25/07/2026: a V1 web sai primeiro, sem arrastar o [[App Mobile]]. O
`lib/` foi copiado, e a duplicação é assumida — mesmo espírito do `GeoUtils`.

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
  encodada, em vez de injetar o `PasswordEncoder`
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
