import { useEffect, useRef } from 'react'

/**
 * Trava de foco e Escape para os modais.
 *
 * `aria-modal="true"` manda o leitor de tela ignorar o resto da página, e os oito modais já
 * traziam o atributo. O teclado não obedecia: o Tab saía do modal e entrava em botões que o
 * leitor tinha sido instruído a fingir que não existiam. **Atributo sem trava é pior que
 * atributo nenhum**, porque nenhuma ferramenta automática acusa — a marcação está formalmente
 * correta. Ver D-83.
 *
 * Duas escolhas que não são óbvias no código:
 *
 * - **O foco vai para o container, não para o primeiro botão.** Assim o leitor anuncia o
 *   rótulo do diálogo antes de qualquer controle: o cliente ouve *onde* chegou antes de ouvir
 *   *o que* pode fazer.
 * - **A pilha existe por causa dos modais empilhados.** O detalhe do produto abre por cima da
 *   gaveta do roteiro; sem ela os dois ouviriam o mesmo `Escape` no `document` e fechariam
 *   juntos. `stopPropagation` não resolveria, porque ambos escutam no mesmo alvo.
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
