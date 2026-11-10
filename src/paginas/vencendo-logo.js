import { montarLayout } from '../componentes/estrutura.js'
import { badge } from '../componentes/selo.js'
import { api } from '../servicos/api.js'
import { dataIso, diasAte } from '../utilitarios/datas.js'
import { esc } from '../utilitarios/textos.js'

const DIAS_VALIDADE_PROXIMA = 30

const usuario = montarLayout()

if (usuario) {
  const $ = (id) => document.getElementById(id)
  const num = (v) => v.toLocaleString('pt-BR')

  // Mesma escala das telas de alertas: ex = alta, lo = média, in = baixa
  const nivelDoPrazo = (dias) => (dias <= 7 ? 'ex' : dias <= 15 ? 'lo' : 'in')
  const textoPrazo = (dias) => (dias < 0 ? 'Vencido' : dias === 0 ? 'Hoje' : dias === 1 ? '1 dia' : `${dias} dias`)
  const comUnidade = (n) => `${n} ${n === 1 ? 'item' : 'itens'}`
  // Locais onde o material tem saldo
  const locais = (saldos) => Object.keys(saldos ?? {}).filter((l) => saldos[l] > 0).join(', ') || '—'

  let itens = []

  // Tabela com filtro por faixa de prazo
  function renderizar(filtro = '') {
    const lista = itens.filter((i) => !filtro || i.nivel === filtro)
    $('qtd-registros').textContent = `${comUnidade(lista.length)} encontrados`

    $('t-vencendo').innerHTML = lista.length
      ? lista.map((i) => `
        <tr>
          <td>${esc(i.nome)}</td>
          <td>${esc(i.lote || '—')}</td>
          <td>${esc(locais(i.saldos))}</td>
          <td>${esc(num(i.quantidade))} un.</td>
          <td>${esc(dataIso(i.validade))}</td>
          <td>${badge(i.nivel, textoPrazo(i.dias))}</td>
        </tr>`).join('')
      : '<tr><td class="vazio" colspan="6">Nenhum item nessa faixa de prazo.</td></tr>'
  }

  async function carregar() {
    try {
      const materiais = await api('GET', '/materiais')
      // Mesma regra da API (AlertaServico.java): vence em até 30 dias e ainda tem saldo. Mais urgentes primeiro.
      itens = materiais
        .filter((m) => m.validade && m.quantidade > 0)
        .map((m) => ({ ...m, dias: diasAte(m.validade) }))
        .filter((m) => m.dias <= DIAS_VALIDADE_PROXIMA)
        .sort((a, b) => a.dias - b.dias)
        .map((m) => ({ ...m, nivel: nivelDoPrazo(m.dias) }))
    } catch (erro) {
      $('qtd-registros').textContent = ''
      $('t-vencendo').innerHTML = `<tr><td class="vazio" colspan="6">${esc(erro.message)}</td></tr>`
      return
    }

    // Indicadores
    const contar = (nivel) => itens.filter((i) => i.nivel === nivel).length
    $('qtd-alta').textContent = comUnidade(contar('ex'))
    $('qtd-media').textContent = comUnidade(contar('lo'))
    $('qtd-baixa').textContent = comUnidade(contar('in'))
    renderizar()
  }

  $('filtro-venc').addEventListener('click', (e) => {
    const botao = e.target.closest('button[data-f]')
    if (botao) renderizar(botao.dataset.f)
  })

  carregar()
}
