-- =====================================================================================
-- Merlin Route Finder - prefixo CH26 nas tabelas, chaves e indices (D-98)
--
-- Executar no SQL Developer (F5, "Executar script"), depois do V001. Os dados sao mantidos:
-- ALTER TABLE ... RENAME so troca o nome, e as chaves estrangeiras acompanham.
--
-- Por que: o schema da FIAP e um por aluno, e o mesmo schema guarda tabelas de outras
-- atividades (TB_USUARIO, TB_DENUNCIA...). Os nomes passam a seguir TB_<ID>_<NOME>, com o ID da
-- atividade: CH26 = Challenge 2026, FIAP + Leroy Merlin.
--
-- As restricoes sao encontradas pela tabela, pelo tipo e pelas colunas, e nao pelo nome. O
-- mesmo script serve a um schema criado pelo V001 e a um criado pelo Hibernate, que deu nomes
-- aleatorios as chaves (FKDX8W5NP9..., UKHTMD1T5..., SYS_C...).
--
-- Pode rodar mais de uma vez: o que ja estiver com o nome novo e deixado como esta.
--
-- As restricoes CHECK continuam sem nome, como no V001: quem as mantem e a RestricaoDeEnum,
-- que as encontra pela condicao.
-- =====================================================================================

DECLARE
    PROCEDURE renomear_tabela(p_de VARCHAR2, p_para VARCHAR2) IS
        v_existe NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_existe FROM user_tables WHERE table_name = p_de;
        IF v_existe = 1 THEN
            EXECUTE IMMEDIATE 'ALTER TABLE ' || p_de || ' RENAME TO ' || p_para;
        END IF;
    END;

    -- p_tipo: 'P' (chave primaria), 'U' (unica) ou 'R' (estrangeira).
    -- p_colunas: as colunas na ordem da restricao, separadas por virgula.
    PROCEDURE renomear_restricao(p_tabela VARCHAR2, p_tipo VARCHAR2, p_colunas VARCHAR2, p_nome VARCHAR2) IS
        v_atual  user_constraints.constraint_name%TYPE;
        v_indice user_constraints.index_name%TYPE;
    BEGIN
        SELECT c.constraint_name, c.index_name
          INTO v_atual, v_indice
          FROM user_constraints c
         WHERE c.table_name = p_tabela
           AND c.constraint_type = p_tipo
           AND (SELECT LISTAGG(cc.column_name, ',') WITHIN GROUP (ORDER BY cc.position)
                  FROM user_cons_columns cc
                 WHERE cc.constraint_name = c.constraint_name) = p_colunas;

        IF v_atual <> p_nome THEN
            EXECUTE IMMEDIATE 'ALTER TABLE ' || p_tabela || ' RENAME CONSTRAINT "' || v_atual || '" TO ' || p_nome;
        END IF;
        -- Chave primaria e unica tem indice proprio, que o Oracle criou com o nome antigo.
        IF v_indice IS NOT NULL AND v_indice <> p_nome THEN
            EXECUTE IMMEDIATE 'ALTER INDEX "' || v_indice || '" RENAME TO ' || p_nome;
        END IF;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20001,
                'Restricao nao encontrada: ' || p_tabela || ' ' || p_tipo || ' (' || p_colunas || ')');
    END;

    PROCEDURE comentar(p_tabela VARCHAR2, p_texto VARCHAR2) IS
    BEGIN
        EXECUTE IMMEDIATE 'COMMENT ON TABLE ' || p_tabela || ' IS ''' || p_texto || '''';
    END;
BEGIN
    -- 1. Tabelas
    renomear_tabela('TB_PONTO_MAPA',       'TB_CH26_PONTO_MAPA');
    renomear_tabela('TB_PRODUTO',          'TB_CH26_PRODUTO');
    renomear_tabela('TB_PRODUTO_ATRIBUTO', 'TB_CH26_PRODUTO_ATRIBUTO');
    renomear_tabela('TB_SESSAO',           'TB_CH26_SESSAO');
    renomear_tabela('TB_LISTA_ROTEIRO',    'TB_CH26_LISTA_ROTEIRO');
    renomear_tabela('TB_ITEM_ROTEIRO',     'TB_CH26_ITEM_ROTEIRO');
    renomear_tabela('TB_CHAT_MENSAGEM',    'TB_CH26_CHAT_MENSAGEM');
    renomear_tabela('TB_REGISTRO_RUPTURA', 'TB_CH26_REGISTRO_RUPTURA');

    -- 2. Chaves primarias
    renomear_restricao('TB_CH26_PONTO_MAPA',       'P', 'ID', 'PK_CH26_PONTO_MAPA');
    renomear_restricao('TB_CH26_PRODUTO',          'P', 'ID', 'PK_CH26_PRODUTO');
    renomear_restricao('TB_CH26_PRODUTO_ATRIBUTO', 'P', 'ID', 'PK_CH26_PRODUTO_ATRIBUTO');
    renomear_restricao('TB_CH26_SESSAO',           'P', 'ID', 'PK_CH26_SESSAO');
    renomear_restricao('TB_CH26_LISTA_ROTEIRO',    'P', 'ID', 'PK_CH26_LISTA_ROTEIRO');
    renomear_restricao('TB_CH26_ITEM_ROTEIRO',     'P', 'ID', 'PK_CH26_ITEM_ROTEIRO');
    renomear_restricao('TB_CH26_CHAT_MENSAGEM',    'P', 'ID', 'PK_CH26_CHAT_MENSAGEM');
    renomear_restricao('TB_CH26_REGISTRO_RUPTURA', 'P', 'ID', 'PK_CH26_REGISTRO_RUPTURA');

    -- 3. Chaves unicas
    renomear_restricao('TB_CH26_PONTO_MAPA',       'U', 'CODIGO_CURTO',     'UK_CH26_PONTO_MAPA_CODIGO_CURTO');
    renomear_restricao('TB_CH26_PRODUTO',          'U', 'SKU',              'UK_CH26_PRODUTO_SKU');
    renomear_restricao('TB_CH26_PRODUTO_ATRIBUTO', 'U', 'PRODUTO_ID,CHAVE', 'UK_CH26_PRODUTO_ATRIBUTO');

    -- 4. Chaves estrangeiras
    renomear_restricao('TB_CH26_PRODUTO',          'R', 'PONTO_MAPA_ID',       'FK_CH26_PRODUTO_PONTO_MAPA');
    renomear_restricao('TB_CH26_PRODUTO_ATRIBUTO', 'R', 'PRODUTO_ID',          'FK_CH26_PRODUTO_ATRIBUTO_PRODUTO');
    renomear_restricao('TB_CH26_SESSAO',           'R', 'PONTO_ESCANEADO_ID',  'FK_CH26_SESSAO_PONTO_ESCANEADO');
    renomear_restricao('TB_CH26_LISTA_ROTEIRO',    'R', 'SESSAO_ID',           'FK_CH26_LISTA_ROTEIRO_SESSAO');
    renomear_restricao('TB_CH26_ITEM_ROTEIRO',     'R', 'LISTA_ID',            'FK_CH26_ITEM_ROTEIRO_LISTA');
    renomear_restricao('TB_CH26_ITEM_ROTEIRO',     'R', 'PRODUTO_ID',          'FK_CH26_ITEM_ROTEIRO_PRODUTO');
    renomear_restricao('TB_CH26_CHAT_MENSAGEM',    'R', 'SESSAO_ID',           'FK_CH26_CHAT_MENSAGEM_SESSAO');
    renomear_restricao('TB_CH26_REGISTRO_RUPTURA', 'R', 'SESSAO_ID',           'FK_CH26_REGISTRO_RUPTURA_SESSAO');
    renomear_restricao('TB_CH26_REGISTRO_RUPTURA', 'R', 'PRODUTO_FALTANTE_ID', 'FK_CH26_REGISTRO_RUPTURA_PRODUTO');

    -- 5. A que atividade cada tabela pertence, visivel no SQL Developer
    comentar('TB_CH26_PONTO_MAPA',       'Challenge 2026 FIAP + Leroy Merlin (Merlin Route Finder): pontos do mapa da loja');
    comentar('TB_CH26_PRODUTO',          'Challenge 2026 FIAP + Leroy Merlin (Merlin Route Finder): catalogo de produtos');
    comentar('TB_CH26_PRODUTO_ATRIBUTO', 'Challenge 2026 FIAP + Leroy Merlin (Merlin Route Finder): caracteristicas dos produtos');
    comentar('TB_CH26_SESSAO',           'Challenge 2026 FIAP + Leroy Merlin (Merlin Route Finder): sessoes de compra');
    comentar('TB_CH26_LISTA_ROTEIRO',    'Challenge 2026 FIAP + Leroy Merlin (Merlin Route Finder): lista de compras da sessao');
    comentar('TB_CH26_ITEM_ROTEIRO',     'Challenge 2026 FIAP + Leroy Merlin (Merlin Route Finder): itens da lista de compras');
    comentar('TB_CH26_CHAT_MENSAGEM',    'Challenge 2026 FIAP + Leroy Merlin (Merlin Route Finder): mensagens do assistente');
    comentar('TB_CH26_REGISTRO_RUPTURA', 'Challenge 2026 FIAP + Leroy Merlin (Merlin Route Finder): relatos de prateleira vazia');
END;
/
