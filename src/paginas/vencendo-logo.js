import { montarLayout } from '../componentes/estrutura.js'
import { badge } from '../componentes/selo.js'
import { esc } from '../utilitarios/textos.js'
import { itensVencendo } from '../dados/dadosExemplo.js'

const usuario = montarLayout()

if (usuario) {
  const $ = (id) => document.getElementById(id)
  const num = (v) => v.toLocaleString('pt-BR')

  // Mesma escala das telas de alertas: ex = alta, lo = média, in = baixa
  const nivelDoPrazo = (dias) => (dias <= 7 ? 'ex' : dias <= 15 ? 'lo' : 'in')
  const textoPrazo = (dias) => (dias === 1 ? '1 dia' : `${dias} dias`)

  // Data de validade = hoje + dias restantes
  function dataDeValidade(dias) {
    const d = new Date()
    d.setDate(d.getDate() + dias)
    return d.toLocaleDateString('pt-BR')
  }

  // Mais urgentes primeiro
  const itens = [...itensVencendo]
    .sort((a, b) => a.dias - b.dias)
    .map((i) => ({ ...i, nivel: nivelDoPrazo(i.dias) }))

  // Indicadores
  const contar = (nivel) => itens.filter((i) => i.nivel === nivel).length
  const comUnidade = (n) => `${n} ${n === 1 ? 'item' : 'itens'}`
  $('qtd-alta').textContent = comUnidade(contar('ex'))
  $('qtd-media').textContent = comUnidade(contar('lo'))
  $('qtd-baixa').textContent = comUnidade(contar('in'))

  // Tabela com filtro por faixa de prazo
  function renderizar(filtro = '') {
    const lista = itens.filter((i) => !filtro || i.nivel === filtro)
    $('qtd-registros').textContent = `${comUnidade(lista.length)} encontrados`

    $('t-vencendo').innerHTML = lista.length
      ? lista.map((i) => `
        <tr>
          <td>${esc(i.material)}</td>
          <td>${esc(i.lote)}</td>
          <td>${esc(i.local)}</td>
          <td>${esc(num(i.quantidade))} un.</td>
          <td>${esc(dataDeValidade(i.dias))}</td>
          <td>${badge(i.nivel, textoPrazo(i.dias))}</td>
        </tr>`).join('')
      : '<tr><td class="vazio" colspan="6">Nenhum item nessa faixa de prazo.</td></tr>'
  }

  $('filtro-venc').addEventListener('click', (e) => {
    const botao = e.target.closest('button[data-f]')
    if (botao) renderizar(botao.dataset.f)
  })

  renderizar()
}
