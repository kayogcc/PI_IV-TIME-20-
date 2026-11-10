import { esc } from '../utilitarios/textos.js'

// Gráfico de barras em HTML/CSS: duas séries lado a lado (entradas e saídas).
// dias: [{ rotulo, entradas, saidas }] em unidades. A altura é em % do maior valor (deixa uma folga no topo).
export function desenharGrafico(idGrafico, idDias, dias) {
  const maior = Math.max(1, ...dias.flatMap((d) => [d.entradas, d.saidas]))
  const altura = (v) => Math.round((v / maior) * 90)
  document.getElementById(idGrafico).innerHTML = dias
    .map((d) => `<div class="day"><div style="height:${altura(d.entradas)}%;background:var(--blue)"></div><div style="height:${altura(d.saidas)}%;background:#bcd0f7"></div></div>`)
    .join('')
  document.getElementById(idDias).innerHTML = dias.map((d) => `<span>${esc(d.rotulo)}</span>`).join('')
}
