// Formatação de valores para exibição.
//
// Existe porque esta função estava copiada em oito arquivos. Não era só repetição: as cópias
// tinham divergido. Seis eram idênticas, uma não fazia coerção nenhuma — segura só porque a
// entrada dela sempre vinha de um `reduce` — e uma, inline no mapa, usava `(x || 0)`, que com
// preço em string devolve "4.2" em vez de R$ 4,20, porque `String.toLocaleString()` ignora as
// opções de moeda.

/**
 * Formata um valor como moeda brasileira.
 *
 * Aceita número ou string, porque o preço chega dos dois jeitos: como número na resposta da
 * API e como string em dado guardado no aparelho. Valor ausente ou ilegível vira zero — numa
 * tela de loja, "R$ 0,00" é menos ruim que "NaN" ou um espaço em branco.
 */
export function formatPrice(valor) {
  const numero = typeof valor === 'number' ? valor : parseFloat(valor) || 0
  return numero.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}
