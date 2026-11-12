# Diagramas do Projeto — Índice e Contexto de Evolução

> [!IMPORTANT]
> **Aviso aos Avaliadores (FIAP e Leroy Merlin):**
> Os arquivos em formato `.pdf` presentes nesta pasta foram produzidos durante a **Sprint 1 (1º Semestre)**, antes da mentoria técnica com os especialistas da Leroy Merlin realizada em **24/08/2026**.
> 
> Naquela etapa inicial, o projeto previa totens físicos multimídia e handoff por token JWT com cálculo de rotas Nearest Neighbor. Após a mentoria da Leroy Merlin, o escopo foi refinado para um **Mobile Web App PWA sem totem** e com foco em **sinalização por QR Code e assistência em ruptura**.
> 
> **Os diagramas oficiais e vigentes da Sprint 2 estão disponíveis em formato aberto (Mermaid) nos links abaixo:**

---

## 1. Diagramas Vigentes da Solução Atual (Sprint 2 / MVP)

| Diagrama Oficial Atualizado | Localização no Repositório | Descrição |
|---|---|---|
| **C4 Model Atualizado** (Contexto & Contêiner) | [`../arquitetura/c4-model-atual.md`](../arquitetura/c4-model-atual.md) | Representa a arquitetura real do MVP: Mobile Web App (Vercel), Backend JAX-RS com Jersey (Render), Oracle 19c e Google Gemini 3.5 Flash-Lite. |
| **DER Atualizado** (8 Tabelas em Produção) | [`../dados/der-atualizado.md`](../dados/der-atualizado.md) | Modelo Entidade-Relacionamento completo contendo `TB_CH26_REGISTRO_RUPTURA`, `TB_CH26_CHAT_MENSAGEM`, `TB_CH26_PRODUTO_ATRIBUTO` e as demais 5 tabelas ativas no Oracle. |
| **Arquitetura de Coleta de Dados** | [`../dados/coleta-de-dados-e-telemetria.md`](../dados/coleta-de-dados-e-telemetria.md) | Mapeamento dos fluxos de telemetria em tempo real (ruptura, dúvidas de chat, coletas e jornada indoor). |
| **Sinalização Física de Loja** | [`../arquitetura/placa-sinalizacao-loja.md`](../arquitetura/placa-sinalizacao-loja.md) | Mockup e especificações da placa física de coluna (QR Code, URL curta e Código alfanumérico). |

---

## 2. Acervo Histórico da Sprint 1 (Primeiro Semestre)

Os arquivos abaixo são mantidos no repositório como registro da evolução metodológica da equipe:

- `C4Model.pdf` — *Sprint 1:* Container com Totem físico e Mobile PWA separados por handoff transitivo.
- `DER.pdf` — *Sprint 1:* Modelagem inicial anterior à criação das tabelas de ruptura e atributos dinâmicos.
- `DiagramaCasosDeUso.pdf` — *Sprint 1:* Casos de uso originais (UC-001 a UC-014).
- `DiagramaDeClasses.pdf` — *Sprint 1:* Diagrama estrutural preliminar.
- `DiagramaDeSequencia.pdf` — *Sprint 1:* Sequência do protocolo de handoff de token entre Totem e Celular.
