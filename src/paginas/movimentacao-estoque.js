import { montarLayout } from '../componentes/estrutura.js'
import { desenharGrafico } from '../componentes/grafico.js'
import { api } from '../servicos/api.js'
import { diaMes, diaSemana, ultimosSeteDias } from '../utilitarios/datas.js'
import { esc } from '../utilitarios/textos.js'

const usuario = montarLayout()

if (usuario) {
  const $ = (id) => document.getElementById(id)
  const num = (v) => v.toLocaleString('pt-BR')
  const comSinal = (v) => (v > 0 ? '+' : v < 0 ? '−' : '') + num(Math.abs(v))
  const corSaldo = (v) => (v >= 0 ? 'var(--green)' : 'var(--red)')
  const somar = (lista) => lista.reduce((t, m) => t + m.quantidade, 0)

  const semana = ultimosSeteDias()
  $('periodo').textContent = `Entradas e saídas de ${diaMes(semana[0])} a ${diaMes(semana[6])}.`

  function desenhar(movimentacoes) {
    const entradas = movimentacoes.filter((m) => m.tipo === 'Entrada')
    const saidas = movimentacoes.filter((m) => m.tipo === 'Saída')
    const totalEntradas = somar(entradas)
    const totalSaidas = somar(saidas)
    const saldo = totalEntradas - totalSaidas

    // Indicadores
    $('tot-entradas').textContent = `${num(totalEntradas)} un.`
    $('reg-entradas').textContent = `${entradas.length} registros`
    $('tot-saidas').textContent = `${num(totalSaidas)} un.`
    $('reg-saidas').textContent = `${saidas.length} registros`
    $('tot-saldo').textContent = `${comSinal(saldo)} un.`
    $('saldo-msg').textContent = saldo >= 0 ? 'Entrou mais do que saiu' : 'Saiu mais do que entrou'
    $('saldo-msg').style.color = corSaldo(saldo)

    // Soma de entradas e saídas (em unidades) de cada dia da semana
    const totaisPorDia = semana.map((dia) => {
      const doDia = movimentacoes.filter((m) => diaMes(m.quando) === diaMes(dia))
      return {
        rotulo: diaSemana(dia),
        dia: diaMes(dia),
        entradas: somar(doDia.filter((m) => m.tipo === 'Entrada')),
        saidas: somar(doDia.filter((m) => m.tipo === 'Saída'))
      }
    })

    // Gráfico + tabela por dia
    desenharGrafico('chart', 'days', totaisPorDia)

    $('t-dias').innerHTML = totaisPorDia.map((d) => `
      <tr>
        <td><b>${d.rotulo}</b> <small>${d.dia}</small></td>
        <td>${num(d.entradas)}</td>
        <td>${num(d.saidas)}</td>
        <td style="color:${corSaldo(d.entradas - d.saidas)}">${comSinal(d.entradas - d.saidas)}</td>
      </tr>`).join('')

    // Agrupa as movimentações por um campo (material ou local)
    function agrupar(campo) {
      const grupos = {}
      movimentacoes.forEach((m) => {
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
    const vazio = '<li><div><small>Nenhuma movimentação no período.</small></div></li>'

    $('l-materiais').innerHTML = agrupar('material').slice(0, 5).map(itemGrupo).join('') || vazio
    $('l-locais').innerHTML = agrupar('local').map(itemGrupo).join('') || vazio

    // Lista de registros com filtro Todas / Entradas / Saídas (a API já manda os mais recentes primeiro)
    function renderizarRegistros(filtro = '') {
      const lista = movimentacoes.filter((m) => !filtro || m.tipo === filtro)
      $('qtd-registros').textContent = `${lista.length} movimentações encontradas`
      $('l-registros').innerHTML = lista.map((m) => `
        <li>
          <div class="dot ${m.tipo === 'Entrada' ? 'in' : 'nt'}">${m.tipo === 'Entrada' ? '↓' : '↑'}</div>
          <div><b>${esc(m.material)} — ${esc(m.quantidade)} un.</b><small>${esc(m.tipo)} · ${esc(m.origem)} · ${esc(m.local)}</small></div>
          <span class="tag" style="color:var(--mute)">${esc(m.quando.toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' }))}</span>
        </li>`).join('')
    }

    $('filtro-mov').addEventListener('click', (e) => {
      const botao = e.target.closest('button[data-f]')
      if (botao) renderizarRegistros(botao.dataset.f)
    })

    renderizarRegistros()
  }

  api('GET', '/movimentacoes?dias=7')
    .then((dados) => desenhar(dados
      .map((m) => ({ ...m, quando: new Date(m.data) }))
      .filter((m) => m.quando >= semana[0])))
    .catch((erro) => {
      $('qtd-registros').textContent = erro.message
    })
}
