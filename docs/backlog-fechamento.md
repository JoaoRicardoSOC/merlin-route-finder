# Backlog de fechamento — o que falta para a entrega no portal

> **Para colar no Trello.** Cada card tem título, o que fazer, como verificar que ficou pronto e de quem é.
>
> **Revisado em 12/11/2026.** A seletiva, a banca e o NEXT já passaram. **Só falta a entrega final no portal da faculdade**: o vídeo de até 3 minutos, o PDF e o repositório. Os cards cumpridos até aqui (publicação do frontend e do backend, os onze acabamentos de tela, acessibilidade, diagramas, fotos dos produtos) e os que serviam às apresentações ao vivo (roteiro e gravação do vídeo da seletiva, planta da loja, arte da placa, resposta pronta para a banca) saíram desta lista. O texto completo de cada um continua no histórico do git deste arquivo.
>
> Os prefixos entre colchetes servem para etiqueta no Trello. A ordem dentro de cada bloco é a sugerida.

---

## Placar

| Bloco | Cards | Quem |
|---|---|---|
| Entrega | 1 (E-5) | Time |
| Credenciais e banco | 3 (O-1 a O-3) | João Ricardo |
| Limpeza do repositório | 1 (L-3) | Time |

**Nada aqui bloqueia o app.** O sistema roda de ponta a ponta em produção: entrar pela placa, buscar, montar a lista, ver no mapa, coletar, relatar prateleira vazia, aceitar o substituto e encerrar. O que falta é a entrega em si e a arrumação antes dela.

**Ordem sugerida:** O-1 e O-2 (credenciais) → O-3 (limpar o banco) → E-5 (gravar) → L-3 (limpar o repositório) → enviar ao portal.

*F-12 (lint do frontend) e O-4 (desempate da ruptura) foram concluídos em 12/11/2026.*

---

## [ENTREGA] — vale ponto direto na rubrica

### E-5. O vídeo do portal (até 3 min) e o PDF — a entrega avaliada

**O que fazer.** Duas peças que andam juntas na rubrica:

1. **o vídeo pitch de até 3 minutos**, mostrando o MVP funcionando. O briefing para produzi-lo está em [`prompt-video-apresentacao.md`](prompt-video-apresentacao.md), e o roteiro de demonstração passo a passo, em [`guia-de-avaliacao-mvp.md`](guia-de-avaliacao-mvp.md);
2. **um PDF** com **nomes de todos os integrantes**, **link do vídeo** e **link do deploy** (`https://merlin-route-finder.vercel.app`).

**O que a rubrica paga (5 pontos no total):** 0,5 pela duração dentro de 3 minutos · 0,5 pelo PDF completo · 1,0 por criatividade e edição · **3,0 por clareza na apresentação da solução e na demonstração** — de longe a maior linha, e a que depende do que já está construído.

**Antes de gravar:**

- **acordar a instância** do Render alguns minutos antes: a partida a frio levou 64 s em 12/11/2026;
- **limpar as sessões de teste** (O-3), para qualquer número mostrado ser verdade;
- **ninguém roda a suíte de integração durante a gravação**: o schema é um só para tudo (O-21 em [`observacoes.md`](observacoes.md));
- **a cena da prateleira vazia é a mais forte**, e é a única que mostra a IA decidindo com dados reais. Se algo tiver que ser cortado por tempo, corte a introdução. O assistente pode ficar indisponível; o sistema degrada com uma mensagem honesta, e mostrar isso não é defeito.

**Verificar:** os dois links do PDF abrem de uma máquina que não é a de quem montou; o vídeo fecha abaixo de 3 minutos, cronometrado.

**De quem:** time.

---

## [CREDENCIAIS E BANCO]

### O-1. Trocar a chave do Gemini

**O que fazer.** Gerar uma chave nova no Google AI Studio, pôr no painel do Render (`GEMINI_API_KEY`) e descartar a atual.

**Por quê.** A chave atual passou por canal de conversa. A cota gratuita é de **5 requisições por minuto**, e uma chave conhecida por mais gente é uma cota disputada bem na hora da gravação. Ver `O-01` em [`observacoes.md`](observacoes.md).

**Verificar:** a ruptura da lixa, no app publicado, mostra o selo **"Sugestão do assistente"**, e não o de proximidade.

**De quem:** João Ricardo.

---

### O-2. Trocar a senha do Oracle

**O que fazer.** A senha atual passou por canal de conversa. Trocar no portal da FIAP e atualizar a variável `DB_PASSWORD` no Render **e** o `.env` da raiz do repositório na máquina de desenvolvimento.

**Verificar:** o serviço publicado volta a responder depois da troca, e a suíte de integração (`mvnw test -Pintegracao`) continua rodando localmente.

**De quem:** João Ricardo.

---

### O-3. Limpar as sessões de teste antes de gravar

**O que fazer.** Apagar as sessões criadas por desenvolvimento, pela suíte e pelas conferências em produção, respeitando a ordem das chaves estrangeiras: registros de ruptura, mensagens de chat, itens, lista, sessão.

**Por quê.** Desenvolvimento, testes e demonstração usam **o mesmo schema**. As sessões de teste entram nas métricas de jornada, e `ABANDONED` é justamente o número que o projeto usa para dizer que uma venda quase aconteceu. Ver `O-20` em [`observacoes.md`](observacoes.md).

> **Não é o caso de mexer no StrictMode.** A duplicação de sessão que se vê em desenvolvimento **não acontece em produção**. Ver `O-30` nas observações.

**Verificar:** nenhuma sessão de teste no schema; o app publicado continua abrindo sessão normalmente.

**De quem:** João Ricardo.

---

## [LIMPEZA] — antes de enviar ao portal

### L-3. Remover o briefing do vídeo depois da gravação

**O que fazer.** Depois da gravação, apagar o `prompt-video-apresentacao.md` e tirar os links que apontam para ele (o card E-5 e a `O-17` em [`observacoes.md`](observacoes.md)).

**Já feito em 12/11/2026:** saíram os diários da migração, o protótipo do Stitch, as imagens do template do Vite, as pastas vazias e a configuração do editor. O plano e os requisitos da migração ficam, porque as decisões os citam.

**Verificar:** `grep -rn prompt-video` sem resultado.

**De quem:** time.

---

## O que **não** entra nesta lista, e por quê

| | Por quê |
|---|---|
| **Renomear "roteiro" para "lista"** no código | Atinge entidades, endpoints, contrato, testes e tela, sem mudar nada que o cliente veja. A tela também usa "Roteiro" de ponta a ponta, então o nome é coerente |
| **Guarda contra a sessão duplicada** | Não é defeito: é o StrictMode em desenvolvimento, e não acontece em produção. Silenciá-lo perderia o sinal que ele existe para dar |
| **Raio da ruptura, simulação de estoque sem proteção, estoque sem ERP** | Limitações **aceitas conscientemente**, com o motivo registrado em [`observacoes.md`](observacoes.md). Não são pendências |
