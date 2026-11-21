// Autores: Carlos Eduardo RA: 25020992 (tela), Bruno Lobo de Jesus RA: 25019830 (integração com a API), Leonardo Fonseca RA: 25024579 (indicadores, prioridades e filtro)

// Palavras-chave: tela de estoque baixo · quantidade ≤ mínimo · Alta ≤ 25% do mínimo · filtro por prioridade · falta repor
import { montarLayout } from '../componentes/estrutura.js'
import { badge } from '../componentes/selo.js'
import { api } from '../servicos/api.js'
import { esc } from '../utilitarios/textos.js'

const usuario = montarLayout()

if (usuario) {
  const $ = (id) => document.getElementById(id)
  const num = (v) => v.toLocaleString('pt-BR')
  const comUnidade = (n) => `${n} ${n === 1 ? 'item' : 'itens'}`
  // Locais onde o material ainda tem saldo
  const locais = (saldos) => Object.keys(saldos ?? {}).filter((l) => saldos[l] > 0).join(', ') || '—'

  // Mesma regra da API (AlertaServico.java): Alta se a quantidade está em até 25% do mínimo, senão Média
  const nivelDoItem = (m) => (m.quantidade * 4 <= m.quantidadeMinima ? 'ex' : 'lo')
  const TEXTO_NIVEL = { ex: 'Alta', lo: 'Média' }

  let itens = []

  // Tabela com filtro por prioridade
  function renderizar(filtro = '') {
    const lista = itens.filter((i) => !filtro || i.nivel === filtro)
    $('qtd-registros').textContent = `${comUnidade(lista.length)} ${lista.length === 1 ? 'encontrado' : 'encontrados'}`

    $('tabela-estoque-baixo').innerHTML = lista.length
      ? lista.map((i) => `
        <tr>
          <td>${esc(i.nome)}</td>
          <td>${esc(locais(i.saldos))}</td>
          <td>${esc(num(i.quantidade))} un.</td>
          <td>${esc(num(i.quantidadeMinima))} un.</td>
          <td>${esc(num(i.faltaRepor))} un.</td>
          <td>${badge(i.nivel, TEXTO_NIVEL[i.nivel])}</td>
        </tr>`).join('')
      : `<tr><td class="vazio" colspan="6">${itens.length ? 'Nenhum item com essa prioridade.' : 'Nenhum produto com estoque baixo.'}</td></tr>`
  }

  async function carregar() {
    let materiais
    try {
      materiais = await api('GET', '/materiais')
    } catch (erro) {
      $('qtd-registros').textContent = ''
      $('tabela-estoque-baixo').innerHTML = `<tr><td class="vazio" colspan="6">${esc(erro.message)}</td></tr>`
      return
    }

    // Mesma regra da API: só conta se houver mínimo definido (> 0) e a quantidade estiver no mínimo ou abaixo.
    // Ordena do mais crítico (menor proporção quantidade/mínimo) ao menos crítico.
    itens = materiais
      .filter((m) => m.quantidadeMinima > 0 && m.quantidade <= m.quantidadeMinima)
      .map((m) => ({
        ...m,
        nivel: nivelDoItem(m),
        // "No mínimo" ainda é alerta; repor pelo menos 1 deixa o item acima do mínimo
        faltaRepor: m.quantidadeMinima - m.quantidade + 1
      }))
      .sort((a, b) => a.quantidade / a.quantidadeMinima - b.quantidade / b.quantidadeMinima)

    // Indicadores
    const contar = (nivel) => itens.filter((i) => i.nivel === nivel).length
    $('qtd-total').textContent = comUnidade(itens.length)
    $('qtd-alta').textContent = comUnidade(contar('ex'))
    $('qtd-media').textContent = comUnidade(contar('lo'))
    renderizar()
  }

  $('filtro-estoque').addEventListener('click', (e) => {
    const botao = e.target.closest('button[data-f]')
    if (botao) renderizar(botao.dataset.f)
  })

  carregar()
}
