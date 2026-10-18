---
tags: [decisao, arquitetura]
atualizado: 2026-08-01
---

# Remoção do Camunda

Decisão de **01/08/2026**: o [[Serviço Cadastro]] deixou de usar Camunda 8 / Zeebe.
O fluxo de cadastro passou a ser resolvido no próprio banco, com um `@Scheduled`
cobrando o prazo de confirmação.

## O que motivou a revisão

A pergunta original era de custo: *"microserviço numa VPS não sai mais caro?"* — a
provocação sugeria fundir os 5 serviços em 3 para economizar. A análise mostrou que
a economia dessa fusão seria de ~R$ 8/mês (não muda o tier da VPS, ver
[[Escalabilidade]]), mas revelou um problema maior no caminho: **o Zeebe era o maior
consumidor único de RAM da stack e não estava resolvendo um problema que existia.**

## O que o processo BPMN realmente fazia

```
cadastro criado
  ├─ envia email ──► espera "email-confirmado" ┐
  └───────────────► espera "termos-aceitos"    ┴─► ativa conta

  ⏱ 10 min sem os dois ──► desativa o cadastro
```

Dois booleanos e um timeout. O gateway paralelo do BPMN é um `if`; o boundary timer
`PT10M` é uma varredura periódica; a correlação por `usuarioId` é um `WHERE`.

Os três workers (`envio-email`, `liberacao-conta`, `delecao-dados`) tinham **uma
linha cada**, chamando um service que já existia. O Camunda não executava lógica —
só decidia *quando* chamar.

## Por que saiu

**1. Custo de RAM.** ~1,2 GB para orquestrar isso — mais que o overhead somado de
duas JVMs de serviço.

**2. Ponto único de falha no cadastro.** `RegistrationService` iniciava a instância
do processo com `.join()` e, se falhasse, **apagava a conta recém-criada** para
liberar o e-mail. Zeebe fora do ar = ninguém se cadastra. Escrever compensação
manual para a falha do orquestrador é o sintoma mais claro de ferramenta no lugar
errado — a proposta de valor dele é justamente gerenciar compensação.

**3. Estado fora do Postgres.** As instâncias viviam no volume `zeebe-data`, que não
entrava no backup. Corrompeu o volume, usuários ficam presos em `PENDENTE` sem nada
que os libere.

**4. Síncrono e eventualmente consistente ao mesmo tempo.** Os três pontos de contato
(`register`, `confirmEmail`, `acceptTerms`) bloqueavam a thread HTTP no `.join()`,
mas o efeito (ativar a conta) só acontecia depois, no job worker. Existia uma janela
real em que o usuário recebia `200 OK` ao confirmar o e-mail e **ainda não estava
`ATIVO`** — se tentasse logar em seguida, tomava erro. Pior dos dois mundos: paga a
latência do síncrono e a inconsistência do assíncrono.

**5. Só o engine estava instalado.** Nunca houve Operate, Tasklist ou Optimize no
compose — nem em dev nem em prod. O histórico de instâncias existia mas era
invisível. Zeebe **é** o Camunda 8 (o motor); sem os satélites, não havia nenhuma
das vantagens de observabilidade que justificam a plataforma.

## O que entrou no lugar

| Peça do BPMN | Substituto |
|---|---|
| Gateway paralelo + `liberacao-conta` | `ActivationService.tryActivate()` (era `ConfirmationService` até 17/10/2026) |
| Boundary timer `PT10M` + `delecao-dados` | `RegistrationCleanupJob` (`@Scheduled`) |
| `envio-email` (send task) | `UserRegisteredEvent` + `@Async` no listener |
| Reentrega automática de job | `@Retryable` no `ConfirmationMailer` |

### A ativação é um `UPDATE` condicional

```sql
UPDATE usuario SET status = ATIVO WHERE id = ? AND status = PENDENTE
```

Confirmar e-mail e aceitar termos podem chegar **simultâneos**: o link do e-mail
costuma ser aberto em outro dispositivo, então ordenar as telas no front reduz a
probabilidade mas não elimina a corrida. Um ler-decidir-gravar na aplicação ativaria
duas vezes ou nenhuma. Aqui quem arbitra é o banco — a segunda transação afeta zero
linhas. Mesma lição do `COUNT(*) + 1` (ver [[Decisões Deliberadas]]).

### A ativação virou transacional

Antes ela acontecia fora da transação, num job worker. Agora roda na mesma transação
que registra a confirmação: ou grava as duas coisas, ou nenhuma. **A janela de
"confirmei mas não estou ativo" deixou de existir.**

### O corte de expiração usa `updatedAt`, não `createdAt`

Detalhe que quase virou bug: o reaproveitamento de conta (`User.reopen`, antes
`UserMapper.mapToReactivated`) devolve uma conta
`INATIVO` para `PENDENTE` mas **preserva a data de criação original**. Filtrando por
`createdAt`, todo recadastro de conta expirada seria varrido no primeiro ciclo — o
usuário se recadastraria e perderia a conta em até 60 segundos. O `updatedAt` é
atualizado nos dois caminhos (criação e reaproveitamento).

## O que se perdeu

Sendo honesto, três coisas:

1. **O diagrama BPMN** como documentação visual do fluxo — substituído por este
   documento e pelo Javadoc do `ActivationService`.
2. **Histórico de instâncias** — que, sem Operate, já era inacessível na prática.
3. **Retry automático com backoff** nos jobs — reposto pelo `@Retryable`, mas agora
   é código nosso e não infraestrutura.

## O que NÃO foi decidido junto

A provocação original pedia consolidação de serviços. Isso foi **avaliado e
rejeitado** — ver [[Escalabilidade]]:

- **Os 5 microserviços continuam 5.** Fundir Cadastro+APP faria todo deploy do feed
  derrubar o emissor de JWT.
- **O [[BFF]] continua em Java.** Reescrever em Go economizaria ~300 MB, mas é a
  peça que toda feature atravessa. Se a RAM apertar, a saída é GraalVM native image.
- **Reduzir bancos não economiza nada** — já são databases lógicos numa instância só.

## Consequências práticas

- O requisito de RAM da VPS cai bastante (ver [[Deploy em Produção]])
- `docker-compose.dev.yml` e `compose.yaml` não sobem mais broker: dev ficou mais leve
- O `POST /usuario` deixou de ter dependência dura de infra externa
- Novas configs: `app.cadastro.expiracao-minutos` e `app.cadastro.intervalo-limpeza-ms`

> [!note] Não foi erro ter adotado
> O Camunda entrou porque parecia encaixar no problema. É assim que se aprende onde
> a ferramenta serve e onde não — orquestrador de processo paga o próprio custo
> quando há muitos passos, muitos atores e o fluxo muda sem deploy. Não era o caso
> de "espere dois eventos e tenha um timeout".
