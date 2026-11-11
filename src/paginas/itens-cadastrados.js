import { montarLayout } from '../componentes/estrutura.js'
import { api } from '../servicos/api.js'
import { criarAviso } from '../utilitarios/formulario.js'
import { esc } from '../utilitarios/textos.js'

const QUANTIDADE_MAXIMA = 100000
const PERFIS_QUE_CADASTRAM = ['ALMOXARIFADO', 'GESTAO']

const usuario = montarLayout()
if (usuario) {
  const $ = (id) => document.getElementById(id)
  const num = (valor) => valor.toLocaleString('pt-BR')
  // "2026-12-01" -> "01/12/2026"
  const data = (iso) => (iso ? iso.split('-').reverse().join('/') : '—')
  const podeEditar = PERFIS_QUE_CADASTRAM.includes(usuario.perfil)
  const totalColunas = podeEditar ? 6 : 5

  let itens = []

  function botoesDoItem(item) {
    if (!podeEditar) return ''
    return `
      <td>
        <div class="acoes-item">
          <button type="button" class="btn-texto" data-acao="editar" data-id="${esc(item.id)}">Editar</button>
          <button type="button" class="btn-texto perigo" data-acao="inativar" data-id="${esc(item.id)}">Inativar</button>
        </div>
      </td>`
  }

  function renderizar() {
    const termo = $('busca-itens').value.trim().toLocaleLowerCase('pt-BR')
    const filtrados = itens.filter((item) =>
      item.nome.toLocaleLowerCase('pt-BR').includes(termo) || item.codigoBarras === termo
    )

    $('qtd-itens').textContent = `${filtrados.length} ${filtrados.length === 1 ? 'item encontrado' : 'itens encontrados'}`
    $('t-itens').innerHTML = filtrados.length
      ? filtrados.map((item) => {
        const estoqueBaixo = item.quantidade <= item.quantidadeMinima
        return `
          <tr>
            <td>${esc(item.nome)}</td>
            <td>${esc(num(item.quantidade))} un.</td>
            <td>${esc(num(item.quantidadeMinima))} un.</td>
            <td>${esc(data(item.validade))}</td>
            <td><span class="tag" style="color:${estoqueBaixo ? 'var(--amber)' : 'var(--green)'}">${estoqueBaixo ? 'Estoque baixo' : 'Normal'}</span></td>
            ${botoesDoItem(item)}
          </tr>`
      }).join('')
      : `<tr><td class="vazio" colspan="${totalColunas}">Nenhum item encontrado.</td></tr>`
  }

  async function carregar() {
    try {
      itens = await api('GET', '/materiais')
      renderizar()
    } catch (erro) {
      $('qtd-itens').textContent = ''
      $('t-itens').innerHTML = `<tr><td class="vazio" colspan="${totalColunas}">${esc(erro.message)}</td></tr>`
    }
  }

  $('busca-itens').addEventListener('input', renderizar)
  carregar()

  if (podeEditar) {
    const form = $('form-material')
    const titulo = $('titulo-form')
    const campoNome = $('mat-nome')
    const campoCodigo = $('mat-codigo')
    const campoLote = $('mat-lote')
    const campoValidade = $('mat-validade')
    const campoMinimo = $('mat-minimo')
    const botao = $('salvar-material')
    const botaoCancelar = $('cancelar-edicao')
    const aviso = criarAviso(form, form.querySelector('.acts'))
    form.hidden = false
    document.querySelector('.col-acoes').hidden = false

    // null: cadastrando um material novo. Com id: editando aquele material.
    let idEmEdicao = null

    const textoDoBotao = () => (idEmEdicao ? 'Salvar alterações' : 'Cadastrar material')

    function entrarNaEdicao(item) {
      idEmEdicao = item.id
      aviso.limpar()
      campoNome.value = item.nome
      campoCodigo.value = item.codigoBarras ?? ''
      campoLote.value = item.lote ?? ''
      campoValidade.value = item.validade ?? ''
      campoMinimo.value = item.quantidadeMinima
      titulo.textContent = `Editar material: ${item.nome}`
      botao.textContent = textoDoBotao()
      botaoCancelar.hidden = false
      form.scrollIntoView({ behavior: 'smooth', block: 'start' })
      campoNome.focus({ preventScroll: true })
    }

    function sairDaEdicao() {
      idEmEdicao = null
      form.reset()
      titulo.textContent = 'Cadastrar material'
      botao.textContent = textoDoBotao()
      botaoCancelar.hidden = true
    }

    async function inativar(item) {
      const alertaSaldo = item.quantidade > 0
        ? `\n\nAtenção: ainda há ${num(item.quantidade)} un. em estoque.`
        : ''
      if (!confirm(`Inativar "${item.nome}"?\n\nEle sai da lista e não pode mais ser movimentado. O histórico continua guardado.${alertaSaldo}`)) return

      try {
        await api('DELETE', `/materiais/${encodeURIComponent(item.id)}`)
        if (idEmEdicao === item.id) sairDaEdicao()
        aviso.sucesso(`${item.nome} inativado.`)
        await carregar()
      } catch (erro) {
        aviso.erro(erro.message)
      }
    }

    // Clique em "Editar" ou "Inativar" de qualquer linha da tabela
    $('t-itens').addEventListener('click', (e) => {
      const botaoAcao = e.target.closest('button[data-acao]')
      if (!botaoAcao) return
      const item = itens.find((i) => i.id === botaoAcao.dataset.id)
      if (!item) return
      if (botaoAcao.dataset.acao === 'editar') entrarNaEdicao(item)
      else inativar(item)
    })

    botaoCancelar.addEventListener('click', () => {
      aviso.limpar()
      sairDaEdicao()
    })

    // Mesmas regras da API (MaterialServico.java)
    function validar() {
      const nome = campoNome.value.trim()
      const codigo = campoCodigo.value.trim()
      const minimo = Number(campoMinimo.value)

      if (nome.length < 2) return { mensagem: 'Informe o nome do material.', campo: campoNome }
      if (codigo && !/^[A-Za-z0-9-]{4,50}$/.test(codigo)) {
        return { mensagem: 'Código de barras inválido: use de 4 a 50 letras, números ou hífen.', campo: campoCodigo }
      }
      if (campoValidade.validity.badInput) return { mensagem: 'Validade inválida.', campo: campoValidade }
      if (!campoMinimo.value.trim()) {
        const mensagem = campoMinimo.validity.badInput ? 'A quantidade mínima deve ser um número.' : 'Informe a quantidade mínima.'
        return { mensagem, campo: campoMinimo }
      }
      if (!Number.isInteger(minimo) || minimo < 0 || minimo > QUANTIDADE_MAXIMA) {
        return { mensagem: `A quantidade mínima deve ser um número inteiro de 0 a ${num(QUANTIDADE_MAXIMA)}.`, campo: campoMinimo }
      }
      return null
    }

    form.addEventListener('input', aviso.limpar)
    form.addEventListener('submit', async (e) => {
      e.preventDefault()
      aviso.limpar()

      const problema = validar()
      if (problema) {
        aviso.erro(problema.mensagem, problema.campo)
        return
      }

      const dados = {
        nome: campoNome.value.trim(),
        codigoBarras: campoCodigo.value.trim() || null,
        lote: campoLote.value.trim() || null,
        validade: campoValidade.value || null,
        quantidadeMinima: Number(campoMinimo.value)
      }
      const editando = idEmEdicao !== null

      botao.disabled = true
      botao.textContent = editando ? 'Salvando...' : 'Cadastrando...'
      try {
        if (editando) {
          const material = await api('PUT', `/materiais/${encodeURIComponent(idEmEdicao)}`, dados)
          sairDaEdicao()
          aviso.sucesso(`${material.nome} atualizado.`)
        } else {
          const material = await api('POST', '/materiais', dados)
          form.reset()
          aviso.sucesso(`${material.nome} cadastrado. Registre uma entrada para lançar o estoque inicial.`)
        }
        await carregar()
        campoNome.focus()
      } catch (erro) {
        // 409: nome ou código de barras repetido
        const campo = erro.status === 409 ? (/código/i.test(erro.message) ? campoCodigo : campoNome) : null
        aviso.erro(erro.message, campo)
      }
      botao.disabled = false
      botao.textContent = textoDoBotao()
    })
  }
}