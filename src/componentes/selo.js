import { esc } from '../utilitarios/textos.js'

// Prioridade do alerta na API -> classe de cor (ex = alta, lo = média, in = baixa)
export const NIVEL_PRIORIDADE = { Alta: 'ex', Média: 'lo', Baixa: 'in' }

export function badge(tipo, texto) {
  return `<span class="b ${esc(tipo)}">${esc(texto)}</span>`
}
