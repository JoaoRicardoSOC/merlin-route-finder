# -*- coding: utf-8 -*-
"""Compara as ordenacoes candidatas do substituto de ruptura. SOMENTE LEITURA.

Nao muda codigo nem dado. Serve para responder a objecao registrada na propria
observacao: "pode reordenar os outros quatro pares, que hoje acertam".
"""
import io
import os
import sys

import oracledb

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

RAIO = 25.0
LIMITE = 20

PARES = [
    ('SKU-TIN-003', 'SKU-TIN-004', 'lixa grao 120 -> lixa d agua 150'),
    ('SKU-ILU-001', 'SKU-ILU-005', 'lampada branca -> outra branca'),
    ('SKU-ENC-004', 'SKU-ENC-005', 'sifao sanfonado -> sifao copo'),
    ('SKU-FER-002', 'SKU-FER-003', 'trena 5m -> trena 7,5m'),
    ('SKU-MAT-001', 'SKU-MAT-003', 'argamassa AC-II -> AC-III'),
]

# Replica a ordenacao de producao e acrescenta o numero de atributos em comum,
# para as duas ordens poderem ser comparadas na mesma consulta.
SQL = """
select p.sku, p.nome,
       case when exists (select 1 from tb_produto_atributo a
                          where a.produto_id = p.id and a.chave = 'TIPO' and a.valor = :tipo)
            then 0 else 1 end as tipo_dif,
       case when exists (select 1 from tb_produto_atributo a
                          where a.produto_id = p.id and a.chave = 'MARCA' and a.valor = :marca)
            then 0 else 1 end as marca_dif,
       sqrt(power(m.coordenada_x - :x, 2) + power(m.coordenada_y - :y, 2)) as dist,
       (select count(*) from tb_produto_atributo a
         join tb_produto_atributo b on b.chave = a.chave and b.valor = a.valor
        where a.produto_id = p.id and b.produto_id = :ref) as comuns,
       -- TIPO e MARCA ja sao as chaves primarias da ordem; QUANTIDADE descreve a
       -- embalagem, nao o produto. Duas lampadas da mesma cor se substituem venham
       -- em caixa de 3 ou de 10.
       (select count(*) from tb_produto_atributo a
         join tb_produto_atributo b on b.chave = a.chave and b.valor = a.valor
        where a.produto_id = p.id and b.produto_id = :ref
          and a.chave not in ('TIPO', 'MARCA', 'QUANTIDADE')) as funcionais
  from tb_produto p
  join tb_ponto_mapa m on m.id = p.ponto_mapa_id
 where p.saldo_estoque > 0
   and p.id <> :excluido
   and sqrt(power(m.coordenada_x - :x, 2) + power(m.coordenada_y - :y, 2)) <= :raio
"""


def main():
    with oracledb.connect(user=os.environ['DB_USER'], password=os.environ['DB_PASSWORD'],
                          dsn='oracle.fiap.com.br:1521/orcl') as con:
        cur = con.cursor()
        print('  %-13s %-26s %-12s %-12s %s' % (
            'em falta', 'par plantado', 'ATUAL', 'CONTA TODOS', 'SO FUNCIONAIS'))
        print('  ' + '-' * 80)
        mudou = []
        for sku, esperado, rotulo in PARES:
            cur.execute("""select p.id, m.coordenada_x, m.coordenada_y,
                                  (select valor from tb_produto_atributo where produto_id = p.id and chave = 'TIPO'),
                                  (select valor from tb_produto_atributo where produto_id = p.id and chave = 'MARCA')
                             from tb_produto p join tb_ponto_mapa m on m.id = p.ponto_mapa_id
                            where p.sku = :s""", s=sku)
            linha = cur.fetchone()
            if not linha:
                print('  %-14s (nao encontrado no schema)' % sku)
                continue
            pid, x, y, tipo, marca = linha

            cur.execute(SQL, tipo=tipo, marca=marca, x=x, y=y, ref=pid, excluido=str(pid), raio=RAIO)
            cands = [dict(sku=r[0], nome=r[1], tipo_dif=r[2], marca_dif=r[3], dist=r[4],
                          comuns=r[5], funcionais=r[6]) for r in cur.fetchall()]

            def primeiro(chave):
                return sorted(cands, key=chave)[0]['sku'] if cands else '(vazio)'

            a = primeiro(lambda c: (c['tipo_dif'], c['marca_dif'], c['dist'], c['nome']))
            p = primeiro(lambda c: (c['tipo_dif'], c['marca_dif'], -c['comuns'], c['dist'], c['nome']))
            f = primeiro(lambda c: (c['tipo_dif'], c['marca_dif'], -c['funcionais'], c['dist'], c['nome']))

            ok = lambda v: ('OK  ' if v == esperado else 'ERRO') + v.replace('SKU-', '')
            print('  %-13s %-26s %-12s %-12s %s' % (sku, rotulo, ok(a), ok(p), ok(f)))
            if len({a, p, f}) > 1:
                mudou.append((sku, a, p, f, esperado))

        print()
        if mudou:
            print('  PARES EM QUE AS ORDENACOES DIVERGEM: %d' % len(mudou))
            for sku, a, p, f, e in mudou:
                print('    %s: atual=%s todos=%s funcionais=%s   (esperado %s)' % (sku, a, p, f, e))
        else:
            print('  As tres ordenacoes elegem o mesmo produto em todos os pares.')


if __name__ == '__main__':
    main()
