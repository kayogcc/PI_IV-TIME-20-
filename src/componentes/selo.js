import { esc } from '../utilitarios/textos.js'

export function badge(tipo, texto) {
  return `<span class="b ${esc(tipo)}">${esc(texto)}</span>`
}