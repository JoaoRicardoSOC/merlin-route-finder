# Diagrama Entidade-Relacionamento (DER) Atualizado — Merlin Route Finder

> **Versão:** 2.1 (12/11/2026: tabelas com o prefixo `TB_CH26_`, ver D-98)  
> **Status:** Mapeamento 1:1 com as tabelas ativas no **Oracle Database 19c**, criadas pelos scripts [`V001__schema_inicial.sql`](../../database/migrations/V001__schema_inicial.sql) e [`V002__prefixo_ch26.sql`](../../database/migrations/V002__prefixo_ch26.sql), nessa ordem (D-94).  
> **Nomes:** `TB_<ID>_<NOME>`, em que o ID identifica a atividade da FIAP dona da tabela: `CH26` = Challenge 2026, FIAP + Leroy Merlin. O schema da FIAP é um por aluno e guarda tabelas de outras atividades; o ID impede que os nomes colidam. Chaves seguem o mesmo padrão: `PK_CH26_...`, `FK_CH26_...`, `UK_CH26_...`.  
> **Substitui:** O PDF preliminar da Sprint 1 (`docs/diagramas/DER.pdf`), que não continha a tabela de telemetria `TB_CH26_REGISTRO_RUPTURA` nem `TB_CH26_PRODUTO_ATRIBUTO`.

---

## 1. Visão Geral do Modelo de Dados

O modelo de dados do **Merlin Route Finder** foi projetado para alta eficiência em consultas geoespaciais e tolerância a falhas na ponta. 

Destaques da modelagem:
1. **Grid Geoespacial Unificado (`TB_CH26_PONTO_MAPA`):** Todas as localizações da loja (gôndolas, placas de QR Code, caixas e sanitários) habitam um grid cartesiano normalizado `(0..100)`, permitindo cálculo de distância euclidiana simples e direto sem conversões de coordenadas.
2. **Telemetria de Ruptura (`TB_CH26_REGISTRO_RUPTURA`):** Registra eventos de gôndola vazia relatados pelos clientes, capturando produto faltante, sugestão de substituto (IA vs proximidade), justificativa e timestamp.
3. **Atributos Dinâmicos Facetados (`TB_CH26_PRODUTO_ATRIBUTO`):** Permite filtros dinâmicos (bitola, voltagem, grão, acabamento) sem colunas esparsas ou nulas em `TB_CH26_PRODUTO`.
4. **Ciclo de Vida de Sessão e Lista (`TB_CH26_SESSAO`, `TB_CH26_LISTA_ROTEIRO`, `TB_CH26_ITEM_ROTEIRO`):** Gerencia sessões móveis com TTL de 4 horas e rastreamento de coletas com carimbo de tempo (`coletado_em`).

---

## 2. Diagrama Entidade-Relacionamento (Mermaid)

```mermaid
erDiagram
    TB_CH26_PONTO_MAPA ||--o{ TB_CH26_PRODUTO : "aloca (1:N)"
    TB_CH26_PONTO_MAPA ||--o{ TB_CH26_SESSAO : "origem_inicial (0:N)"
    
    TB_CH26_PRODUTO ||--o{ TB_CH26_PRODUTO_ATRIBUTO : "possui (1:N)"
    TB_CH26_PRODUTO ||--o{ TB_CH26_ITEM_ROTEIRO : "compoe (1:N)"
    TB_CH26_PRODUTO ||--o{ TB_CH26_REGISTRO_RUPTURA : "relatado_em (1:N)"

    TB_CH26_SESSAO ||--|| TB_CH26_LISTA_ROTEIRO : "possui (1:1)"
    TB_CH26_SESSAO ||--o{ TB_CH26_CHAT_MENSAGEM : "registra (1:N)"
    TB_CH26_SESSAO ||--o{ TB_CH26_REGISTRO_RUPTURA : "origina (1:N)"

    TB_CH26_LISTA_ROTEIRO ||--o{ TB_CH26_ITEM_ROTEIRO : "contem (1:N)"

    TB_CH26_PONTO_MAPA {
        VARCHAR(36) id PK "UUID da coordenada"
        VARCHAR(20) tipo "PRATELEIRA, TOTEM, BANHEIRO, CAIXA"
        VARCHAR(100) corredor "Nome do corredor ou seção"
        VARCHAR(20) codigo_curto UK "Código alfanumérico da placa (ex: TIN-02)"
        INT coordenada_x "Posição X no grid (0..100)"
        INT coordenada_y "Posição Y no grid (0..100)"
    }

    TB_CH26_PRODUTO {
        VARCHAR(36) id PK "UUID do produto"
        VARCHAR(50) sku UK "Código de barra / SKU do catálogo"
        VARCHAR(200) nome "Nome comercial do produto"
        VARCHAR(1000) descricao "Detalhamento e especificações"
        VARCHAR(500) imagem_url "URL CDN da imagem oficial Leroy Merlin"
        DECIMAL(10_2) preco "Preço de venda em BRL"
        INT saldo_estoque "Quantidade física em estoque"
        VARCHAR(36) ponto_mapa_id FK "Referência à gôndola/ponto no mapa"
    }

    TB_CH26_PRODUTO_ATRIBUTO {
        VARCHAR(36) id PK "UUID do atributo"
        VARCHAR(36) produto_id FK "Produto proprietário"
        VARCHAR(30) chave "MARCA, VOLTAGEM, GRAO, BITOLA, etc."
        VARCHAR(100) valor "Valor da especificação técnica"
    }

    TB_CH26_SESSAO {
        VARCHAR(36) id PK "UUID identificador da sessão móvel"
        VARCHAR(20) status "ACTIVE, COMPLETED, EXPIRED, ABANDONED"
        TIMESTAMP criado_em "Data/hora de inicialização"
        TIMESTAMP expiracao_ttl "Momento de expiração por inatividade (4h)"
        VARCHAR(36) ponto_escaneado_id FK "Placa de QR Code lida na entrada/recentralização"
        TIMESTAMP escaneado_em "Data/hora da última leitura de placa"
    }

    TB_CH26_LISTA_ROTEIRO {
        VARCHAR(36) id PK "UUID da lista de compras"
        VARCHAR(36) sessao_id FK "Sessão associada"
    }

    TB_CH26_ITEM_ROTEIRO {
        VARCHAR(36) id PK "UUID do item na lista"
        VARCHAR(36) lista_id FK "Lista de compras proprietária"
        VARCHAR(36) produto_id FK "Produto adicionado"
        BOOLEAN coletado "Flag de confirmação de retirada na prateleira"
        TIMESTAMP coletado_em "Data/hora em que o cliente coletou o item"
    }

    TB_CH26_CHAT_MENSAGEM {
        VARCHAR(36) id PK "UUID da mensagem"
        VARCHAR(36) sessao_id FK "Sessão da conversa"
        VARCHAR(20) remetente "USER ou ASSISTANT"
        CLOB conteudo "Texto da dúvida ou resposta estruturada"
        TIMESTAMP enviado_em "Data/hora de envio da mensagem"
    }

    TB_CH26_REGISTRO_RUPTURA {
        VARCHAR(36) id PK "UUID do evento de ruptura"
        VARCHAR(36) sessao_id FK "Sessão do cliente que relatou"
        VARCHAR(36) item_roteiro_id "ID do item no momento do relato"
        VARCHAR(36) produto_faltante_id FK "Produto indisponível na gôndola"
        VARCHAR(36) produto_sugerido_id "Produto substituto oferecido (nullable)"
        CLOB justificativa "Explicação do modelo de IA ou razão da troca"
        VARCHAR(20) origem "ASSISTANT (IA) ou PROXIMITY (distância)"
        TIMESTAMP registrado_em "Momento do registro da ocorrência"
    }
```

---

## 3. Dicionário das Tabelas de Dados

### 3.1. `TB_CH26_PONTO_MAPA`
- **Finalidade:** Modela a geometria física da loja em sistema de coordenadas relativo (0..100).
- **Regras:** `codigo_curto` é único e indexado para resolver rapidamente entradas por QR Code (`TIN-02`, `FER-01`). Pode ser nulo para pontos internos que não possuem placa física (ex: centro de gôndola).

### 3.2. `TB_CH26_PRODUTO`
- **Finalidade:** Catálogo central de itens comercializados na loja física.
- **Regras:** Todo produto possui obrigatoriamente um `ponto_mapa_id` que define a gôndola onde está exposto. O `saldo_estoque` reflete o inventário sistêmico da loja.

### 3.3. `TB_CH26_PRODUTO_ATRIBUTO`
- **Finalidade:** Suporta facetas de busca dinâmica em arquitetura EAV (Entity-Attribute-Value) controlada.
- **Regras:** Constraint única em `(produto_id, chave)`. Permite que categorias diferentes exponham apenas filtros pertinentes (ex: "Grão" em Tintas, "Voltagem" em Ferramentas).

### 3.4. `TB_CH26_SESSAO`
- **Finalidade:** Rastreamento do ciclo de vida da jornada do cliente no navegador móvel.
- **Regras:** O TTL padrão é de 4 horas com renovação a cada interação. A sessão pode ser iniciada mesmo com código de placa desconhecido (`ponto_escaneado_id = null`), permitindo navegação sem "você está aqui".

### 3.5. `TB_CH26_LISTA_ROTEIRO` e `TB_CH26_ITEM_ROTEIRO`
- **Finalidade:** Carrinho de compras indoor sem limite fixo de produtos.
- **Regras:** `TB_CH26_ITEM_ROTEIRO` armazena o status de coleta física (`coletado`) e carimbo de data/hora (`coletado_em`), possibilitando calcular a velocidade de coleta e deduzir a posição estimada do cliente.

### 3.6. `TB_CH26_CHAT_MENSAGEM`
- **Finalidade:** Auditoria e memória conversacional do assistente de inteligência artificial.
- **Regras:** Registra todas as mensagens trocadas entre o usuário e o modelo generativo Gemini, mantendo o histórico ordenado por sessão.

### 3.7. `TB_CH26_REGISTRO_RUPTURA` (Telemetria Crítica)
- **Finalidade:** Captura de descompasso de estoque físico vs sistêmico em tempo real na gôndola.
- **Regras:** Persistido em transação isolada (`REQUIRES_NEW`), garantindo que a evidência de ruptura seja gravada no banco mesmo que o cliente desista ou altere o item em seguida. Guarda o motivo da sugestão e a origem (`ASSISTANT` ou `PROXIMITY`).
