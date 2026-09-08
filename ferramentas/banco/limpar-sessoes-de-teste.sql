-- ---------------------------------------------------------------------------
-- Limpeza das sessoes de teste do schema de demonstracao.  Ver O-20.
--
-- QUANDO RODAR: na vespera da gravacao do video ou da banca, e nao antes.
-- Cada execucao da suite e cada teste manual criam sessoes novas; limpar com
-- dias de antecedencia so adia o problema.
--
-- O QUE APAGA: sessoes ja encerradas -- EXPIRED, ABANDONED e COMPLETED -- e
-- tudo que pende delas.
--
-- O QUE NAO APAGA: sessoes ACTIVE. Alguem do time pode estar com o app aberto
-- neste momento, e apagar a sessao em uso derruba a lista de compras da pessoa
-- sem aviso.
--
-- ORDEM: filhos antes dos pais. Inverter a ordem falha na chave estrangeira.
--
-- PRODUTOS, PONTOS DE MAPA E ATRIBUTOS NAO SAO TOCADOS. A carga inicial os
-- recria a cada subida do backend, mas apaga-los aqui derrubaria as sessoes
-- ACTIVE que os referenciam.
-- ---------------------------------------------------------------------------


-- ===========================================================================
-- PARTE 1 -- CONFERENCIA.  Rode isto primeiro e leia os numeros.
--            Nada aqui altera dado.
-- ===========================================================================

-- Quanto sera apagado, por status
select status, count(*) as sessoes
  from tb_sessao
 where status in ('EXPIRED', 'ABANDONED', 'COMPLETED')
 group by status
 order by count(*) desc;

-- Quanto FICA.  Se aparecer alguma sessao ACTIVE que nao seja sua, avise o time
-- antes de continuar: ela pertence a alguem com o app aberto agora.
select id, status, criado_em, expiracao_ttl
  from tb_sessao
 where status = 'ACTIVE'
 order by criado_em desc;

-- Quantas linhas dependentes serao levadas junto
select 'tb_registro_ruptura' as tabela, count(*) as linhas
  from tb_registro_ruptura
 where sessao_id in (select id from tb_sessao
                      where status in ('EXPIRED', 'ABANDONED', 'COMPLETED'))
union all
select 'tb_chat_mensagem', count(*)
  from tb_chat_mensagem
 where sessao_id in (select id from tb_sessao
                      where status in ('EXPIRED', 'ABANDONED', 'COMPLETED'))
union all
select 'tb_item_roteiro', count(*)
  from tb_item_roteiro
 where lista_id in (select id from tb_lista_roteiro
                     where sessao_id in (select id from tb_sessao
                                          where status in ('EXPIRED', 'ABANDONED', 'COMPLETED')))
union all
select 'tb_lista_roteiro', count(*)
  from tb_lista_roteiro
 where sessao_id in (select id from tb_sessao
                      where status in ('EXPIRED', 'ABANDONED', 'COMPLETED'));


-- ===========================================================================
-- PARTE 2 -- APAGAR.  So execute depois de ler a Parte 1.
--            O COMMIT esta no fim, comentado, de proposito.
-- ===========================================================================

delete from tb_registro_ruptura
 where sessao_id in (select id from tb_sessao
                      where status in ('EXPIRED', 'ABANDONED', 'COMPLETED'));

delete from tb_chat_mensagem
 where sessao_id in (select id from tb_sessao
                      where status in ('EXPIRED', 'ABANDONED', 'COMPLETED'));

delete from tb_item_roteiro
 where lista_id in (select id from tb_lista_roteiro
                     where sessao_id in (select id from tb_sessao
                                          where status in ('EXPIRED', 'ABANDONED', 'COMPLETED')));

delete from tb_lista_roteiro
 where sessao_id in (select id from tb_sessao
                      where status in ('EXPIRED', 'ABANDONED', 'COMPLETED'));

delete from tb_sessao
 where status in ('EXPIRED', 'ABANDONED', 'COMPLETED');


-- ===========================================================================
-- PARTE 3 -- CONFERENCIA DEPOIS.  Rode ANTES do commit.
-- ===========================================================================

-- Deve restar apenas ACTIVE
select status, count(*) as sessoes from tb_sessao group by status;

-- Devem ser zero: nada pode ter ficado apontando para sessao que nao existe
select 'lista orfa' as verificacao, count(*) as qtd
  from tb_lista_roteiro l
 where not exists (select 1 from tb_sessao s where s.id = l.sessao_id)
union all
select 'item orfao', count(*)
  from tb_item_roteiro i
 where not exists (select 1 from tb_lista_roteiro l where l.id = i.lista_id)
union all
select 'chat orfao', count(*)
  from tb_chat_mensagem c
 where not exists (select 1 from tb_sessao s where s.id = c.sessao_id)
union all
select 'ruptura orfa', count(*)
  from tb_registro_ruptura r
 where not exists (select 1 from tb_sessao s where s.id = r.sessao_id);


-- ---------------------------------------------------------------------------
-- Se os numeros da Parte 3 estiverem certos:
--
--   commit;
--
-- Se algo estiver errado, e enquanto nao houver commit:
--
--   rollback;
--
-- O commit fica comentado porque a alternativa e um script que apaga sozinho
-- quando alguem o executa por engano.
-- ---------------------------------------------------------------------------
