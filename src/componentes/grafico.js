import { diasSemana } from '../dados/dadosExemplo.js'

// Gráfico de barras em HTML/CSS: duas séries lado a lado (entradas e saídas).
export function desenharGrafico(idGrafico, idDias, entradas, saidas) {
  document.getElementById(idGrafico).innerHTML = entradas
    .map((v, i) => `<div class="day"><div style="height:${v}%;background:var(--blue)"></div><div style="height:${saidas[i]}%;background:#bcd0f7"></div></div>`)
    .join('')
  document.getElementById(idDias).innerHTML = diasSemana.map((d) => `<span>${d}</span>`).join('')
}
