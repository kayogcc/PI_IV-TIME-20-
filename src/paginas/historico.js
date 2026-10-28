import { montarLayout } from '../componentes/estrutura.js'
import { esc } from '../utilitarios/textos.js'
import { datasSemana, diasSemana, movimentacoesSemana } from '../dados/dadosExemplo.js'

if (montarLayout()) {
  const $ = (id) => document.getElementById(id)
  const num = (v) => v.toLocaleString('pt-BR')
  // Compara sem diferenciar maiúsculas e acentos ("mascara" encontra "Máscara")
  const normalizar = (t) => t.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()

  // Mais recentes primeiro
  const registros = [...movimentacoesSemana].reverse()

  let filtroTipo = ''
  let busca = ''

  function rotuloDoDia(dia) {
    const i = datasSemana.indexOf(dia)
    return i >= 0 ? `${diasSemana[i]}, ${dia}` : dia
  }

  function itemHtml(m) {
    const entrada = m.tipo === 'Entrada'
    return `
      <li>
        <div class="dot ${entrada ? 'in' : 'nt'}">${entrada ? '↓' : '↑'}</div>
        <div><b>${esc(m.material)}</b><small>${esc(m.tipo)} · ${esc(m.origem)} · ${esc(m.local)}</small></div>
        <div class="hist-qtd">
          <b class="${entrada ? 'entrada' : 'saida'}">${entrada ? '+' : '−'}${num(m.quantidade)} un.</b>
          <small>${esc(m.data.split(' ')[1])}</small>
        </div>
      </li>`
  }

  function renderizar() {
    const termo = normalizar(busca.trim())
    const lista = registros.filter((m) =>
      (!filtroTipo || m.tipo === filtroTipo) && (!termo || normalizar(m.material).includes(termo)))

    $('hist-total').textContent = lista.length === 1 ? '1 movimentação' : `${lista.length} movimentações`

    if (!lista.length) {
      $('hist-dias').innerHTML = '<p class="hist-vazio">Nenhuma movimentação encontrada.</p>'
      return
    }

    // Agrupa por dia, mantendo a ordem
    const dias = new Map()
    lista.forEach((m) => {
      const dia = m.data.split(' ')[0]
      if (!dias.has(dia)) dias.set(dia, [])
      dias.get(dia).push(m)
    })

    $('hist-dias').innerHTML = [...dias].map(([dia, itens]) => {
      const somar = (tipo) => itens.filter((m) => m.tipo === tipo).reduce((t, m) => t + m.quantidade, 0)
      return `
        <section class="hist-dia">
          <h4><span>${esc(rotuloDoDia(dia))}</span><small>↓ ${num(somar('Entrada'))} · ↑ ${num(somar('Saída'))}</small></h4>
          <ul class="list">${itens.map(itemHtml).join('')}</ul>
        </section>`
    }).join('')
  }

  $('fm').addEventListener('click', (e) => {
    const botao = e.target.closest('button[data-f]')
    if (!botao) return
    filtroTipo = botao.dataset.f
    renderizar()
  })

  $('hist-busca').addEventListener('input', (e) => {
    busca = e.target.value
    renderizar()
  })

  renderizar()
}