# -*- coding: utf-8 -*-
"""Mede a qualidade dos substitutos que a ruptura ofereceria. SOMENTE LEITURA.

    DB_USER=... DB_PASSWORD=... python ferramentas/banco/medir-substitutos.py

Nao muda codigo nem dado. Responde quatro perguntas que a massa de demonstracao
determina, e que mudam sozinhas quando alguem acrescenta produto:

1. Quantos produtos ficariam SEM CANDIDATO NENHUM se entrassem em falta.
2. Para quantos o fallback por proximidade ofereceria algo de OUTRA FUNCAO.
3. Se algum TIPO existe em mais de uma secao -- e essa e a propriedade que hoje
   torna inofensiva a divergencia entre as duas plantas da loja (O-38).
4. Em quantos pares de secao as duas plantas discordam sobre "dentro do raio".

O item 2 e o que aparece na tela: a cota gratuita do Gemini e de 5 chamadas por
minuto, entao o fallback e o caminho comum durante uma sessao de testes, e nao a
excecao. Ver O-40.
"""
import collections
import io
import itertools
import math
import os
import re
import sys
import unicodedata

import oracledb

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

AQUI = os.path.dirname(os.path.abspath(__file__))
PLANTA = os.path.normpath(os.path.join(
    AQUI, '..', '..', 'frontend', 'src', 'services', 'plantaInterlagos.js'))

# Os mesmos valores de TratarRupturaEstoqueUseCase. Se mudarem la, mudam aqui.
RAIO = 25.0
LIMITE = 20

# O centro dos blocos de PlantaDaLoja.java. Duplicado aqui de proposito: o objetivo
# da medicao e justamente comparar esta lista com a planta tracada do frontend, e
# le-la do Java exigiria interpretar Java.
BLOCOS_DO_BACKEND = {
    'Tintas': (32, 10), 'Ferragens': (22, 32), 'Eletrica': (34, 30),
    'Encanamento': (48, 30), 'Cozinhas': (62, 30), 'Iluminacao': (76, 32),
    'Jardim': (36, 50), 'Ferramentas': (20, 55), 'Decoracao': (88, 55),
    'Materiais de construcao': (14, 80),
}


def sem_acento(texto):
    return ''.join(c for c in unicodedata.normalize('NFD', texto)
                   if unicodedata.category(c) != 'Mn')


def planta_tracada():
    """Centro de cada secao em plantaInterlagos.js, indexado sem acento."""
    texto = io.open(PLANTA, encoding='utf-8').read()
    centros, referencias = {}, {}
    for m in re.finditer(
            r'nome:\s*"([^"]+)".*?secaoRef:\s*(null|"[^"]*").*?centro:\s*\[([\d.]+),\s*([\d.]+)\]',
            texto, re.S):
        nome = sem_acento(m.group(1))
        centros[nome] = (float(m.group(3)), float(m.group(4)))
        referencias[nome] = None if m.group(2) == 'null' else sem_acento(m.group(2).strip('"'))
    return centros, referencias


def parear(nome, centros, referencias):
    alvo = sem_acento(nome).lower()
    achado = next((k for k in centros if k.lower() == alvo), None)
    if achado is None:
        achado = next((k for k, r in referencias.items() if r and r.lower() == alvo), None)
    return centros.get(achado) if achado else None


def dist(a, b):
    return math.hypot(a[0] - b[0], a[1] - b[1])


def carregar():
    with oracledb.connect(user=os.environ['DB_USER'], password=os.environ['DB_PASSWORD'],
                          dsn='oracle.fiap.com.br:1521/orcl') as con:
        cur = con.cursor()
        cur.execute("""
            select p.sku, p.nome, m.corredor, m.coordenada_x, m.coordenada_y, p.saldo_estoque,
                   (select valor from tb_produto_atributo
                     where produto_id = p.id and chave = 'TIPO'),
                   (select valor from tb_produto_atributo
                     where produto_id = p.id and chave = 'MARCA')
              from tb_produto p
              join tb_ponto_mapa m on m.id = p.ponto_mapa_id
        """)
        return [dict(sku=r[0], nome=r[1], corredor=r[2], x=r[3], y=r[4],
                     saldo=r[5], tipo=r[6], marca=r[7]) for r in cur.fetchall()]


def candidatos_de(alvo, produtos):
    """Replica a consulta de producao: raio, disponibilidade e a ordem do D-68."""
    perto = [p for p in produtos
             if p is not alvo and p['saldo'] > 0
             and dist((alvo['x'], alvo['y']), (p['x'], p['y'])) <= RAIO]
    perto.sort(key=lambda p: (
        0 if p['tipo'] == alvo['tipo'] else 1,
        0 if p['marca'] == alvo['marca'] else 1,
        dist((alvo['x'], alvo['y']), (p['x'], p['y'])),
        p['nome']))
    return perto[:LIMITE]


def main():
    produtos = carregar()
    print('  produtos com ponto no mapa: %d   raio: %.0f   teto de candidatos: %d'
          % (len(produtos), RAIO, LIMITE))
    print()

    # ------------------------------------------------------- 1 e 2: o que a tela mostra
    sem_candidato, mesmo_tipo, outra_funcao = [], [], []
    for alvo in produtos:
        cands = candidatos_de(alvo, produtos)
        if not cands:
            sem_candidato.append(alvo)
        elif cands[0]['tipo'] == alvo['tipo']:
            mesmo_tipo.append((alvo, cands[0]))
        else:
            outra_funcao.append((alvo, cands[0]))

    print('  1. SEM CANDIDATO NENHUM se entrar em falta: %d' % len(sem_candidato))
    for p in sem_candidato:
        print('       %-14s %s' % (p['sku'], p['nome'][:50]))
    print()
    print('  2. O que o fallback por proximidade ofereceria (cota estourada):')
    print('       do mesmo tipo funcional: %3d (%.0f%%)'
          % (len(mesmo_tipo), 100.0 * len(mesmo_tipo) / len(produtos)))
    print('       de OUTRA funcao:         %3d (%.0f%%)'
          % (len(outra_funcao), 100.0 * len(outra_funcao) / len(produtos)))
    if outra_funcao:
        print()
        print('       amostra:')
        for alvo, esc in outra_funcao[:10]:
            print('         %-32s -> %s' % (alvo['nome'][:32], esc['nome'][:32]))
    print()

    # ------------------------------------------------------- 3: TIPO cruzando secao
    por_tipo = collections.defaultdict(set)
    for p in produtos:
        if p['tipo']:
            por_tipo[p['tipo']].add(p['corredor'])
    cruzam = {t: s for t, s in por_tipo.items() if len(s) > 1}
    print('  3. TIPOs presentes em mais de uma secao: %d de %d' % (len(cruzam), len(por_tipo)))
    for t, s in sorted(cruzam.items()):
        print('       %-28s %s' % (t, ', '.join(sorted(s))))
    if not cruzam:
        print('       Nenhum. E esta propriedade -- e nao o codigo -- que hoje impede a')
        print('       divergencia do item 4 de aparecer na tela.')
    print()

    # ------------------------------------------------------- 4: as duas plantas
    centros, referencias = planta_tracada()
    comuns = []
    for nome, centro_backend in BLOCOS_DO_BACKEND.items():
        centro_tracado = parear(nome, centros, referencias)
        if centro_tracado:
            comuns.append((nome, centro_backend, centro_tracado))

    discordam = []
    for (na, ba, fa), (nb, bb, fb) in itertools.combinations(comuns, 2):
        if (dist(ba, bb) <= RAIO) != (dist(fa, fb) <= RAIO):
            discordam.append((na, nb, dist(ba, bb), dist(fa, fb)))

    total = len(comuns) * (len(comuns) - 1) // 2
    print('  4. Pares de secao em que as duas plantas discordam sobre "dentro do raio":'
          ' %d de %d' % (len(discordam), total))
    for na, nb, db, df in sorted(discordam):
        print('       %-24s %-24s  backend %5.1f   tela %5.1f' % (na, nb, db, df))
    if discordam:
        print()
        print('       Todos na mesma direcao: perto na tela, longe no calculo. Ver O-38.')


if __name__ == '__main__':
    main()
