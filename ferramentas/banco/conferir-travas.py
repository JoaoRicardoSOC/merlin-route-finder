# -*- coding: utf-8 -*-
"""Diz em segundos se o schema esta travado, e por quem. SOMENTE LEITURA.

    DB_USER=... DB_PASSWORD=... python ferramentas/banco/conferir-travas.py

Existe por causa da O-23: uma JVM do Surefire que sobrevive ao Ctrl+C fica
segurando transacao aberta, e a proxima execucao da suite trava esperando --
sem falhar e sem avisar. Em 28/08 isso custou vinte minutos parados, e nao ha
privilegio para derrubar a sessao (`alter system kill session` responde
ORA-01031): so resta encerrar o processo do lado de ca.

QUANDO RODAR: suite parada por mais de dois minutos sem sair do lugar, ou antes
de gravar. Nao escreve nada; pode rodar a qualquer momento.

O QUE NAO E ALARME, e por que este script precisa saber disso:

  A instancia do Render mantem um POOL de conexoes, e conexao de pool em repouso
  fica INACTIVE por projeto. Sao tres ou quatro sessoes INACTIVE o tempo todo, e
  um detector ingenuo apontaria todas como travadas. Elas se distinguem pela
  MAQUINA: o Render se identifica como `srv-...`, maquina de gente nao.

  Por isso o alarme aqui exige INACTIVE + maquina que nao e do Render + ociosa
  ha bastante tempo. Bloqueio de verdade nao depende de heuristica nenhuma:
  `blocking_session` preenchida e fato, e vem primeiro no relatorio.
"""
import io
import os
import platform
import subprocess
import sys

import oracledb

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

# Uma sessao de gente parada mais que isto, sem estar rodando nada, e suspeita.
# Nao e curto de proposito: pausa no depurador ou terminal aberto sao normais.
# Ajustavel por ambiente para o alarme poder ser exercitado sem esperar cinco
# minutos -- deteccao que nunca disparou em teste nao vale como deteccao.
OCIOSA_SUSPEITA_SEG = int(os.environ.get('OCIOSA_SUSPEITA_SEG', '300'))

PREFIXO_RENDER = 'srv-'


def sessoes():
    with oracledb.connect(user=os.environ['DB_USER'], password=os.environ['DB_PASSWORD'],
                          dsn='oracle.fiap.com.br:1521/orcl') as con:
        cur = con.cursor()
        cur.execute("""
            select sid, serial#, status, blocking_session, last_call_et,
                   nvl(program, '?'), nvl(machine, '?'), nvl(osuser, '?')
              from v$session
             where username = user
             order by last_call_et desc
        """)
        return [dict(sid=r[0], serial=r[1], status=r[2], bloqueada_por=r[3],
                     ociosa=r[4], programa=r[5], maquina=r[6], osuser=r[7])
                for r in cur.fetchall()]


def duracao(segundos):
    return '%dmin%02ds' % (segundos // 60, segundos % 60)


def jvms_orfas():
    """Processos java.exe desta maquina, com a linha de comando. So no Windows."""
    if platform.system() != 'Windows':
        return None
    try:
        saida = subprocess.run(
            ['powershell', '-NoProfile', '-Command',
             "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | "
             "Select-Object ProcessId,CreationDate,CommandLine | ConvertTo-Json -Compress"],
            capture_output=True, text=True, timeout=30)
        if saida.returncode != 0 or not saida.stdout.strip():
            return []
        import json
        dados = json.loads(saida.stdout)
        return dados if isinstance(dados, list) else [dados]
    except Exception as erro:                     # noqa: BLE001 - diagnostico nao pode falhar
        print('  (nao consegui listar processos java: %s)' % erro)
        return None


def main():
    lista = sessoes()
    minha = max(lista, key=lambda s: (s['status'] == 'ACTIVE', -s['ociosa']))

    print()
    print('  %-6s %-9s %-10s %-11s %-22s %s'
          % ('SID', 'STATUS', 'BLOQ.POR', 'OCIOSA', 'PROGRAMA', 'MAQUINA'))
    print('  ' + '-' * 84)
    for s in lista:
        marca = '  <- esta consulta' if s is minha else ''
        print('  %-6s %-9s %-10s %-11s %-22s %s%s'
              % (s['sid'], s['status'], s['bloqueada_por'] or '-', duracao(s['ociosa']),
                 s['programa'][:22], s['maquina'][:24], marca))
    print()

    bloqueadas = [s for s in lista if s['bloqueada_por']]
    suspeitas = [s for s in lista
                 if s is not minha
                 and s['status'] == 'INACTIVE'
                 and not s['maquina'].startswith(PREFIXO_RENDER)
                 and s['ociosa'] >= OCIOSA_SUSPEITA_SEG]
    do_render = [s for s in lista if s['maquina'].startswith(PREFIXO_RENDER)]

    if bloqueadas:
        print('  TRAVADO. %d sessao(oes) esperando outra:' % len(bloqueadas))
        for s in bloqueadas:
            print('    SID %s esta bloqueada pela SID %s' % (s['sid'], s['bloqueada_por']))
        print()
        print('  Quem bloqueia costuma ser JVM do Surefire que sobreviveu ao Ctrl+C.')
        print('  Nao ha privilegio para derrubar a sessao: encerre o PROCESSO.')
    elif suspeitas:
        limite = ('%d min' % (OCIOSA_SUSPEITA_SEG // 60) if OCIOSA_SUSPEITA_SEG >= 60
                  else '%d s' % OCIOSA_SUSPEITA_SEG)
        print('  ATENCAO. %d sessao(oes) de maquina de gente, ociosa(s) ha mais de %s:'
              % (len(suspeitas), limite))
        for s in suspeitas:
            print('    SID %-6s %-11s %s (%s)'
                  % (s['sid'], duracao(s['ociosa']), s['maquina'], s['osuser']))
        print()
        print('  Ninguem esta bloqueado AINDA. Se for terminal ou depurador seu, tudo bem;')
        print('  se nao reconhecer, e candidata a virar bloqueio na proxima execucao.')
    else:
        print('  LIVRE. Nenhuma sessao bloqueada e nenhuma ociosa suspeita.')

    if do_render:
        print()
        print('  (%d sessao(oes) do pool do Render, INACTIVE por projeto: %s)'
              % (len(do_render), ', '.join(str(s['sid']) for s in do_render)))

    processos = jvms_orfas()
    if processos:
        surefire = [p for p in processos if 'surefire' in (p.get('CommandLine') or '').lower()]
        print()
        print('  Processos java.exe nesta maquina: %d, sendo %d do Surefire.'
              % (len(processos), len(surefire)))
        for p in surefire:
            print('    PID %s  ->  encerrar com:  taskkill /PID %s /T /F'
                  % (p['ProcessId'], p['ProcessId']))
        if surefire and not (bloqueadas or suspeitas):
            print('    Suite rodando agora? Entao estao certos. Senao, sao orfaos.')
    print()


if __name__ == '__main__':
    main()
