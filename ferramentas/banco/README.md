# Manutenção do schema de demonstração

## `limpar-sessoes-de-teste.sql`

Remove as sessões já encerradas — `EXPIRED`, `ABANDONED` e `COMPLETED` — e as linhas
que dependem delas. Ver [O-20](../../docs/observacoes.md).

### Por que existe

`ABANDONED` é a métrica que o projeto usa para dizer que uma venda quase aconteceu
([D-42](../../docs/decisoes-tecnicas.md)). Misturada com resíduo de execução de testes, ela
deixa de significar isso — e é o tipo de número que se cita numa banca.

Medição de 08/09/2026:

| status | sessões |
|---|---|
| `EXPIRED` | 52 |
| `ABANDONED` | 28 |
| `COMPLETED` | 4 |
| `ACTIVE` | 7 |

Das 28 `ABANDONED`, a concentração denuncia a origem: **em 23/08 foram 11 sessões criadas e
as 11 terminaram abandonadas**; em 22/08, 6 de 9. São suítes interrompidas, não clientes.

### Quando rodar

**Na véspera da gravação do vídeo ou da banca, e não antes.** Cada execução da suíte e cada
teste manual criam sessões novas. Limpar com dias de antecedência apenas adia o problema.

### Como rodar

O script tem três partes, na ordem:

1. **Conferência** — só `select`. Mostra o que será apagado e o que fica.
2. **Remoção** — os `delete`, na ordem das chaves estrangeiras.
3. **Conferência depois** — confirma que sobrou apenas `ACTIVE` e que nada ficou órfão.

O `commit` está comentado no fim, de propósito. Rode a Parte 3 antes de confirmar; se algo
estiver errado, `rollback` desfaz.

> [!WARNING]
> **O banco é compartilhado pelas cinco pessoas do time.** A Parte 1 lista as sessões
> `ACTIVE`: se aparecer alguma que não seja sua, alguém está com o app aberto naquele momento.
> O script não as apaga, mas vale avisar antes de mexer no schema.

### O que o script não faz

Não toca em produtos, pontos de mapa nem atributos. A carga inicial os recria a cada subida do
backend, mas apagá-los aqui derrubaria as sessões `ACTIVE` que os referenciam.

### Verificação já feita

As consultas das Partes 1 e 3 foram executadas contra o schema em 08/09/2026, com `rollback`
ao final e nenhum `delete` enviado. Sintaxe validada e números conferidos contra a medição
acima.


---

## `simular-afinidade.py`

Compara, **sem alterar código nem dado**, as ordenações candidatas do substituto de ruptura.
Ver [O-35](../../docs/observacoes.md).

```bash
DB_USER=... DB_PASSWORD=... python ferramentas/banco/simular-afinidade.py
```

Percorre os cinco pares plantados na massa e mostra qual produto cada ordenação elegeria. Foi
com ele que se descobriu, em 08/09, que a solução proposta pela O-35 — contar atributos em comum
— **empata** no caso que a motivou, e portanto não corrige nada.

Rode antes de mexer na cláusula `order by` de `buscarDisponiveisProximosDe`: mudar a ordenação
sem simular é como o defeito nasce.

---

## `medir-substitutos.py`

Mede, **sem alterar código nem dado**, a qualidade dos substitutos que a ruptura ofereceria.
Ver [O-40](../../docs/observacoes.md) e [O-38](../../docs/observacoes.md).

```bash
DB_USER=... DB_PASSWORD=... python ferramentas/banco/medir-substitutos.py
```

Responde quatro perguntas que **a massa determina** e que mudam sozinhas quando alguém
cadastra produto:

1. Quantos produtos ficariam sem candidato nenhum se entrassem em falta.
2. Para quantos o fallback por proximidade ofereceria algo de outra função.
3. Se algum `TIPO` existe em mais de uma seção.
4. Em quantos pares de seção a planta do backend e a traçada discordam sobre o raio de 25.

Medição de 08/09/2026:

| | |
|---|---|
| sem candidato nenhum | 0 de 111 |
| fallback ofereceria outra função | **64 de 111 (58%)** |
| `TIPO` em mais de uma seção | 0 de 82 |
| pares de seção em que as plantas discordam | 9 de 45 |

### Por que o item 3 é o mais importante

É a linha que hoje mantém o item 4 inofensivo. Enquanto nenhum `TIPO` cruzar seção, o melhor
candidato está sempre no próprio corredor e o raio nunca decide nada. **Nada no código garante
isso** — é propriedade da massa. No dia em que essa linha deixar de ser zero, a divergência
entre as duas plantas passa a escolher substituto, e escolher errado.

### Quando rodar

Depois de cadastrar produto novo, e antes de gravar. É rápido e não escreve nada.
