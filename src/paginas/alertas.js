import { montarLayout } from '../componentes/estrutura.js'
import { badge } from '../componentes/selo.js'
import { esc } from '../utilitarios/textos.js'
import { alertas } from '../dados/dadosExemplo.js'

if (montarLayout()) {
  // Posição do botão → tipo de alerta (null = todos)
  const filtros = [null, 'estoque', 'validade']

  function desenhar(tipo) {
    document.getElementById('l-alt').innerHTML = alertas
      .filter((a) => !tipo || a.tipoAlerta === tipo)
      .map((a) => `
        <li>
          <a class="alerta-link" href="${a.tipoAlerta === 'estoque' ? 'estoque-baixo.html' : 'vencendo-logo.html'}" aria-label="${esc(`${a.titulo}: ${a.detalhe}`)}" title="Ver itens relacionados">
            <div class="dot ${a.nivel}">!</div>
            <div><b>${esc(a.titulo)}</b><small>${esc(a.detalhe)}</small></div>
            <span class="tag">${badge(a.nivel, a.prioridade)}</span>
          </a>
        </li>`).join('')
  }

  document.querySelectorAll('#fa button').forEach((botao, i) => {
    botao.addEventListener('click', () => desenhar(filtros[i]))
  })
  desenhar(null)
}