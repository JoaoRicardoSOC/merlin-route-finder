# Casos de uso

**Esta pasta é um índice, não um depósito.** Os casos de uso deste projeto estão escritos —
em três lugares, cada um servindo a um leitor diferente.

| O que você procura | Onde está |
|---|---|
| **A jornada do cliente na loja**, do QR Code ao caixa | [`../fluxo-do-cliente.md`](../fluxo-do-cliente.md) |
| **O que acontece quando a jornada quebra** — rede cai, sessão expira, produto some da prateleira | [`../quebras-de-fluxo.md`](../quebras-de-fluxo.md) |
| **Os casos de uso como código**, um por arquivo | [`../../backend/src/main/java/br/com/jence/backend/application/usecase/`](../../backend/src/main/java/br/com/jence/backend/application/usecase/) |
| **O contrato de cada operação**, com exemplos de requisição e resposta | [`../../backend/src/main/resources/openapi/openapi.yaml`](../../backend/src/main/resources/openapi/openapi.yaml) |

## Onde o caso de uso é mais confiável

Na pasta `usecase` do backend. Cada classe ali **é** um caso de uso — `TratarRupturaEstoque`,
`ConversarComAssistente`, `SubstituirItemDoRoteiro` — e o nome do arquivo é o nome do caso.
Documento de caso de uso pode divergir do sistema sem ninguém notar; essa pasta não pode,
porque é o sistema.

O que vale ler em prosa é a **quebra de fluxo**: o caminho feliz se lê no código, mas o que o
cliente vê quando a rede cai no meio do corredor é decisão de produto, e essa está escrita.
