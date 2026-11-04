-- =====================================================================================
-- Merlin Route Finder - schema inicial
--
-- Executar no SQL Developer, como no material, num schema vazio. Ate a migracao para
-- JAX-RS o schema era criado pelo Hibernate (ddl-auto: update); este script foi gerado a
-- partir das entidades JPA e reescrito a mao.
--
-- Comparado com o schema publicado em 04/11/2026 (USER_TAB_COLUMNS e USER_CONSTRAINTS): tabelas,
-- colunas, tipos, nulidade, chaves, UNIQUE e CHECK batem. O publicado tem ainda tres colunas
-- que nenhum codigo usa, sobras do token de handoff (D-44) que o ddl-auto: update nunca apagou:
-- tb_item_roteiro.ordem_caminho, tb_lista_roteiro.handoff_token e
-- tb_lista_roteiro.token_expiracao. Todas aceitam nulo, entao nao atrapalham as gravacoes, e
-- ficam de fora deste script.
--
-- Diferente do material, nao ha SEQUENCE: as chaves sao UUID gerados pela aplicacao e
-- gravados como VARCHAR2(36) (decisao D-11).
--
-- As restricoes CHECK dos enums ficam sem nome de proposito: quem as mantem alinhadas com
-- o codigo e a RestricaoDeEnum, que as encontra pela condicao, nao pelo nome.
-- =====================================================================================

-- Pontos do mapa da loja: prateleiras, caixas, banheiros e placas de QR Code.
CREATE TABLE tb_ponto_mapa (
    id            VARCHAR2(36 CHAR)  NOT NULL,
    tipo          VARCHAR2(20 CHAR)  NOT NULL CHECK (tipo IN ('PRATELEIRA','BANHEIRO','CAIXA','QR_CODE')),
    corredor      VARCHAR2(100 CHAR),
    coordenada_x  NUMBER(10,0)       NOT NULL,
    coordenada_y  NUMBER(10,0)       NOT NULL,
    codigo_curto  VARCHAR2(20 CHAR),
    CONSTRAINT pk_ponto_mapa PRIMARY KEY (id),
    CONSTRAINT uk_ponto_mapa_codigo_curto UNIQUE (codigo_curto)
);

-- Catalogo de produtos, cada um numa prateleira.
CREATE TABLE tb_produto (
    id             VARCHAR2(36 CHAR)   NOT NULL,
    sku            VARCHAR2(50 CHAR)   NOT NULL,
    nome           VARCHAR2(200 CHAR)  NOT NULL,
    descricao      VARCHAR2(1000 CHAR),
    imagem_url     VARCHAR2(500 CHAR),
    preco          NUMBER(10,2)        NOT NULL,
    saldo_estoque  NUMBER(10,0)        NOT NULL,
    ponto_mapa_id  VARCHAR2(36 CHAR)   NOT NULL,
    CONSTRAINT pk_produto PRIMARY KEY (id),
    CONSTRAINT uk_produto_sku UNIQUE (sku),
    CONSTRAINT fk_produto_ponto_mapa FOREIGN KEY (ponto_mapa_id) REFERENCES tb_ponto_mapa (id)
);

-- Caracteristicas do produto (marca, cor, bitola...), no maximo uma de cada chave.
CREATE TABLE tb_produto_atributo (
    id          VARCHAR2(36 CHAR)   NOT NULL,
    produto_id  VARCHAR2(36 CHAR)   NOT NULL,
    chave       VARCHAR2(30 CHAR)   NOT NULL CHECK (chave IN ('MARCA','TIPO','MATERIAL','COR','TEMPERATURA_DE_COR','ACABAMENTO','FORMATO','POTENCIA','AMPERAGEM','POLOS','GRAO','BITOLA','VOLUME','PESO','COMPRIMENTO','LARGURA','DIMENSAO','QUANTIDADE')),
    valor       VARCHAR2(100 CHAR)  NOT NULL,
    CONSTRAINT pk_produto_atributo PRIMARY KEY (id),
    CONSTRAINT uk_produto_atributo UNIQUE (produto_id, chave),
    CONSTRAINT fk_produto_atributo_produto FOREIGN KEY (produto_id) REFERENCES tb_produto (id)
);

-- Sessao de compra de um cliente, aberta ao ler uma placa.
CREATE TABLE tb_sessao (
    id                  VARCHAR2(36 CHAR)  NOT NULL,
    status              VARCHAR2(20 CHAR)  NOT NULL CHECK (status IN ('ACTIVE','COMPLETED','EXPIRED','ABANDONED')),
    criado_em           TIMESTAMP(9)       NOT NULL,
    expiracao_ttl       TIMESTAMP(9)       NOT NULL,
    ponto_escaneado_id  VARCHAR2(36 CHAR),
    escaneado_em        TIMESTAMP(9),
    CONSTRAINT pk_sessao PRIMARY KEY (id),
    CONSTRAINT fk_sessao_ponto_escaneado FOREIGN KEY (ponto_escaneado_id) REFERENCES tb_ponto_mapa (id)
);

-- Lista de compras da sessao.
CREATE TABLE tb_lista_roteiro (
    id         VARCHAR2(36 CHAR)  NOT NULL,
    sessao_id  VARCHAR2(36 CHAR)  NOT NULL,
    CONSTRAINT pk_lista_roteiro PRIMARY KEY (id),
    CONSTRAINT fk_lista_roteiro_sessao FOREIGN KEY (sessao_id) REFERENCES tb_sessao (id)
);

-- Itens da lista de compras.
CREATE TABLE tb_item_roteiro (
    id           VARCHAR2(36 CHAR)  NOT NULL,
    lista_id     VARCHAR2(36 CHAR)  NOT NULL,
    produto_id   VARCHAR2(36 CHAR)  NOT NULL,
    coletado     NUMBER(1,0)        NOT NULL CHECK (coletado IN (0,1)),
    coletado_em  TIMESTAMP(9),
    CONSTRAINT pk_item_roteiro PRIMARY KEY (id),
    CONSTRAINT fk_item_roteiro_lista FOREIGN KEY (lista_id) REFERENCES tb_lista_roteiro (id),
    CONSTRAINT fk_item_roteiro_produto FOREIGN KEY (produto_id) REFERENCES tb_produto (id)
);

-- Historico do chat com o assistente.
CREATE TABLE tb_chat_mensagem (
    id          VARCHAR2(36 CHAR)  NOT NULL,
    sessao_id   VARCHAR2(36 CHAR)  NOT NULL,
    remetente   VARCHAR2(20 CHAR)  NOT NULL CHECK (remetente IN ('USER','ASSISTANT')),
    conteudo    CLOB               NOT NULL,
    enviado_em  TIMESTAMP(9)       NOT NULL,
    CONSTRAINT pk_chat_mensagem PRIMARY KEY (id),
    CONSTRAINT fk_chat_mensagem_sessao FOREIGN KEY (sessao_id) REFERENCES tb_sessao (id)
);

-- Relatos de ruptura de estoque (D-39). item_roteiro_id fica sem chave estrangeira: o
-- cliente pode remover o item do carrinho depois do relato, e o registro precisa sobreviver,
-- porque para a loja a gondola vazia continua sendo informacao. produto_sugerido_id tambem
-- fica sem chave, como no mapeamento JPA.
CREATE TABLE tb_registro_ruptura (
    id                   VARCHAR2(36 CHAR)  NOT NULL,
    sessao_id            VARCHAR2(36 CHAR)  NOT NULL,
    item_roteiro_id      VARCHAR2(36 CHAR)  NOT NULL,
    produto_faltante_id  VARCHAR2(36 CHAR)  NOT NULL,
    produto_sugerido_id  VARCHAR2(36 CHAR),
    origem               VARCHAR2(20 CHAR)  NOT NULL CHECK (origem IN ('ASSISTENTE_IA','PROXIMIDADE','NENHUMA')),
    justificativa        CLOB,
    registrado_em        TIMESTAMP(9)       NOT NULL,
    CONSTRAINT pk_registro_ruptura PRIMARY KEY (id),
    CONSTRAINT fk_registro_ruptura_sessao FOREIGN KEY (sessao_id) REFERENCES tb_sessao (id),
    CONSTRAINT fk_registro_ruptura_produto FOREIGN KEY (produto_faltante_id) REFERENCES tb_produto (id)
);
