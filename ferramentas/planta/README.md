# Bancadas de traçado da planta

A geometria do mapa da loja não foi gerada por fórmula nem aproximada a olho: foi **decalcada
da planta técnica que a Leroy compartilhou no kickoff**, vértice a vértice. Estas são as
ferramentas que fizeram esse decalque, e elas ficam no repositório por dois motivos: duas seções
ainda não foram traçadas ([O-39](../../docs/observacoes.md)), e a planta original é a fonte
documentada da geometria.

O raciocínio completo — inclusive as três tentativas que falharam antes — está em
[D-89](../../docs/decisoes-tecnicas.md).

## Como abrir

```bash
node ferramentas/planta/servir.mjs
```

Depois, no navegador:

| Endereço | O que faz |
|---|---|
| <http://localhost:5180/__tracar.html> | traça as **seções** — clique em cada canto do departamento |
| <http://localhost:5180/__gondolas.html> | traça as **gôndolas** — arraste de uma ponta à outra da barra |
| <http://localhost:5180/__render.html> | mostra o resultado desenhado, com a conferência |

**Por que um servidor, e não abrir o arquivo direto.** Em `file://` o navegador bloqueia o
`localStorage` — e é nele que as bancadas guardam o desenho enquanto você trabalha. O servidor
do Vite também não serve: a raiz dele é `frontend/`, e estas ferramentas ficam fora de
propósito, para não irem parar dentro do `dist/`. O `servir.mjs` não tem dependência nenhuma,
só o que já vem com o Node.

## O que cada bancada faz de diferente

**`__tracar.html` — seções.** Clique em cada canto; `Enter` ou clicar no primeiro ponto fecha a
forma. Duas ajudas ligadas por padrão, e nenhuma é enfeite:

- **trava em ângulo reto** — parede de galpão é ortogonal, e sem a trava cada lado sai com um ou
  dois graus de inclinação que só aparecem ampliados;
- **grude em canto já desenhado** — é o que faz departamento vizinho *compartilhar* a borda em
  vez de quase encostar. Sem isso sobra fresta entre um e outro, e o mapa vira cartões soltos.

O grude fica **desligado nos dois contornos**, porque a parede do prédio não deve ser puxada
para o canto de um departamento.

**`__gondolas.html` — gôndolas.** Gôndola é uma barra: eixo e espessura. Um arrasto por barra, e
não quatro cliques.

- **roda do mouse amplia** onde o cursor está; botão direito (ou `Espaço`) desloca. Sem isso não
  há precisão: na planta a gôndola tem 3 a 8 pixels.
- **o departamento se reconhece sozinho** pelo lugar onde a barra cai.
- **"Repetir" enche o banco e para na parede** do departamento. Ponha 20 em *Até* e ele coloca
  quantas couberem — no L de Madeiras ele para no encaixe, que é onde a prateleira para de
  verdade. Se o sentido estiver errado, ele avisa em vez de espalhar gôndola fora da loja.
- **barra com a ponta fora da seção sai com contorno âmbar**, porque o meio pode estar dentro e
  a ponta fora — e aí o dado fica errado com aparência correta.

**`__render.html` — conferência.** Lê o traçado do armazenamento do navegador (ou o
`tracado.js`, quando não há nada guardado) e mede: seções sobrepostas, vértice fora do contorno,
centroide fora da própria forma, e quais seções ainda são desenhadas por fórmula.

## Levando o traçado para o app

O botão **Copiar** de cada bancada devolve as coordenadas em unidades da grade 0-100 — a mesma
grade do backend. Elas entram em `frontend/src/services/plantaInterlagos.js`, que é a **fonte de
verdade**. O `tracado.js` daqui é apenas uma cópia, para a conferência funcionar sem depender do
navegador de quem desenhou.
