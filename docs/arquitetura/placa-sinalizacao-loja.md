# Especificação Técnica da Placa Física de Sinalização de Loja

> **Card do Backlog:** `T-2` / `O-19`, encerrados em 12/11/2026: a placa impressa servia às apresentações ao vivo, que já aconteceram. Esta especificação fica como registro de como a solução funcionaria na loja.  
> **Finalidade:** Comprovar a viabilidade e aplicabilidade física da solução no espaço de 10.000m² da **Leroy Merlin**, demonstrando como os clientes conectam o mundo físico ao Mobile Web App sem barreiras.

---

## 1. O Conceito: Sinalização Ativa de Baixo Custo

A substituição dos totens multimídia (custo unitário de R$ 12.000 a R$ 18.000 + manutenção de telas touchscreen) por **placas inteligentes adesivadas nas colunas da loja** é uma das decisões mais pragmáticas e defensáveis do projeto:

- **Custo Quase Zero:** Impressão em vinil adesivo de alta durabilidade (menos de R$ 15 por placa).
- **Onipresença:** Podem ser instaladas dezenas de placas em cruzamentos, entradas e corredores principais.
- **Resiliência a Falhas:** Funciona mesmo se a câmera do cliente estiver danificada ou sem leitor de QR.

---

## 2. Os Três Elementos Obrigatórios de Cada Placa

Cada placa afixada nas colunas da Leroy Merlin contém obrigatoriamente **três elementos integrados**:

```
+-------------------------------------------------------------+
|                     LEROY MERLIN                            |
|             ENCONTRE TUDO NA LOJA COM SEU CELULAR           |
+-------------------------------------------------------------+
|                                                             |
|   [  QR CODE  ]      1. ESCANEIE O QR CODE AO LADO          |
|   [  DINÂMICO ]         (Abre o mapa da loja instantaneamente)
|   [   AQUI    ]                                             |
|                                                             |
|   -------------------------------------------------------   |
|                                                             |
|   OU ACESSE PELO NAVEGADOR:                                 |
|   🌐 merlin-route-finder.vercel.app                         |
|                                                             |
|   E DIGITE O CÓDIGO DESTA COLUNA:                           |
|   +---------------------------------------+                 |
|   |             T I N - 0 2               |                 |
|   +---------------------------------------+                 |
|            Corredor B01 — Tintas e Vernizes                 |
|                                                             |
+-------------------------------------------------------------+
```

### 1. O QR Code (Plano A — Caminho Principal)
- **Conteúdo codificado:** URL completa com parâmetro de inicialização:  
  `https://merlin-route-finder.vercel.app/?ponto=TIN-02`
- **Experiência do cliente:** O cliente aponta a câmera nativa do smartphone, clica no link e o aplicativo abre direto no catálogo exibindo: *"Você está em: Corredor de Tintas"*. Sem telas intermediárias.

### 2. A URL Curta e Legível (Plano B — Conexão)
- **Texto impresso:** `merlin-route-finder.vercel.app`
- **Por que é essencial:** Se a câmera do cliente estiver quebrada, com lente embaçada ou sem permissão de leitura, ele pode simplesmente digitar o endereço no Chrome/Safari.

### 3. O Código Alfanumérico da Coluna (Plano B — Localização)
- **Formato padronizado:** `SET-NN` (ex: `TIN-02`, `FER-01`, `ELE-03`, `MAT-01`).
- **Design:** Tipografia com peso bold, altura mínima de 4 cm para leitura nítida a 3 metros de distância.
- **Comportamento:** Ao entrar pela URL sem QR, o cliente digita o código no modal de localização e obtém paridade imediata com o Plano A.

---

## 3. Matriz de Códigos de Sinalização da Loja Modelo (Interlagos)

Os códigos mapeados no banco de dados (`TB_CH26_PONTO_MAPA`) correspondem às coordenadas de gôndolas reais:

| Código da Placa | Seção / Corredor | Coordenada X | Coordenada Y | Setor de Referência |
|---|---|:---:|:---:|---|
| **`ENT-01`** | Entrada Principal da Loja | 50 | 92 | Entrada e Recepção |
| **`TIN-01`** | Corredor B01 — Tintas Imobiliárias | 47 | 43 | Tintas e Acessórios |
| **`TIN-02`** | Corredor B02 — Lixas e Solventes | 51 | 47 | Tintas e Acessórios |
| **`FER-01`** | Corredor A11 — Ferramentas Elétricas | 51 | 33 | Ferramentas |
| **`ELE-01`** | Corredor E05 — Iluminação e Lâmpadas | 35 | 34 | Iluminação / Elétrica |
| **`ENC-01`** | Corredor H02 — Tubos, Conexões e Sifões| 33 | 55 | Hidráulica e Encanamento |
| **`MAT-01`** | Corredor C01 — Cimentos e Argamassas | 20 | 70 | Materiais Básicos |
| **`CXA-01`** | Bateria de Caixas e Autoatendimento | 50 | 90 | Caixas / Saída |

---

## 4. Diretrizes de Instalação Física na Loja

1. **Altura dos Olhos:** Centro da placa fixado a **1,50m a 1,60m** do chão, garantindo ângulo ergonômico para apontar a câmera do celular em pé.
2. **Posicionamento Estratégico:**
   - Colunas nas esquinas de cruzamento dos corredores principais.
   - Totem de entrada e balcão de atendimento ao cliente.
   - Início de cada corredor especializado de produtos técnicos (Tintas, Madeiras, Elétrica).
3. **Resistência do Material:** Adesivo em vinil laminado fosco (antirreflexo sob as luminárias de LED industriais da loja), evitando reflexos que atrapalhem o leitor óptico da câmera.

---

## 5. Valor para a Defesa na Banca

Esta documentação prova à banca que:
- O projeto não é uma abstração digital descolada da realidade: **ele resolve o mundo físico**.
- Elimina o investimento em hardware proprietário, tornando a expansão para todas as dezenas de lojas da rede Leroy Merlin rápida, barata e escalável.
