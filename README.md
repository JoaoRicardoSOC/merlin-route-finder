# Merlin Route Finder 🧭🛠️

> **FIAP Challenge 2026 — Parceria com Leroy Merlin**  
> *Otimização da Jornada do Cliente em Loja com Inteligência Artificial e Localização Indoor.*

[![Deploy Vercel](https://img.shields.io/badge/Frontend-Vercel-black?logo=vercel&logoColor=white)](https://merlin-route-finder.vercel.app)
[![Deploy Render](https://img.shields.io/badge/Backend-Render-46E3B7?logo=render&logoColor=white)](https://merlin-route-finder-api.onrender.com)
[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Jersey](https://img.shields.io/badge/JAX--RS-Jersey%204.0.3-F8981D)](https://eclipse-ee4j.github.io/jersey/)
[![Oracle Database](https://img.shields.io/badge/Oracle%20DB-19c-F80000?logo=oracle&logoColor=white)](https://www.oracle.com/database/)
[![React 19](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)](https://react.dev/)
[![Google Gemini](https://img.shields.io/badge/AI-Gemini%20Flash--Lite-8E75B2?logo=google&logoColor=white)](https://ai.google.dev/)

---

## 📌 Links Oficiais do MVP em Produção

| Serviço | Link de Acesso | Descrição |
|---|---|---|
| 📱 **Mobile Web App** | [**merlin-route-finder.vercel.app**](https://merlin-route-finder.vercel.app) | Aplicação PWA para smartphone (zero instalação necessária). |
| 📍 **Acesso com Ponto de Loja** | [**merlin-route-finder.vercel.app/?code=TIN-02**](https://merlin-route-finder.vercel.app/?code=TIN-02) | Simula a leitura de QR Code no corredor de Tintas. |
| ⚙️ **Backend REST API** | [**merlin-route-finder-api.onrender.com**](https://merlin-route-finder-api.onrender.com) | API REST em JAX-RS (Jersey sobre Grizzly), com JDBC e Oracle 19c. |
| 📄 **Contrato da API** | [**openapi.yaml**](backend/src/main/resources/openapi/openapi.yaml) | Especificação OpenAPI 3.0 de todos os endpoints. |

> [!NOTE]
> **Aviso sobre o Backend:** Hospedado no plano gratuito do Render, o backend entra em modo de suspensão após períodos de inatividade. A primeira chamada do dia pode levar cerca de 90 a 110 segundos (partida a frio). Uma vez acordado, o tempo de resposta é imediato (~150ms).

---

## 🎯 O Desafio e a Proposta de Valor

Lojas físicas da **Leroy Merlin** possuem até 10.000m² e mais de 30.000 itens técnicos catalogados. Essa escala gera três grandes atritos para o cliente:
1. **O Problema do "Último Metro":** Dificuldade em achar a gôndola exata onde o produto está exposto.
2. **Inércia e Assimetria Técnica:** Clientes que não sabem os nomes técnicos dos produtos para seu projeto.
3. **Frustração com Prateleiras Vazias (Rupturas Físicas):** Chegar ao corredor e descobrir que o produto desejado acabou, levando ao abandono da compra.

### A Solução: Merlin Route Finder
- **Zero Instalação:** O cliente escaneia um QR Code afixado na coluna do corredor (`TIN-02`, `FER-01`) e o sistema abre instantaneamente no navegador web móvel, já sabendo sua localização exata.
- **Busca Tolerante a Falhas:** Pesquisa textual inteligente via `UTL_MATCH` do Oracle (reconhece *"furadera"*, *"tnta"*).
- **Mapa Indoor Interativo:** Planta visual da loja no mesmo grid cartesiano `(0..100)` dos produtos e colunas.
- **Assistente IA Ancorado:** Consulta de dúvidas em linguagem natural via Google Gemini com *Function Calling*, citando apenas produtos e corredores reais da loja.
- **O "Pulo do Gato" — Resolução de Ruptura com IA:** Ao sinalizar gôndola vazia ("Não encontrei este produto"), a IA analisa substitutos equivalentes disponíveis na mesma loja com estoque físico e permite a troca com 1 toque, gravando a telemetria no banco.

---

## 📊 Coleta de Dados e Inteligência Operacional

O sistema não é apenas um visualizador passivo, mas um ativo de **coleta de dados em tempo real** para a gestão da Leroy Merlin:

- **Telemetria de Rupturas (`TB_REGISTRO_RUPTURA`):** Captura ocorrências de falta de produto na gôndola, identificando "estoques fantasmas" no ERP e acionando a reposição física imediata.
- **Mapeamento de Dúvidas Técnicas (`TB_CHAT_MENSAGEM`):** Registra perguntas em linguagem natural para identificar categorias que exigem capacitação dos consultores de loja.
- **Métricas de Jornada e Coleta (`TB_SESSAO` e `TB_ITEM_ROTEIRO`):** Monitora tempo de permanência, taxas de abandono de roteiro e mapas de calor de tráfego de clientes.

Consulte o dossiê completo em: [**Dossiê de Coleta de Dados e Telemetria**](docs/dados/coleta-de-dados-e-telemetria.md).

---

## 🗺️ Mapa da Documentação Técnica

| Assunto | Arquivo |
|---|---|
| 🧪 **Roteiro de Avaliação do MVP** | [**`docs/guia-de-avaliacao-mvp.md`**](docs/guia-de-avaliacao-mvp.md) |
| 🏛️ **C4 Model Atualizado (Contexto e Contêiner)** | [**`docs/arquitetura/c4-model-atual.md`**](docs/arquitetura/c4-model-atual.md) |
| 🗄️ **DER Atualizado (8 Tabelas Oracle 19c)** | [**`docs/dados/der-atualizado.md`**](docs/dados/der-atualizado.md) |
| 📡 **Coleta de Dados e Telemetria** | [**`docs/dados/coleta-de-dados-e-telemetria.md`**](docs/dados/coleta-de-dados-e-telemetria.md) |
| 🏷️ **Placa Física de Sinalização da Loja** | [**`docs/arquitetura/placa-sinalizacao-loja.md`**](docs/arquitetura/placa-sinalizacao-loja.md) |
| 🚶 **Jornada do Cliente (Pós-Mentoria)** | [**`docs/fluxo-do-cliente.md`**](docs/fluxo-do-cliente.md) |
| 💡 **Registro de Decisões Técnicas (ADRs)** | [**`docs/decisoes-tecnicas.md`**](docs/decisoes-tecnicas.md) |
| 🚀 **Instruções de Deploy** | [**`docs/deploy.md`**](docs/deploy.md) |

---

## 🧪 Como Executar o Projeto Localmente

### 1. Pré-requisitos
- Java 21 JDK
- Node.js 20+ e npm
- Maven 3.9+ (ou utilizar o wrapper `./mvnw`)

### 2. Back-end (JAX-RS com Jersey)
```bash
cd backend
# Compilar e rodar os testes que nao precisam de banco
./mvnw clean test

# Executar a aplicação localmente (precisa de DB_URL, DB_USER e DB_PASSWORD)
./mvnw compile exec:java
```

### 3. Front-end (React + Vite)
```bash
cd frontend
# Instalar dependências
npm install

# Rodar em modo de desenvolvimento (porta 5173)
npm run dev

# Gerar build de produção
npm run build
```

---

## 👥 Equipe do Projeto

- **João Ricardo:** Arquitetura e Back-end
- **Caio:** Back-end e Integrações
- **Bielecky:** Back-end, Arquitetura, Front-end e UI/UX Design
- **Marcela:** Front-end e UI/UX Design
- **Vicentini:** Modelagem e Banco de Dados
