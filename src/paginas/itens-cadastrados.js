import { montarLayout } from '../componentes/estrutura.js'
import { itensCadastrados } from '../dados/dadosExemplo.js'
import { esc } from '../utilitarios/textos.js'

if (montarLayout()) {
  const $ = (id) => document.getElementById(id)
  const num = (valor) => valor.toLocaleString('pt-BR')

  function renderizar(termo = '') {
    const itens = itensCadastrados.filter((item) =>
      item.nome.toLocaleLowerCase('pt-BR').includes(termo.trim().toLocaleLowerCase('pt-BR'))
    )

    $('qtd-itens').textContent = `${itens.length} ${itens.length === 1 ? 'item encontrado' : 'itens encontrados'}`
    $('t-itens').innerHTML = itens.length
      ? itens.map((item) => {
        const estoqueBaixo = item.quantidade <= item.minimo
        return `
          <tr>
            <td>${esc(item.nome)}</td>
            <td>${esc(num(item.quantidade))} un.</td>
            <td>${esc(num(item.minimo))} un.</td>
            <td><span class="tag" style="color:${estoqueBaixo ? 'var(--amber)' : 'var(--green)'}">${estoqueBaixo ? 'Estoque baixo' : 'Normal'}</span></td>
          </tr>`
      }).join('')
      : '<tr><td class="vazio" colspan="4">Nenhum item encontrado.</td></tr>'
  }

  $('busca-itens').addEventListener('input', (e) => renderizar(e.target.value))
  renderizar()
}
