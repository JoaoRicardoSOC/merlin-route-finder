# Merlin Route Finder — Decisões Técnicas

> Registro das decisões técnicas não convencionais tomadas no backend, com o raciocínio por trás de cada uma. Existe para que qualquer integrante do time — ou um avaliador do projeto — entenda **por que** o código está do jeito que está, sem precisar vasculhar arquivo por arquivo.
>
> Complementa [`contexto-e-planejamento.md`](contexto-e-planejamento.md) (o que o projeto é) e [`planejamento-tecnico.md`](planejamento-tecnico.md) (como o trabalho foi organizado).
>
> Cada decisão traz: **Contexto** (o problema), **Decisão** (o que foi feito), **Alternativas** (o que foi descartado e por quê), **Consequências** (o que isso custa) e **Onde no código**.

## Índice

**Arquitetura**
- [D-01. Arquitetura hexagonal com domínio livre de framework](#d-01-arquitetura-hexagonal-com-domínio-livre-de-framework)
- [D-02. Casos de uso devolvem DTO, não entidade de domínio](#d-02-casos-de-uso-devolvem-dto-não-entidade-de-domínio)
- [D-03. Tipo de paginação próprio em vez de `Page` do Spring](#d-03-tipo-de-paginação-próprio-em-vez-de-page-do-spring)
- [D-22. Exceção única para "não encontrado", tratada centralmente](#d-22-exceção-única-para-não-encontrado-tratada-centralmente)
- [D-25. 409 para sessão inativa, e quando é aceitável evoluir o contrato](#d-25-409-para-sessão-inativa-e-quando-é-aceitável-evoluir-o-contrato)

**Domínio**
- [D-04. Entidades imutáveis por padrão](#d-04-entidades-imutáveis-por-padrão)
- [D-05. Construtor privado com fábricas de criação e de reconstrução](#d-05-construtor-privado-com-fábricas-de-criação-e-de-reconstrução)
- [D-06. `encerrar()` desdobrado em três métodos nomeados por evento](#d-06-encerrar-desdobrado-em-três-métodos-nomeados-por-evento)
- [D-07. `ItemRoteiro.reconstituir` para leitura do banco](#d-07-itemroteiroreconstituir-para-leitura-do-banco)
- [D-08. O domínio registra o token de handoff, nunca o assina](#d-08-o-domínio-registra-o-token-de-handoff-nunca-o-assina)
- [D-09. Relação com a sessão é unidirecional](#d-09-relação-com-a-sessão-é-unidirecional)

**Persistência**
- [D-10. Entidades JPA espelho, separadas das de domínio](#d-10-entidades-jpa-espelho-separadas-das-de-domínio)
- [D-11. UUID gravado como `varchar2(36)`, não `raw(16)`](#d-11-uuid-gravado-como-varchar236-não-raw16)
- [D-12. `sessao_id` mapeado duas vezes na mesma entidade](#d-12-sessao_id-mapeado-duas-vezes-na-mesma-entidade)
- [D-13. Cascata e remoção de órfãos no agregado da lista](#d-13-cascata-e-remoção-de-órfãos-no-agregado-da-lista)
- [D-14. Salvar item carrega a entidade gerenciada em vez de remapear](#d-14-salvar-item-carrega-a-entidade-gerenciada-em-vez-de-remapear)
- [D-15. Query nativa com `UTL_MATCH` para busca tolerante a erro de digitação](#d-15-query-nativa-com-utl_match-para-busca-tolerante-a-erro-de-digitação)
- [D-16. Carga inicial em Java em vez de SQL](#d-16-carga-inicial-em-java-em-vez-de-sql)

**Produto e negócio**
- [D-17. Carrinho de roteiro sem limite de itens](#d-17-carrinho-de-roteiro-sem-limite-de-itens)
- [D-18. Produto duplicado é ignorado no carrinho](#d-18-produto-duplicado-é-ignorado-no-carrinho)
- [D-19. Coordenadas do seed seguem a planta real da loja](#d-19-coordenadas-do-seed-seguem-a-planta-real-da-loja)
- [D-24. TTL da sessão é renovado a cada interação](#d-24-ttl-da-sessão-é-renovado-a-cada-interação)
- [D-20. Google Gemini como provedor de LLM](#d-20-google-gemini-como-provedor-de-llm)
- [D-21. Demo da banca por simulação animada, não posicionamento real](#d-21-demo-da-banca-por-simulação-animada-não-posicionamento-real)
- [D-23. "Resolução síncrona de inventário" não é integração com ERP](#d-23-resolução-síncrona-de-inventário-não-é-integração-com-erp)

---

## Arquitetura

### D-01. Arquitetura hexagonal com domínio livre de framework

**Contexto.** O caminho comum em Spring Boot é anotar a entidade de negócio com `@Entity` e usá-la em todas as camadas. É mais rápido de escrever, mas amarra as regras de negócio ao Hibernate: a entidade precisa de construtor vazio, de setters e de campos mutáveis para o framework popular por reflexão — exatamente o oposto do que uma entidade com invariantes deveria permitir.

**Decisão.** Duas hierarquias separadas. As entidades de domínio (`domain/entity`) são POJOs puros, sem nenhuma anotação, com métodos de comportamento e imutabilidade onde faz sentido. A persistência tem classes espelho próprias (`infrastructure/database/entity`), e mapeadores traduzem entre os dois mundos.

O domínio também não conhece Spring: os repositórios são interfaces declaradas em `domain/repository` (ports), implementadas em `infrastructure/database/adapter` (adapters).

**Alternativas.** Entidade única anotada com JPA — descartada porque forçaria abrir mão da imutabilidade e das invariantes protegidas, e faria uma mudança de esquema de banco virar mudança de regra de negócio.

**Consequências.** Mais arquivos e um mapeamento a manter para cada entidade. Em troca, o domínio é testável sem subir contexto Spring nem banco, e mudanças no banco não vazam para as regras.

**Verificação objetiva.** O comando abaixo deve sempre retornar vazio:
```bash
grep -rn "org.springframework\|jakarta.persistence\|org.hibernate" backend/src/main/java/br/com/jence/backend/domain/
```

**Onde no código.** `domain/entity/`, `domain/repository/`, `infrastructure/database/`.

---

### D-02. Casos de uso devolvem DTO, não entidade de domínio

**Contexto.** Um caso de uso poderia devolver a entidade de domínio e deixar o Jackson serializá-la direto na resposta HTTP.

**Decisão.** Cada caso de uso devolve um `record` de `application/dto`, com os campos correspondentes ao schema do contrato OpenAPI. A conversão fica num método estático `de(...)` no próprio DTO.

**Alternativas.** Devolver a entidade — descartada porque acoplaria o JSON público ao modelo interno: renomear um campo do domínio, ou adicionar um getter, mudaria o contrato da API sem ninguém perceber, quebrando o frontend.

**Consequências.** Um DTO a mais por operação. Em troca, o contrato da API é explícito e verificável contra o `openapi.yaml`.

**Onde no código.** `application/dto/SessaoResponse.java`, `ProdutoResponse.java`, `PaginaResponse.java`.

---

### D-03. Tipo de paginação próprio em vez de `Page` do Spring

**Contexto.** A busca paginada de produtos precisa devolver conteúdo mais metadados (página atual, total). O tipo natural em Spring seria `Page<T>`.

**Decisão.** Um `record Pagina<T>` declarado em `domain/repository`. O adaptador traduz o `Page` do Spring para ele, num ponto só.

**Alternativas.** Usar `Page`/`Pageable` do Spring nas interfaces de repositório — descartada porque faria o pacote `domain` importar `org.springframework.data`, contradizendo a D-01. Devolver apenas `List` — descartada porque perderia o total de registros, e o Totem não teria como exibir "página 2 de 3".

**Consequências.** ~15 linhas de código e uma tradução no adaptador.

**Onde no código.** `domain/repository/Pagina.java`, `infrastructure/database/adapter/ProdutoRepositoryAdapter.java`.

---

### D-22. Exceção única para "não encontrado", tratada centralmente

**Contexto.** Vários casos de uso recebem um identificador e podem não encontrar o recurso (produto, sessão, lista, item). O contrato manda responder 404.

**Decisão.** Uma `RecursoNaoEncontradoException(recurso, identificador)` em `domain/exception`, lançada pelo caso de uso, capturada pelo `GlobalExceptionHandler` e convertida em 404 com o `StandardError` padrão do projeto.

**Alternativas.** Cada caso de uso devolver `Optional` e cada controller decidir o status — descartada porque espalharia a mesma decisão por ~8 controllers, e bastaria um esquecer para a API responder 200 com corpo vazio em vez de 404.

Uma classe de exceção por entidade (`ProdutoNaoEncontradoException`, `SessaoNaoEncontradaException`, ...) — descartada porque seis classes que diferem apenas no nome são repetição, não design. A mensagem carrega o contexto: `"Produto nao encontrado(a): <uuid>"`.

**Consequências.** Nenhum controller precisa tratar ausência de recurso: basta o caso de uso lançar. Se no futuro alguma resposta 404 precisar de corpo diferente, é um `@ExceptionHandler` novo, não uma mudança espalhada.

**Onde no código.** `domain/exception/RecursoNaoEncontradoException.java`, `presentation/advice/GlobalExceptionHandler.java`.

---

### D-25. 409 para sessão inativa, e quando é aceitável evoluir o contrato

**Contexto.** Adicionar ou remover produto do roteiro de uma sessão já encerrada ou expirada precisa falhar. Mas 404 seria mentira: a sessão existe, o que impede a operação é o **estado** dela.

**Decisão.** Uma segunda exceção de domínio, `OperacaoNaoPermitidaException`, mapeada para HTTP 409 no `GlobalExceptionHandler`. O contrato OpenAPI ganhou uma resposta reutilizável `SessaoInativa` aplicada aos três endpoints de roteiro.

A separação entre as duas exceções é semântica e vale registrar: **404 = o recurso não existe; 409 = existe, mas o estado atual não permite**. A distinção importa para o frontend, que deve reagir de formas diferentes (404: erro de navegação; 409: sessão precisa ser reiniciada no Totem).

**Política sobre o contrato.** O contrato é fonte de verdade (padrão API-First), mas é um documento vivo. O critério adotado:
- **Adicionar** uma resposta de erro ou um campo opcional é aceitável — não quebra quem já integra.
- **Mudar ou remover** um campo existente exige combinar com a dupla de frontend antes.

Foi por esse critério que no UC-003 optamos por **não** adicionar um campo `disponivel` ao `ProdutoDetalhado` (mudança sem necessidade real), mas aqui adicionamos o 409 (sem ele, o frontend receberia um status não documentado e não saberia tratar).

**Onde no código.** `domain/exception/OperacaoNaoPermitidaException.java`, `presentation/advice/GlobalExceptionHandler.java`, `backend/src/main/resources/openapi/openapi.yaml`.

---

## Domínio

### D-04. Entidades imutáveis por padrão

**Contexto.** Entidades com setters públicos permitem que qualquer parte do código altere o estado, contornando as regras de negócio.

**Decisão.** Campos `final` e apenas `@Getter` por padrão. Mutação só existe onde o negócio exige, e sempre por um método com nome de evento (`marcarComoColetado()`, `definirOrdem()`, `encerrar()`), nunca por setter genérico.

`ChatMensagem` é totalmente imutável — uma mensagem enviada é um fato histórico. Isso também é o que torna o histórico confiável como contexto do motor RAG: nada consegue reescrever o passado da conversa.

**Consequências.** Reconstruir uma entidade a partir do banco exige uma via própria (ver D-05 e D-07).

**Onde no código.** Todo o pacote `domain/entity/`.

---

### D-05. Construtor privado com fábricas de criação e de reconstrução

**Contexto.** Uma entidade nasce de dois jeitos muito diferentes: criada agora pela aplicação (regras de negócio devem valer — sessão nova é sempre `ACTIVE` com TTL a partir de agora), ou reconstruída a partir de uma linha do banco (pode estar `COMPLETED`, com datas antigas).

**Decisão.** Construtor privado e duas fábricas estáticas com nomes que revelam a intenção:
- `Sessao.iniciar(id)` / `ListaRoteiro.criarPara(id, sessaoId)` / `ChatMensagem.doCliente(...)` — criação nova, com as regras aplicadas.
- `reconstituir(...)` — restaura o estado exato vindo do banco.

**Alternativas.** Um construtor público único — descartada porque ou o mapeador não conseguiria restaurar o estado real, ou a criação de uma entidade nova exigiria que o chamador informasse status e datas, que é justamente a regra que a entidade deveria garantir.

**Consequências.** Quem lê o código precisa saber qual fábrica usar; os nomes tornam isso evidente.

**Onde no código.** `domain/entity/Sessao.java`, `ListaRoteiro.java`, `ChatMensagem.java`, `ItemRoteiro.java`.

---

### D-06. `encerrar()` desdobrado em três métodos nomeados por evento

**Contexto.** O diagrama de classes previa um `encerrar()` em `Sessao`. Mas ele tem dois consumidores com desfechos diferentes: a conclusão da rota pelo cliente (deve virar `COMPLETED`) e o job de expiração de TTL (deve virar `EXPIRED` ou `ABANDONED`).

**Decisão.** Três métodos, um por evento de negócio: `encerrar()` → `COMPLETED`, `expirar()` → `EXPIRED`, `abandonar()` → `ABANDONED`. Os três compartilham um guard privado que **só age se a sessão ainda estiver `ACTIVE`**.

**Alternativas.** `encerrar(StatusSessao statusFinal)` — descartada porque permitiria chamadas sem sentido (`encerrar(ACTIVE)`) e esconderia o evento de negócio atrás de um parâmetro.

**Consequências.** O guard evita um problema real: o cliente conclui a rota às 10h00 (`COMPLETED`) e às 10h05 o cron encontra a sessão com TTL vencido. Sem o guard, o cron sobrescreveria para `EXPIRED` e o sistema perderia a informação de que o cliente **completou** a jornada — que é a métrica de sucesso do produto.

**Onde no código.** `domain/entity/Sessao.java`.

---

### D-07. `ItemRoteiro.reconstituir` para leitura do banco

**Contexto.** O construtor público de `ItemRoteiro` cria um item recém-adicionado no Totem: sempre com `ordemCaminho = null` e `coletado = false`, que é o estado correto para um item novo.

**Decisão.** Uma fábrica `reconstituir(id, produto, ordemCaminho, coletado)` para o mapeador de persistência.

**Alternativas.** Usar o construtor público e depois chamar `definirOrdem()` e `marcarComoColetado()` — funciona, mas usaria métodos que representam **eventos de negócio** ("o cliente pegou o produto da prateleira") para restaurar estado, o que confunde quem lê.

**Consequências.** Sem essa fábrica, recarregar uma lista do banco descartaria a rota já calculada e o progresso de coleta do cliente a cada leitura.

**Onde no código.** `domain/entity/ItemRoteiro.java`, `infrastructure/database/factory/ItemRoteiroFactory.java`.

---

### D-08. O domínio registra o token de handoff, nunca o assina

**Contexto.** O DER define `handoff_token` como um JWT assinado. Assinar JWT exige a biblioteca `jjwt` — que é infraestrutura.

**Decisão.** `ListaRoteiro.registrarTokenHandoff(String tokenAssinado)` recebe o token já pronto e apenas o guarda, calculando a expiração (5 minutos). A assinatura acontece na camada de infraestrutura.

Complementos: `isTokenValido()` e `invalidarToken()` — este último é o que viabiliza o requisito de **uso único** do QR Code.

**Alternativas.** Um `gerarTokenHandoff()` que assinasse o JWT dentro da entidade — descartada por importar biblioteca no domínio, violando a D-01. Gerar um token opaco (UUID aleatório) no domínio — descartada por abandonar o JWT previsto no DER e no diagrama de sequência.

**Consequências.** O caso de uso de handoff precisa orquestrar dois passos (assinar na infraestrutura, registrar no domínio) em vez de um.

**Onde no código.** `domain/entity/ListaRoteiro.java`.

---

### D-09. Relação com a sessão é unidirecional

**Contexto.** O diagrama de classes mostra `Sessao` 1-1 `ListaRoteiro` e 1-N `ChatMensagem`. Uma modelagem literal criaria referências nos dois sentidos.

**Decisão.** `ListaRoteiro` e `ChatMensagem` guardam apenas `sessaoId (UUID)`. `Sessao` não conhece nenhum dos dois.

**Alternativas.** Referência bidirecional — descartada porque criaria um ciclo, que gera recursão infinita na serialização JSON e complica o mapeamento JPA, sem benefício prático aqui.

**Consequências.** Para obter a lista de uma sessão usa-se `listaRoteiroRepository.buscarPorSessao(id)` em vez de `sessao.getListaRoteiro()`.

**Onde no código.** `domain/entity/ListaRoteiro.java`, `ChatMensagem.java`.

---

## Persistência

### D-10. Entidades JPA espelho, separadas das de domínio

**Contexto.** Consequência direta da D-01: o Hibernate precisa de construtor sem argumentos, setters e campos mutáveis.

**Decisão.** Classes `*Entity` em `infrastructure/database/entity`, anotadas com JPA e com `@Getter @Setter @NoArgsConstructor @AllArgsConstructor`. Os mapeadores (`*Factory`) traduzem nos dois sentidos.

**Consequências.** Toda entidade nova exige três arquivos (domínio, JPA, mapeador). É o custo assumido da D-01.

**Onde no código.** `infrastructure/database/entity/`, `infrastructure/database/factory/`.

---

### D-11. UUID gravado como `varchar2(36)`, não `raw(16)`

**Contexto.** O padrão do Hibernate para `UUID` no Oracle é `raw(16)` — compacto, porém ilegível: consultar vira `where id = hextoraw('A1B2...')`.

**Decisão.** `@JdbcTypeCode(SqlTypes.VARCHAR)` em todos os ids, resultando em `varchar2(36)`.

**Alternativas.** Manter `raw(16)` — descartada porque tornaria a inspeção manual do banco e a escrita de dados de teste impraticáveis, prejudicando tanto a depuração quanto o trabalho do integrante responsável pelo banco.

**Consequências.** ~20 bytes a mais por id, irrelevante na escala do projeto.

**Onde no código.** Todas as classes de `infrastructure/database/entity/`.

---

### D-12. `sessao_id` mapeado duas vezes na mesma entidade

**Contexto.** Tensão entre a D-09 (domínio guarda só o UUID) e o DER (que especifica chave estrangeira de verdade). Um campo `UUID` simples não faz o Hibernate criar a constraint de FK; uma associação JPA faria, mas obrigaria o mapeador a construir entidades-stub para escrever.

**Decisão.** Dois campos apontando para a mesma coluna:
```java
@Column(name = "sessao_id", nullable = false)
private UUID sessaoId;                       // grava

@ManyToOne(fetch = LAZY)
@JoinColumn(name = "sessao_id", insertable = false, updatable = false)
private SessaoEntity sessao;                 // só existe para gerar a FK
```

**Alternativas.** Só o UUID — descartada por deixar o banco sem integridade referencial e divergente do DER entregue. Só a associação — descartada por exigir que o adaptador passasse referências gerenciadas ao mapeador, aumentando o acoplamento.

**Consequências.** Parece erro para quem lê sem contexto — por isso há comentário explicativo nas duas classes. O `insertable=false, updatable=false` é obrigatório: sem ele, o Hibernate acusa dois campos disputando a mesma coluna.

**Onde no código.** `infrastructure/database/entity/ListaRoteiroEntity.java`, `ChatMensagemEntity.java`.

---

### D-13. Cascata e remoção de órfãos no agregado da lista

**Contexto.** Um `ItemRoteiro` não existe fora de uma `ListaRoteiro`. Quando o cliente remove um produto do carrinho, a linha precisa sumir do banco.

**Decisão.** `@OneToMany(mappedBy = "listaRoteiro", cascade = ALL, orphanRemoval = true)`.

**Consequência não óbvia.** `ItemRoteiroEntity` é o lado proprietário da relação (é ele que carrega a coluna `lista_id`). Por isso o mapeador **precisa criar a entidade da lista antes dos itens** e passá-la a cada um; caso contrário, a FK seria gravada nula e o insert falharia. É o motivo de `ListaRoteiroFactory.paraPersistencia` não usar o construtor cheio como os outros mapeadores.

**Validação.** Comportamento confirmado contra o Oracle real: remover um item do domínio e salvar emite `delete from tb_item_roteiro where id=?`.

**Onde no código.** `infrastructure/database/entity/ListaRoteiroEntity.java`, `factory/ListaRoteiroFactory.java`.

---

### D-14. Salvar item carrega a entidade gerenciada em vez de remapear

**Contexto.** Todos os adaptadores seguem o padrão "mapeia → delega → mapeia de volta". `ItemRoteiroRepositoryAdapter.salvar` é a exceção.

**Decisão.** Ele carrega a entidade existente pelo id, altera apenas `ordemCaminho` e `coletado`, e salva. Se o item não existir, lança exceção explícita.

**Motivo.** Pela D-09, `ItemRoteiro` no domínio **não conhece a lista que o contém**. Remapear um grafo novo gravaria `lista_id` nula e desligaria o item do roteiro — na prática, o cliente perderia aquele produto da rota ao marcá-lo como coletado.

**Consequências.** Este método não serve para criar itens novos; a criação passa por `ListaRoteiro.adicionarProduto()` + salvar a lista, via cascata (D-13). A exceção explícita evita que alguém tente o caminho errado e fique depurando FK nula.

**Onde no código.** `infrastructure/database/adapter/ItemRoteiroRepositoryAdapter.java`.

---

### D-15. Query nativa com `UTL_MATCH` para busca tolerante a erro de digitação

**Contexto.** O UC-002 pede busca "fuzzy". Um `LIKE '%termo%'` cobre busca parcial (`"tint"` acha `"Tinta"`), mas **não** tolera erro de digitação: `"tnta"` retorna vazio.

**Decisão.** Query nativa combinando as duas estratégias, ordenada por similaridade decrescente:
```sql
where upper(p.nome) like upper('%' || :termo || '%')
   or utl_match.jaro_winkler_similarity(upper(p.nome), upper(:termo)) > 70
order by utl_match.jaro_winkler_similarity(upper(p.nome), upper(:termo)) desc, p.nome
```

**Alternativas.** Só `LIKE` — descartada porque o card promete tolerância a erro e não entregaria: se um avaliador digitar errado na demonstração ao vivo, a tela voltaria vazia. Elasticsearch ou similar — descartada por exigir infraestrutura adicional, contra a restrição de custo zero.

**Consequências.** Este é o **único ponto do código amarrado ao Oracle** — `UTL_MATCH` não tem equivalente em JPQL nem em outros bancos. Aceitável porque o banco é fixo neste projeto (instância fornecida pela FIAP), mas é o primeiro lugar a mexer numa eventual troca de SGBD.

O limiar de 70 foi calibrado empiricamente: mais alto perde `"furadera"` → `"Furadeira"`, mais baixo traz ruído. Uma consequência conhecida: `"tnta"` também retorna `"Trena 5m"`, porque as palavras são de fato próximas na métrica — a ordenação por similaridade garante que a Tinta apareça primeiro.

**Validação.** Confirmado contra o Oracle real: `"tint"`, `"tinta"`, `"tnta"` e `"furadera"` retornam os produtos esperados.

**Onde no código.** `infrastructure/database/repository/ProdutoJpaRepository.java`.

---

### D-16. Carga inicial em Java em vez de SQL

**Contexto.** O esqueleto do repositório tem uma pasta `database/seeds/`, sugerindo scripts SQL. Mas o projeto usa `ddl-auto: update` (sem Flyway), e cada integrante tem seu próprio schema Oracle.

**Decisão.** Um `ApplicationRunner` que verifica se o catálogo está vazio e só então popula, usando os ports de domínio.

**Alternativas.** `data.sql` — descartada porque roda a cada inicialização e, para não falhar com chave duplicada no Oracle, exigiria `MERGE INTO`, bem mais verboso que `INSERT`. Script SQL manual em `database/seeds/` — descartada porque exigiria cada integrante rodar à mão em seu schema.

**Consequências.** A massa de dados vive em código Java, não em SQL — menos acessível para quem só mexe com banco. Em troca, é idempotente por natureza e cada integrante popula o próprio schema automaticamente ao subir a aplicação. Desligável com `merlin.seed.enabled=false`.

Efeito colateral positivo: por usar os ports de domínio, o seeder funciona como uma validação de que a arquitetura funciona ponta a ponta em volume.

**Onde no código.** `infrastructure/database/seed/CarregadorDadosIniciais.java`.

---

## Produto e negócio

### D-17. Carrinho de roteiro sem limite de itens

**Contexto.** O material original do projeto previa teto de 15 itens, justificado pela capacidade física de um carrinho de compras.

**Decisão.** Sem limite. `ListaRoteiro` aceita qualquer quantidade.

**Motivo.** O público B2B (empreiteiros, pedreiros, arquitetos) compra listas grandes para uma obra inteira — e é justamente o perfil mais sensível a perda de tempo, que a solução pretende atender. Um teto arbitrário excluiria o caso de uso mais valioso.

**Consequências.** O algoritmo de roteamento e a interface do mapa no celular precisam lidar bem com listas potencialmente longas. Os cards do backlog que falavam em "validação de limites estruturais" e "controle capacitivo" foram reescritos.

**Onde no código.** `domain/entity/ListaRoteiro.java`.

---

### D-18. Produto duplicado é ignorado no carrinho

**Contexto.** Nada impede o cliente de adicionar o mesmo produto duas vezes. O DER não tem campo de quantidade.

**Decisão.** `adicionarProduto` devolve o item já existente em vez de criar outro.

**Alternativas.** Permitir duplicatas — descartada porque geraria duas paradas na mesma coordenada da rota (o cliente andaria até a mesma prateleira duas vezes) e ordens de caminho conflitantes. Adicionar campo `quantidade` — descartada por divergir do DER já entregue e aumentar o escopo.

**Consequências.** Não há como o cliente indicar "quero 2 unidades" pelo roteiro. Aceitável: o roteiro serve para **localizar** produtos, e a quantidade é resolvida na prateleira.

**Onde no código.** `domain/entity/ListaRoteiro.java`.

---

### D-19. Coordenadas do seed seguem a planta real da loja

**Contexto.** A massa de demonstração precisa de coordenadas para cada seção. O caminho fácil seria distribuí-las aleatoriamente.

**Decisão.** As posições seguem aproximadamente a planta real compartilhada pela Leroy Merlin no kickoff — Tintas no topo, Cozinhas e Iluminação no centro-direita, Materiais de construção no canto inferior esquerdo, totem próximo à entrada.

**Motivo.** Com coordenadas sem relação com o layout, a rota "otimizada" pareceria um emaranhado sem lógica quando o frontend desenhasse o mapa — e a demonstração perderia credibilidade exatamente no ponto que ela quer provar.

**Consequências.** As distâncias resultantes são realistas (Tintas → Materiais de construção dá 72 unidades num grid de 100), dando ao algoritmo de roteamento um problema de verdade para otimizar.

**Onde no código.** `infrastructure/database/seed/CarregadorDadosIniciais.java`.

---

### D-24. TTL da sessão é renovado a cada interação

**Contexto.** A sessão nasce com TTL de 30 minutos (card 4). Adicionar e remover itens são as ações que o cliente executa enquanto monta o roteiro no Totem.

**Decisão.** Adicionar ou remover item chama `Sessao.renovarSessao()`, empurrando o TTL para 30 minutos à frente da interação. Consultar a lista **não** renova.

**Motivo — e por que isso é obrigatório, não um extra.** Sem renovação, teríamos uma contradição silenciosa com a [D-17](#d-17-carrinho-de-roteiro-sem-limite-de-itens): removemos o limite de itens do carrinho justamente para atender o empreiteiro que monta uma lista grande para uma obra inteira, mas um TTL fixo de 30 minutos expulsaria exatamente esse cliente no meio da montagem. As duas decisões só funcionam juntas.

**Por que consultar não renova.** Consulta é leitura; renovar exigiria gravar a cada `GET`, e listar a lista não é sinal forte de atividade (uma tela aberta e esquecida continuaria consultando). As ações de escrita são evidência real de que há alguém interagindo.

**Consequências.** Uma sessão só expira após 30 minutos de **inatividade** real, não 30 minutos de duração total. O job de expiração da Fase 3 continua funcionando normalmente: ele varre por `expiracaoTtl` vencido, que agora reflete a última interação.

**Onde no código.** `application/usecase/AdicionarProdutoAoRoteiroUseCase.java`, `RemoverProdutoDoRoteiroUseCase.java`, `domain/entity/Sessao.java`.

---

### D-20. Google Gemini como provedor de LLM

**Contexto.** O projeto usa IA em dois pontos: o assistente conversacional e a sugestão de substituto em caso de ruptura de estoque. Restrição de custo zero (projeto acadêmico).

**Decisão.** Google Gemini, no tier gratuito, com *function calling*.

**Motivo do function calling.** A sugestão de substituto **não** pode ser texto livre do modelo: ele inventaria produtos inexistentes ou fisicamente distantes. Com function calling, o modelo só consegue sugerir chamando uma função que consulta `Produto`/`PontoMapa` reais, filtrados por proximidade e estoque. A restrição fica garantida pela arquitetura, não pela boa vontade do prompt.

**Alternativas.** OpenAI (citada no C4 original) — sem tier gratuito adequado. Groq — também gratuito e mais rápido, mas com qualidade de resposta mais variável.

**Consequências.** A chave de API vem de variável de ambiente (`GEMINI_API_KEY`), nunca versionada.

**Status.** Decidido, ainda não implementado (Fase 2 do planejamento).

---

### D-21. Demo da banca por simulação animada, não posicionamento real

**Contexto.** A banca final (21/09/2026) é 100% online. Não há como caminhar por uma loja real mostrando o celular navegando.

**Decisão.** O Mobile PWA replay a rota já calculada, animando um marcador avançando pelos pontos num mapa simples, sem depender de bússola ou acelerômetro.

**Alternativas.** Antecipar o sistema autoral de posicionamento (bússola + acelerômetro) previsto para o NEXT — descartada por ser P&D não iniciado, com risco alto de não estar confiável no prazo, e por depender de condições físicas impossíveis numa chamada de vídeo.

**Consequências.** A simulação usa as coordenadas reais calculadas pelo algoritmo, então o que se vê na tela é a rota de verdade — não uma animação fabricada. O sistema de posicionamento real fica reservado para o NEXT (24/10/2026), se o grupo for selecionado.

**Dependência.** É o que torna o endpoint de marcação de item coletado (UC-014) parte do caminho crítico: é ele que faz o marcador avançar.

---

### D-23. "Resolução síncrona de inventário" não é integração com ERP

**Contexto.** O card do UC-003 no backlog se chama "Consulta de Detalhamento e **Resolução Síncrona de Inventário**". O nome sugere consulta em tempo real a um sistema de estoque externo.

**Decisão.** Não há integração alguma. O `saldoEstoque` é lido da nossa própria tabela `TB_PRODUTO` no momento da requisição, sem cache. "Síncrono" significa apenas que o valor não é pré-calculado nem defasado em relação ao nosso banco.

**Motivo.** Integração com ERP/WMS está fora do escopo de um projeto acadêmico — não temos acesso a sistema real da Leroy Merlin, e a restrição de custo zero inviabiliza qualquer serviço intermediário.

**Consequências.** É importante ser honesto sobre isso na apresentação: o sistema resolve o problema de **localizar** o produto, mas a confiabilidade do estoque exibido depende do dado no banco estar atualizado. Esse é justamente o risco de negócio levantado no início do projeto — o Merlin poderia reproduzir a mesma "ruptura silenciosa" que promete resolver, se o estoque não refletir a prateleira.

É por isso que o fluxo de ruptura física (UC-013) existe e é tratado como diferencial: ele assume que a divergência **vai** acontecer e converte a falha em oportunidade de venda, em vez de fingir que o estoque é confiável.

**Onde no código.** `application/usecase/ConsultarProdutoUseCase.java`.

---

## Como manter este documento

Toda decisão técnica não convencional deve ser registrada aqui **antes do commit** que a implementa, como parte do fluxo de trabalho do time (planejar → aprovar → implementar → **documentar** → revisar → commitar).

O critério para registrar: *alguém que chegue depois ficaria em dúvida sobre por que isso foi feito assim?* Se sim, documente. Decisões óbvias ou padrão de mercado não precisam entrar.
