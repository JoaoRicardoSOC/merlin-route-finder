# -*- coding: utf-8 -*-
"""Baixa as fontes para dentro do projeto e gera o @font-face local.

    python ferramentas/fontes/gerar.py

Rode isto sempre que acrescentar um icone novo a alguma tela. O motivo esta no
README ao lado: a fonte de icones e um RECORTE, e icone fora do recorte nao desenha
nada -- sem erro nenhum no console.

As escolhas, e o porque de cada uma:

- HANKEN GROTESK VARIAVEL em vez de cinco pesos estaticos. Um arquivo por alfabeto
  cobre 100 a 900, e o CSS do app usa peso 750 e 900 -- que nao estavam entre os cinco
  pedidos ao Google e caiam no vizinho mais proximo. 53 KB contra 265 KB.
- SO latin e latin-ext. O app e em portugues; os recortes cyrillic e vietnamese nunca
  seriam pedidos, e o unicode-range ja impedia o download deles.
- MATERIAL SYMBOLS RECORTADO nos icones que o app de fato usa: ~19 KB contra 1.103 KB
  da fonte variavel completa.
- font-display: block NO ICONE, swap no texto. Com swap o navegador desenha a LIGADURA
  enquanto a fonte nao chegou, e o cliente le "search", "home", "qr_code_scanner"
  escritos na tela. Icone nao tem texto de reserva que preste.
"""
import glob
import io
import os
import re
import urllib.request

AQUI = os.path.dirname(os.path.abspath(__file__))
FRONTEND = os.path.normpath(os.path.join(AQUI, '..', '..', 'frontend'))
DESTINO = os.path.join(FRONTEND, 'public', 'fontes')

UA = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
                    '(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'}

ALFABETOS = {'latin', 'latin-ext'}


def icones_em_uso():
    """Os nomes de icone que o codigo realmente usa.

    Fixar a lista aqui garantiria que ela envelhecesse: alguem acrescenta um icone
    numa tela, esquece de atualizar o script, e o icone nao desenha. Varrendo, o
    recorte acompanha o codigo sozinho.

    O padrao dos dados aceita AS DUAS grafias de aspas. A primeira versao so aceitava
    simples, e plantaInterlagos.js e gerado com duplas: 13 icones de secao ficaram
    fora do recorte e apareceram como palavra escrita no mapa -- "FOREST Madeiras",
    "BOLT Eletrica". O recorte nao avisa quando falta um icone, entao o extrator
    precisa ser abrangente por conta propria.
    """
    padrao_jsx = re.compile(
        r'material-symbols-outlined[^>]*>\s*\{?\s*[\'"]?([a-z0-9_]+)[\'"]?\s*\}?\s*<')
    padrao_dado = re.compile(r'\bicone?:\s*[\'"]([a-z0-9_]+)[\'"]')
    achados = set()
    for caminho in glob.glob(os.path.join(FRONTEND, 'src', '**', '*.js*'), recursive=True):
        texto = io.open(caminho, encoding='utf-8', errors='ignore').read()
        achados |= set(padrao_jsx.findall(texto))
        achados |= set(padrao_dado.findall(texto))
    achados.discard('icon')          # do proprio padrao `icon:`, nao e um icone
    return sorted(achados)


def baixar(url):
    return urllib.request.urlopen(urllib.request.Request(url, headers=UA)).read()


def main():
    os.makedirs(DESTINO, exist_ok=True)
    regras = []

    lista = icones_em_uso()
    print('  icones encontrados no codigo: %d' % len(lista))
    print('  ' + ', '.join(lista))
    print()

    # ---------------------------------------------------------------- texto
    css = baixar('https://fonts.googleapis.com/css2'
                 '?family=Hanken+Grotesk:wght@100..900&display=swap').decode('utf-8')
    for alfabeto, corpo in re.findall(
            r'/\*\s*([a-z0-9-]+)\s*\*/\s*@font-face \{(.*?)\}', css, re.S):
        if alfabeto not in ALFABETOS:
            continue
        url = re.search(r'url\((https://[^)]+)\)', corpo).group(1)
        faixa = re.search(r'unicode-range: ([^;]+);', corpo).group(1).strip()
        nome = 'hanken-grotesk-%s.woff2' % alfabeto
        dados = baixar(url)
        io.open(os.path.join(DESTINO, nome), 'wb').write(dados)
        print('  %-34s %7.1f KB' % (nome, len(dados) / 1024))
        regras.append(
            "/* %s */\n@font-face {\n  font-family: 'Hanken Grotesk';\n"
            "  font-style: normal;\n  font-weight: 100 900;\n  font-display: swap;\n"
            "  src: url('/fontes/%s') format('woff2');\n  unicode-range: %s;\n}"
            % (alfabeto, nome, faixa))

    # ---------------------------------------------------------------- icones
    css = baixar('https://fonts.googleapis.com/css2'
                 '?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@24,100..700,0..1,0'
                 '&icon_names=%s&display=block' % ','.join(lista)).decode('utf-8')
    url = re.search(r'url\((https://[^)]+)\)', css).group(1)
    dados = baixar(url)
    io.open(os.path.join(DESTINO, 'material-symbols.woff2'), 'wb').write(dados)
    print('  %-34s %7.1f KB' % ('material-symbols.woff2', len(dados) / 1024))

    regras.append(
        "/* Material Symbols, recortado nos %d icones que o app usa. A fonte completa\n"
        "   tem 1.103 KB. Icone novo exige rodar ferramentas/fontes/gerar.py de novo. */\n"
        "@font-face {\n  font-family: 'Material Symbols Outlined';\n"
        "  font-style: normal;\n  font-weight: 100 700;\n"
        "  /* `block`, e nao `swap`: com swap o navegador desenha a LIGADURA enquanto a\n"
        "     fonte nao chegou, e o cliente le \"search\" e \"home\" escritos na tela. */\n"
        "  font-display: block;\n  src: url('/fontes/material-symbols.woff2') format('woff2');\n}"
        % len(lista))

    saida = os.path.join(AQUI, 'fontface.css')
    io.open(saida, 'w', encoding='utf-8').write('\n\n'.join(regras) + '\n')

    total = sum(os.path.getsize(os.path.join(DESTINO, f)) for f in os.listdir(DESTINO))
    print()
    print('  total em frontend/public/fontes/: %.1f KB' % (total / 1024))
    print()
    print('  As regras @font-face ficaram em ferramentas/fontes/fontface.css.')
    print('  Elas JA estao no topo de frontend/src/index.css -- so vale copiar de novo')
    print('  se o unicode-range mudar, o que praticamente nao acontece.')


if __name__ == '__main__':
    main()
