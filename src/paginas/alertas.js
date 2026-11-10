import { montarLayout } from '../componentes/estrutura.js'
import { badge, NIVEL_PRIORIDADE } from '../componentes/selo.js'
import { api } from '../servicos/api.js'
import { esc } from '../utilitarios/textos.js'

if (montarLayout()) {
  const lista = document.getElementById('l-alt')
  // Posição do botão → tipo de alerta (null = todos)
  const filtros = [null, 'estoque', 'validade']
  let alertas = []

  function desenhar(tipo) {
    const filtrados = alertas.filter((a) => !tipo || a.tipoAlerta === tipo)
    lista.innerHTML = filtrados.length
      ? filtrados.map((a) => {
        const nivel = NIVEL_PRIORIDADE[a.prioridade]
        return `
        <li>
          <a class="alerta-link" href="${a.tipoAlerta === 'estoque' ? 'estoque-baixo.html' : 'vencendo-logo.html'}" aria-label="${esc(`${a.titulo}: ${a.detalhe}`)}" title="Ver itens relacionados">
            <div class="dot ${nivel}">!</div>
            <div><b>${esc(a.titulo)}</b><small>${esc(a.detalhe)}</small></div>
            <span class="tag">${badge(nivel, a.prioridade)}</span>
          </a>
        </li>`
      }).join('')
      : '<li><div><small>Nenhum alerta no momento.</small></div></li>'
  }

  document.querySelectorAll('#fa button').forEach((botao, i) => {
    botao.addEventListener('click', () => desenhar(filtros[i]))
  })

  api('GET', '/alertas')
    .then((dados) => {
      alertas = dados
      desenhar(null)
    })
    .catch((erro) => {
      lista.innerHTML = `<li><div><small>${esc(erro.message)}</small></div></li>`
    })
}
