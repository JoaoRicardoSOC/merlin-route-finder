import { useEffect, useRef } from 'react'

/**
 * Faz o modal cumprir o que ele já declara.
 *
 * Os oito modais do app sempre trouxeram `role="dialog"` e `aria-modal="true"`. Esse atributo
 * diz à tecnologia assistiva: *ignore o resto da página, ela não existe agora*. O leitor de
 * tela obedece — mas o teclado não obedecia, porque nada prendia o foco. O Tab saía do modal e
 * entrava em botões que o leitor de tela tinha sido instruído a fingir que não estavam lá.
 *
 * Faltar o atributo seria um app sem suporte. Ter o atributo sem a trava é um app que mente
 * sobre o próprio estado — e é pior, porque nenhuma ferramenta automática acusa: a marcação
 * está formalmente correta. Este hook é a metade que faltava.
 *
 * Três coisas, e nessa ordem:
 *
 * 1. **Ao abrir**, move o foco para o container. Ele recebe `tabindex="-1"` aqui mesmo, em vez
 *    de no JSX de cada modal, para que a correção caiba num arquivo só. Focar o container (e
 *    não o primeiro botão) faz o leitor de tela anunciar o `aria-label` do diálogo antes de
 *    qualquer controle — o cliente ouve *onde* chegou antes de ouvir *o que* pode fazer.
 * 2. **Enquanto aberto**, prende o Tab: do último volta ao primeiro, do primeiro com Shift
 *    volta ao último. E `Escape` fecha — não havia **um** tratador de teclado no projeto.
 * 3. **Ao fechar**, devolve o foco a quem abriu. Sem isso o foco volta para o começo da
 *    página, e quem navega por teclado precisa refazer todo o caminho.
 *
 * **A pilha existe por causa dos modais empilhados.** O detalhe do produto abre por cima da
 * gaveta do roteiro. Sem a pilha, os dois ouviriam o mesmo `Escape` no `document` e fechariam
 * juntos; `stopPropagation` não resolveria, porque ambos escutam no mesmo alvo. Só o modal do
 * topo responde.
 *
 * @param {boolean} isOpen  se o modal está aberto
 * @param {Function} onClose  o que fazer no Escape
 * @returns {import('react').RefObject} ref para pôr no elemento com role="dialog"
 */

const SELETOR_FOCAVEL = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled]):not([type="hidden"])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])'
].join(', ')

/** Modais abertos, do mais antigo ao mais recente. Só o último responde ao teclado. */
const pilha = []

export default function useModalAcessivel(isOpen, onClose) {
  const containerRef = useRef(null)

  // onClose fica numa ref para não entrar nas dependências do efeito. Se entrasse, um
  // `onClose={() => setX(false)}` no pai — que é uma função nova a cada render — remontaria o
  // efeito e roubaria o foco de volta para o container a cada digitação do cliente.
  const fecharRef = useRef(onClose)
  useEffect(() => {
    fecharRef.current = onClose
  })

  useEffect(() => {
    const container = containerRef.current
    if (!isOpen || !container) return

    const origem = document.activeElement
    pilha.push(container)

    container.setAttribute('tabindex', '-1')
    container.focus({ preventScroll: true })

    const focaveis = () =>
      Array.from(container.querySelectorAll(SELETOR_FOCAVEL)).filter(
        el => el.offsetWidth > 0 || el.offsetHeight > 0 || el === document.activeElement
      )

    function aoTeclar(evento) {
      if (pilha[pilha.length - 1] !== container) return

      if (evento.key === 'Escape') {
        evento.preventDefault()
        fecharRef.current?.()
        return
      }

      if (evento.key !== 'Tab') return

      const lista = focaveis()
      if (lista.length === 0) {
        evento.preventDefault()
        return
      }

      const primeiro = lista[0]
      const ultimo = lista[lista.length - 1]
      const atual = document.activeElement
      const foraDoModal = !container.contains(atual)

      if (evento.shiftKey && (atual === primeiro || foraDoModal)) {
        evento.preventDefault()
        ultimo.focus()
      } else if (!evento.shiftKey && (atual === ultimo || foraDoModal)) {
        evento.preventDefault()
        primeiro.focus()
      }
    }

    document.addEventListener('keydown', aoTeclar)

    return () => {
      document.removeEventListener('keydown', aoTeclar)

      const posicao = pilha.indexOf(container)
      if (posicao !== -1) pilha.splice(posicao, 1)

      // Devolve o foco só quando ele ficou órfão por causa do fechamento. São dois casos, e o
      // segundo é o normal: quando o React remove o modal do documento, o elemento focado
      // desaparece junto e o navegador joga o foco no `<body>`. Testar apenas
      // `container.contains(activeElement)` não bastava — na hora da limpeza o container já
      // saiu do documento, a condição dava falso e o foco nunca voltava.
      //
      // Se o cliente já clicou em outro lugar da página, `atual` é esse outro lugar e nada
      // acontece: roubar o foco de volta seria pior do que não fazer nada.
      const atual = document.activeElement
      const focoFicouOrfao = !atual || atual === document.body || container.contains(atual)

      if (origem && document.contains(origem) && focoFicouOrfao) {
        origem.focus({ preventScroll: true })
      }
    }
  }, [isOpen])

  return containerRef
}
