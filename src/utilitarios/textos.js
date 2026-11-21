// Autores: Gabriel Albasini RA: 25019916 (esc), Leonardo Fonseca RA: 25024579 (normalizar)

// Palavras-chave: esc (proteção contra XSS) · normalizar (busca sem acento)
export function esc(texto) {
  return String(texto).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]))
}

// Deixa o texto pronto para comparar em buscas: sem acento, minúsculo e sem espaços sobrando.
// "  Gaze  Estéril " -> "gaze esteril"
export function normalizar(texto) {
  return String(texto ?? '').normalize('NFD').replace(/\p{M}+/gu, '').trim().replace(/\s+/g, ' ').toLocaleLowerCase('pt-BR')
}
