# C4 Model — Arquitetura Atual do Merlin Route Finder

> **Versão:** 2.1 (05/11/2026: backend migrado do Spring Boot para JAX-RS, ver D-89)  
> **Status:** Atualizado para refletir o MVP real em produção (`https://merlin-route-finder.vercel.app`).  
> **Substitui:** O diagrama preliminar da Sprint 1 (`docs/diagramas/C4Model.pdf`), que previa totem físico e transição por handoff JWT.

---

## 1. Por que este documento substitui o C4 da Sprint 1?

Na mentoria técnica de 24/08/2026 com os representantes da **Leroy Merlin**, foram estabelecidas duas diretrizes que simplificaram e tornaram a solução muito mais viável e aderente ao mundo real:

1. **Sem Totem Físico:** O cliente utiliza exclusivamente o próprio smartphone através de um Mobile Web App (PWA leve, sem necessidade de download na loja de aplicativos), acessado via escaneamento de **QR Code afixado nas colunas e corredores** da loja física.
2. **Sem Rota Fixa Calculada (Livre Arbítrio do Cliente):** Em vez de impor um caminho rígido pré-calculado (que prejudicaria as vendas por impulso no circuito da loja), o sistema apresenta no mapa interativo **a posição atual do cliente e a localização exata de cada item da lista**, permitindo que o cliente decida seu próprio percurso.

---

## 2. Nível 1 — Diagrama de Contexto de Sistema (System Context)

O diagrama de contexto ilustra como o **Merlin Route Finder** se posiciona no ecossistema do cliente dentro da loja física e suas integrações com provedores de IA e serviços em nuvem.

```mermaid
C4Context
    title Diagrama de Contexto (C4 - Nível 1) — Merlin Route Finder

    Person(cliente, "Cliente da Loja", "Consumidor final (B2C) ou profissional de obras (B2B) dentro da loja física da Leroy Merlin.")
    
    System(merlin, "Merlin Route Finder", "Plataforma web móvel que guia o cliente até a prateleira correta, sugere substitutos em caso de ruptura e responde dúvidas com IA.")
    
    System_Ext(gemini, "Google Gemini AI", "LLM Generativo (gemini-3.5-flash-lite) responsável pela recomendação semântica e assistente conversacional via Function Calling.")
    System_Ext(oracle, "Oracle Database 19c", "Banco de dados relacional que armazena produtos, atributos, mapa da loja, sessões e telemetria de ruptura.")

    Rel(cliente, merlin, "Acessa via QR Code na coluna da loja, busca produtos, vê o mapa e relata rupturas", "HTTPS / Mobile Browser")
    Rel(merlin, gemini, "Solicita recomendações semânticas de substitutos e diálogo conversacional com grounding de catálogo", "HTTPS / JSON API")
    Rel(merlin, oracle, "Consulta catálogo com busca fuzzy, persiste sessões, histórico e registros de ruptura", "JDBC / SQL (Port 1521)")
```

---

## 3. Nível 2 — Diagrama de Contêineres (Container Diagram)

O diagrama de contêineres detalha as fronteiras de software, tecnologias adotadas, protocolos de comunicação e responsabilidades de cada componente implantado.

```mermaid
graph TB
    subgraph Cliente["Dispositivo Móvel do Cliente (Smartphone)"]
        Browser["Navegador Web / PWA\n(Chrome, Safari, Edge)\nZero instalação"]
    end

    subgraph Nuvem_Vercel["Borda / Frontend (Vercel)"]
        WebApp["Mobile Web App SPA\n[React 19, Vite, Vanilla CSS]\n- Navegação por Abas (BottomNav)\n- Cache Local-First (localStorage)\n- Mapa Vetorial SVG Interativo\n- Fila de Sincronização Offline"]
    end

    subgraph Nuvem_Render["Serviços de Backend (Render)"]
        API["Backend API REST\n[Java 21, JAX-RS: Jersey 4 sobre Grizzly]\n- Pacotes do material FIAP (resource, service, dao, dto, model)\n- Agendador de Expiração de Sessão (TTL 4h)\n- Endpoints REST (/produtos, /sessoes, /mapa, /roteiro, /chat) e /health\n- Validação manual nos DTOs e tratamento de erro em cada Resource"]
    end

    subgraph Provedores_Externos["Serviços em Nuvem & Dados"]
        OracleDB[("Oracle Database 19c\n[FIAP Cloud Server]\n- Catálogo com 111 produtos reais\n- Busca tolerante a erros (UTL_MATCH)\n- Telemetria de Ruptura (TB_REGISTRO_RUPTURA)\n- Histórico de Chat e Sessões")]
        GeminiAPI["Google AI Studio\n[Gemini 3.5 Flash-Lite]\n- Function Calling estruturado\n- Fallback determinístico de proximidade"]
    end

    Browser -->|"1. Escaneia QR da coluna ou digita URL"| WebApp
    WebApp -->|"2. Requisições REST / JSON (HTTPS), repassadas pelo rewrite /api da Vercel"| API
    API -->|"3. Consultas e Telemetria via JDBC (DAO + ConnectionFactory, sem pool)"| OracleDB
    API -->|"4. Chamadas de IA com Tool-Grounding (HTTPS)"| GeminiAPI

    classDef client fill:#E1F5FE,stroke:#0288D1,stroke-width:2px,color:#01579B;
    classDef vercel fill:#E8F5E9,stroke:#388E3C,stroke-width:2px,color:#1B5E20;
    classDef render fill:#FFF3E0,stroke:#F57C00,stroke-width:2px,color:#E65100;
    classDef extern fill:#F3E5F5,stroke:#7B1FA2,stroke-width:2px,color:#4A148C;

    class Browser client;
    class WebApp vercel;
    class API render;
    class OracleDB,GeminiAPI extern;
```

---

## 4. Detalhamento dos Contêineres e Decisões Técnicas

| Contêiner | Tecnologia | Responsabilidade Principal | Decisões Críticas Associadas |
|---|---|---|---|
| **Mobile Web App** | React 19, Vite 8, CSS3 | Interface com o cliente na loja. Fornece catálogo com facetas dinâmicas, mapa SVG interativo com grid unificado (0..100), chat flutuante e lista de compras com suporte *local-first* e fila de sincronização para áreas sem sinal. | • **D-79 / D-80:** Acessibilidade WCAG e suporte a `prefers-reduced-motion`.<br>• **D-86:** Fila offline para envio de coletas pendentes.<br>• **D-81:** Barra inferior permanente com 4 destinos. |
| **Backend REST API** | Java 21, JAX-RS (Jersey 4 + Grizzly), JSON-B, JDBC, ModelMapper | Orquestração das regras de negócio, na estrutura de pacotes do material da FIAP. Processa o ciclo de vida das sessões, gerencia o roteiro de compras, calcula geometria da loja e coordena o motor de sugestão de ruptura. | • **D-89:** Saída do Spring Boot para seguir o material à risca.<br>• **D-24:** TTL de sessão de 4 horas com renovação a cada interação.<br>• **D-39:** Registro de ruptura em transação própria (`Transacao.iniciarNova()`).<br>• **D-91:** Sem CORS: o frontend chama a API pelo rewrite da Vercel. |
| **Banco de Dados** | Oracle Database 19c Enterprise | Persistência transacional com integridade referencial. Executa busca textual tolerante a erros de digitação via pacote nativo `UTL_MATCH`. | • **D-90:** Sem pool: cada transação abre e fecha a própria conexão, ocupando sessões do schema só enquanto executa.<br>• **D-94:** Schema mantido por script em `database/migrations`.<br>• **D-47:** Carga de 111 produtos reais da Leroy Merlin com coordenadas em grid de gôndola. |
| **Inteligência Artificial** | Google Gemini (3.5 Flash-Lite) | Interpretação de linguagem natural no chat consultivo e recomendação de produtos substitutos em caso de ruptura de estoque. | • **D-37:** Uso do modelo *Flash-Lite* para latência de ~1s contra >20s de modelos maiores.<br>• **D-38:** Restrição absoluta de grounding: o modelo escolhe apenas entre candidatos reais com saldo em estoque. Em caso de indisponibilidade ou cota, aciona fallback de proximidade. |

---

## 5. Rastreabilidade com os Requisitos da Avaliação

1. **Implementação da Solução (10 pts):** O contêiner do Web App e da API trabalham de forma desacoplada e resiliente. O cliente nunca fica bloqueado por falha temporária de rede ou indisponibilidade de IA.
2. **Coleta de Dados:** Todas as interações críticas (leitura de QR Code inicial, adição de itens, marcação de coleta física e relato de ruptura em gôndola) são capturadas pela API e persistidas no Oracle Database em esquemas relacionais auditáveis.
3. **Usabilidade (5 pts):** A aplicação web roda em qualquer smartphone sem necessidade de autenticação prévia, instalação ou permissões invasivas, garantindo taxa de conversão máxima no ponto de venda.
