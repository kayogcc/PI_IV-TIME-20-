import { montarLayout } from '../componentes/estrutura.js'
import { desenharGrafico } from '../componentes/grafico.js'
import { esc } from '../utilitarios/textos.js'
import { datasSemana, diasSemana, movimentacaoSemanal, movimentacoesSemana, totaisPorDia } from '../dados/dadosExemplo.js'

const usuario = montarLayout()

if (usuario) {
  const $ = (id) => document.getElementById(id)
  const num = (v) => v.toLocaleString('pt-BR')
  const comSinal = (v) => (v > 0 ? '+' : v < 0 ? '−' : '') + num(Math.abs(v))
  const corSaldo = (v) => (v >= 0 ? 'var(--green)' : 'var(--red)')
  const somar = (lista) => lista.reduce((t, m) => t + m.quantidade, 0)

  const entradas = movimentacoesSemana.filter((m) => m.tipo === 'Entrada')
  const saidas = movimentacoesSemana.filter((m) => m.tipo === 'Saída')
  const totalEntradas = somar(entradas)
  const totalSaidas = somar(saidas)
  const saldo = totalEntradas - totalSaidas

  // Cabeçalho e indicadores
  $('periodo').textContent = `Entradas e saídas de ${datasSemana[0]} a ${datasSemana[datasSemana.length - 1]}.`
  $('tot-entradas').textContent = `${num(totalEntradas)} un.`
  $('reg-entradas').textContent = `${entradas.length} registros`
  $('tot-saidas').textContent = `${num(totalSaidas)} un.`
  $('reg-saidas').textContent = `${saidas.length} registros`
  $('tot-saldo').textContent = `${comSinal(saldo)} un.`
  $('saldo-msg').textContent = saldo >= 0 ? 'Entrou mais do que saiu' : 'Saiu mais do que entrou'
  $('saldo-msg').style.color = corSaldo(saldo)

  // Gráfico + tabela por dia
  desenharGrafico('chart', 'days', movimentacaoSemanal.entradas, movimentacaoSemanal.saidas)

  $('t-dias').innerHTML = totaisPorDia.map((d, i) => `
    <tr>
      <td><b>${diasSemana[i]}</b> <small>${d.dia}</small></td>
      <td>${num(d.entradas)}</td>
      <td>${num(d.saidas)}</td>
      <td style="color:${corSaldo(d.entradas - d.saidas)}">${comSinal(d.entradas - d.saidas)}</td>
    </tr>`).join('')

  // Agrupa as movimentações por um campo (material ou local)
  function agrupar(campo) {
    const grupos = {}
    movimentacoesSemana.forEach((m) => {
      const g = (grupos[m[campo]] ??= { nome: m[campo], entradas: 0, saidas: 0 })
      if (m.tipo === 'Entrada') g.entradas += m.quantidade
      else g.saidas += m.quantidade
    })
    return Object.values(grupos).sort((a, b) => (b.entradas + b.saidas) - (a.entradas + a.saidas))
  }

  const itemGrupo = (g) => `
    <li>
      <div class="dot in">${esc(g.nome[0])}</div>
      <div><b>${esc(g.nome)}</b><small>↓ ${num(g.entradas)} entradas · ↑ ${num(g.saidas)} saídas</small></div>
      <span class="tag" style="color:var(--mute)">${num(g.entradas + g.saidas)} un.</span>
    </li>`

  $('l-materiais').innerHTML = agrupar('material').slice(0, 5).map(itemGrupo).join('')
  $('l-locais').innerHTML = agrupar('local').map(itemGrupo).join('')

  // Lista de registros com filtro Todas / Entradas / Saídas (mais recentes primeiro)
  function renderizarRegistros(filtro = '') {
    const lista = movimentacoesSemana.filter((m) => !filtro || m.tipo === filtro).reverse()
    $('qtd-registros').textContent = `${lista.length} movimentações encontradas`
    $('l-registros').innerHTML = lista.map((m) => `
      <li>
        <div class="dot ${m.tipo === 'Entrada' ? 'in' : 'nt'}">${m.tipo === 'Entrada' ? '↓' : '↑'}</div>
        <div><b>${esc(m.material)} — ${m.quantidade} un.</b><small>${m.tipo} · ${esc(m.origem)} · ${esc(m.local)}</small></div>
        <span class="tag" style="color:var(--mute)">${m.data}</span>
      </li>`).join('')
  }

  $('filtro-mov').addEventListener('click', (e) => {
    const botao = e.target.closest('button[data-f]')
    if (botao) renderizarRegistros(botao.dataset.f)
  })

  renderizarRegistros()
}
