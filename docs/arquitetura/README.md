# Arquitetura

**Esta pasta é um índice, não um depósito.** A documentação de arquitetura deste projeto
existe e é extensa — ela só não mora aqui, e uma pasta vazia num repositório que vai ser
avaliado sugere trabalho planejado e não feito, que é o contrário do que aconteceu.

| O que você procura | Onde está |
|---|---|
| **As decisões de arquitetura**, uma a uma, com o porquê | [`../decisoes-tecnicas.md`](../decisoes-tecnicas.md) |
| **A arquitetura hexagonal**: camadas, portas e adaptadores | [`../planejamento-tecnico.md`](../planejamento-tecnico.md) |
| **O contrato da API** | [`../../backend/src/main/resources/openapi/openapi.yaml`](../../backend/src/main/resources/openapi/openapi.yaml) |
| **Como o sistema é publicado** (Render e Vercel) | [`../deploy.md`](../deploy.md) |
| **O C4 Model atualizado** (Contexto e Contêiner do MVP) | [`c4-model-atual.md`](c4-model-atual.md) |
| **A especificação da placa física de loja** | [`placa-sinalizacao-loja.md`](placa-sinalizacao-loja.md) |
| **Os diagramas históricos da Sprint 1** (C4, DER, sequência) | [`../diagramas/`](../diagramas/) — ver ressalva pós-mentoria em `O-24` |

## Por que as decisões não viraram um documento de arquitetura

Porque decisão sem motivo envelhece mal. `decisoes-tecnicas.md` guarda cada escolha junto do
problema que a provocou, das alternativas medidas e do que a fez perder — que é o que alguém
precisa para **discordar com fundamento** depois. Um diagrama de caixas diz o que existe; ele
não diz por que a lista do cliente vive na sessão e não no navegador.
