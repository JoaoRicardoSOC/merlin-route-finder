# Perguntas para a mentoria — 24/08/2026

> [!IMPORTANT]
> **Documento de trabalho — remover antes da entrega final.** Este arquivo serve à preparação de uma reunião especifica e não faz parte da documentação do produto. Quando o repositório for enviado ao portal da faculdade, no fim do ano, ele deve ser apagado. Registrado como O-17 em [`observacoes.md`](observacoes.md).

> Preparado para a mentoria com os representantes técnicos da Leroy Merlin.
>
> O critério de seleção foi um só: **perguntas que só eles podem responder e cuja resposta muda o que construímos.** Ficaram de fora as que se resolvem lendo o enunciado ou pesquisando por conta própria — tempo de mentoria é caro demais para isso.
>
> **Como usar.** O bloco em destaque é para ser falado, e já carrega o contexto: quem ouve precisa entender o que construímos e por quê antes de conseguir responder bem. Abaixo dele ficam duas notas que são só para nós — a aposta que fizemos e o que mudaria com a resposta.

---

## Lembrete de como a solução funciona

Vale ter isto na ponta da língua, porque quase toda pergunta se apoia nele:

O cliente chega ao **totem**, monta uma lista de compras — buscando por nome ou conversando com um assistente que entende pedidos abertos, do tipo *"o que preciso para pintar uma parede?"*. O sistema calcula a **ordem mais curta de visita** aos corredores e mostra um **QR Code**.

Ele escaneia com o próprio celular e **abre uma página no navegador — não é aplicativo, não instala nada** — com o mapa e a sequência de paradas. Enquanto caminha, marca o que já pegou. **Se chegar a uma prateleira vazia**, aperta um botão e o sistema sugere na hora um substituto que cumpra a mesma função e esteja perto dali.

---

## As três que não podem faltar

Se só der tempo de três, são estas. Todas nascem de uma decisão que tomamos **conscientemente e sem base para tomar**.

### 1. A rota mais curta é mesmo o que a Leroy quer?

> **"Uma parte central da nossa solução é calcular a ordem mais eficiente de visita aos corredores. Medindo com as coordenadas da planta que vocês compartilharam, conseguimos encurtar o percurso em cerca de 40% comparado à ordem em que o cliente foi adicionando os itens.**
>
> **Só que, ao estudar isso, percebemos uma tensão com o próprio enunciado do desafio. Ele pede para reduzir o tempo não produtivo em loja, e também para aumentar o ticket médio. E a loja física é organizada justamente para o cliente passar por coisas que não veio buscar. Levar alguém direto ao produto resolve o primeiro objetivo e pode trabalhar contra o segundo.**
>
> **Como vocês equilibram isso na prática? Faz sentido a rota ser a mais curta possível, ou existem pontos pelos quais vocês prefeririam que o cliente passasse mesmo custando alguns metros a mais?"**

**Nossa aposta.** Otimizamos só a distância, porque é o que dá para medir sem conhecer a estratégia comercial deles.

**O que muda com a resposta.** Se a exposição importa, a rota deixa de ser "menor caminho" e passa a ser "menor caminho **passando por** determinados pontos". O algoritmo já aceita paradas obrigatórias — usamos isso para inserir o desvio ao banheiro. Seria a mesma mecânica com outra finalidade.

---

### 2. O substituto deve ser escolhido pela função, ou também pela margem?

> **"O momento que consideramos o mais valioso da jornada é quando o cliente chega à prateleira e o produto acabou. Ele aperta um botão avisando, e o sistema sugere outro item na hora.**
>
> **Hoje essa escolha usa dois critérios: cumprir a mesma função do produto que faltou, e estar fisicamente perto para o cliente não ter que atravessar a loja. Deliberadamente não olhamos margem, promoção nem giro de estoque.**
>
> **Mas esse é o instante em que o sistema tem a atenção total de um cliente que já está disposto a aceitar uma alternativa. Para vocês, essa sugestão deveria levar o interesse comercial em conta? E, se sim, até onde dá para ir sem quebrar a confiança do cliente na recomendação?"**

**Nossa aposta.** Que uma sugestão percebida como honesta vale mais no longo prazo do que uma venda a mais agora. É aposta, não certeza.

**O que muda com a resposta.** O critério de ordenação dos candidatos — hoje distância, poderia ser uma combinação com margem ou giro. **A segunda metade da pergunta é a que interessa mais**: onde eles traçam esse limite diz muito sobre a cultura da empresa, e é conteúdo bom para a banca.

---

### 3. A ruptura na prateleira é frequente o bastante para justificar isso?

> **"Todo o fluxo que acabei de descrever parte de uma premissa: a de que o estoque do sistema e o que está de fato na gôndola divergem com alguma frequência. Construímos em cima disso, e inclusive guardamos cada relato num registro, pensando que essa informação teria valor para a operação da loja.**
>
> **Só que não temos como saber se a premissa se sustenta. Na realidade de vocês, com que frequência o cliente chega à prateleira e o produto não está lá, mesmo o sistema dizendo que tem? E existe hoje algum caminho pelo qual esse tipo de divergência é reportado — pelo cliente ou pelo funcionário?"**

**Nossa aposta.** Que a divergência é comum e que ninguém a captura de forma sistemática.

**O que muda com a resposta.** Se for raro, o destaque da apresentação passa para a rota e o assistente, e o tratamento de ruptura vira um detalhe. Se for comum **e não houver canal de reporte hoje**, o registro das rupturas vale tanto quanto a sugestão de substituto — e temos um produto maior do que imaginávamos, com uma segunda tela voltada para a operação.

---

## Viabilidade — para os representantes técnicos

Dois dos eixos que a FIAP citou como avaliação de escopo são *integração com estoque em tempo real* e *identificação de produtos na loja*. Nos dois improvisamos, e admitir isso é o que abre a conversa boa.

### 4. Como um sistema externo enxergaria o estoque de vocês?

> **"Precisamos ser francos sobre um limite do nosso protótipo: não há integração nenhuma com estoque real. O saldo que mostramos vem do nosso próprio banco de dados, com uma massa de demonstração que criamos — não temos acesso a sistema da Leroy, e não seria realista num projeto acadêmico.**
>
> **Mas isso deixa uma pergunta em aberto que vocês podem responder: num cenário de verdade, esse dado viria de onde? É um sistema com integração possível, e com qual defasagem — tempo real, minutos, horas? E o saldo que o cliente veria seria o mesmo número que o vendedor consulta hoje?"**

**O que muda.** Se o dado tem atraso de horas, o discurso sobre confiabilidade muda inteiro — e reforça bastante a pergunta 3, porque a defasagem vira uma **causa** da ruptura percebida, não só um detalhe técnico.

### 5. Existe planta digital das lojas, com a posição dos produtos?

> **"Para calcular a rota, precisamos saber onde cada produto está fisicamente. Usamos a planta que vocês compartilharam no kickoff e posicionamos os produtos à mão, num sistema de coordenadas que criamos — funciona para demonstrar, mas obviamente não escala.**
>
> **Vocês têm essa informação em formato digital? Algum planograma, base de endereçamento, coordenadas por seção? E, mais importante: quando uma loja remaneja uma seção, existe um processo que mantém esse dado atualizado?"**

**O que muda.** É a diferença entre demonstrável e implantável. Se a base existe e é mantida, escala deixa de ser problema. Se não existe, **manter o mapa atualizado é o verdadeiro custo da solução** — e essa é uma descoberta que vale levar para a banca, porque mostra que enxergamos além do protótipo.

### 6. O caminho pelo navegador, sem instalar nada, faz sentido para vocês?

> **"Uma decisão que tomamos cedo e que consideramos um diferencial: o cliente não instala aplicativo nenhum. Ele escaneia o QR Code do totem e o celular abre uma página web comum, já com o mapa e a rota. A ideia é eliminar a barreira de pedir para alguém baixar um app no meio de uma compra — é atrito que costuma matar esse tipo de jornada.**
>
> **Isso conversa com a realidade das lojas de vocês? E como isso se relaciona com o aplicativo da Leroy — faria mais sentido essa experiência viver dentro dele para quem já é usuário, ou o caminho sem instalação atende melhor o cliente eventual?"**

**Nossa aposta.** Que a fricção de instalar app é alta o suficiente para justificar abrir mão dos recursos que só um app nativo teria.

**O que muda.** Se já existe app com base instalada relevante, o totem pode virar um ponto de entrada opcional em vez do início obrigatório. E se não existem totens nas lojas, precisamos saber se o custo de hardware é aceitável ou se a jornada deveria começar no celular do cliente desde o primeiro passo.

---

## Direção e diferencial

### 7. O que vocês considerariam o "pulo do gato"?

> **"O enunciado do desafio pede um diferencial disruptivo, e vocês têm uma visão que nós não temos: já viram muitas propostas e provavelmente já testaram coisas internamente.**
>
> **Do ponto de vista de vocês, o que hoje já é esperado como básico numa solução dessas, e o que realmente chamaria atenção? Preferimos ouvir isso agora do que descobrir na banca."**

**Por que perguntar.** É a pergunta mais barata de fazer e a de maior retorno possível.

### 8. O assistente deve substituir a conversa com o vendedor, ou levar até ele?

> **"Um dos objetivos do desafio é liberar os consultores para vendas de maior valor, e nosso assistente vai nessa direção: ele responde sozinho, e o cliente não precisa procurar ninguém para saber o que comprar.**
>
> **Mas ao construir isso ficamos com uma dúvida. Em projetos realmente complexos, o certo talvez seja o oposto — o sistema reconhecer que a pergunta passou do ponto e encaminhar para um especialista, em vez de arriscar uma resposta. Onde vocês traçam essa linha? Que tipo de dúvida vocês querem que continue chegando a uma pessoa?"**

**O que muda.** Abriria um caminho novo: o assistente detectar complexidade e oferecer chamar um consultor. Ataca a assimetria de informação sem prometer que uma IA substitui conhecimento técnico — o que, dito na banca, é mais maduro do que prometer que substitui.

### 9. O escopo do assistente deveria passar de produtos?

> **"Nosso assistente tem escopo fechado de propósito: responde sobre produtos, materiais e projetos, e recusa educadamente qualquer outro assunto. Foi uma decisão de segurança, para ele não sair falando o que não deve na frente de um cliente.**
>
> **Mas o cliente dentro da loja pergunta outras coisas: corte de madeira, prazo de entrega, serviço de instalação, onde fica o banheiro. Vale abrir para os serviços da loja, ou o foco restrito é mais seguro para uma primeira versão?"**

### 10. Como vocês mediriam se isso deu certo?

> **"Se essa solução estivesse rodando numa loja de verdade daqui a um ano, qual número vocês olhariam para dizer que valeu a pena? Tempo de permanência, itens por atendimento, ticket médio, quantidade de vezes que um cliente precisou chamar um vendedor?**
>
> **Perguntamos porque isso define o que vale a pena o sistema registrar. Hoje já guardamos as rupturas relatadas e o desfecho de cada sessão — se o cliente concluiu a compra ou abandonou o carrinho no meio."**

**O que muda.** Define o que instrumentamos, e dá um argumento de negócio pronto para a banca.

---

## Se sobrar tempo

### 11. O cliente B2B tem uma jornada diferente?

> **"Tratamos duas personas: o consumidor final e o profissional — empreiteiro, pedreiro, arquiteto. Chegamos a tirar o limite de itens do carrinho justamente pensando em quem monta a lista de uma obra inteira.**
>
> **Esse cliente profissional compra na loja do mesmo jeito que o consumidor final, ou a jornada dele é outra? Ele já chega com a lista pronta, tem atendimento dedicado?"**

### 12. A sessão deveria identificar o cliente?

> **"Nossa sessão é anônima: o cliente encosta no totem e começa, sem login, sem cadastro, sem dado pessoal nenhum. Isso simplifica muito e evita questões de privacidade.**
>
> **Vocês gostariam que essa jornada estivesse ligada ao cadastro ou ao programa de fidelidade? E, na experiência de vocês, o cliente aceita se identificar em troca de quê?"**

---

## Para os professores, se houver espaço

Curtas, e de outra natureza:

- A entrega de 13/09 pede o MVP funcionando: **quanto do fluxo precisa estar de ponta a ponta?** No nosso caso o backend está publicado e completo, e o frontend ainda está em construção.
- O vídeo de até 3 minutos da rubrica e o de até 5 minutos da seletiva são entregas separadas — **confirmar se o conteúdo pode ser aproveitado entre os dois**.
- O link do deploy é item obrigatório: **um link de API, sem interface visual, conta?**

---

## Uma nota sobre o que temos para mostrar

Se a conversa abrir espaço, a API está publicada e funcionando contra um banco de dados real. O fluxo mais forte para demonstrar em poucos segundos é o da ruptura: zerar o estoque de um produto e ver o assistente eleger o substituto, com a justificativa escrita em linguagem natural.

**Dois cuidados**, se for mostrar ao vivo: a instância gratuita hiberna e leva mais de dois minutos para acordar, então é preciso **abrir a aplicação antes da reunião começar**; e a resposta do assistente leva cerca de 8 segundos, o que convém avisar antes para não parecer travamento.
