import { montarLayout } from '../componentes/estrutura.js'
import { criarAviso } from '../utilitarios/formulario.js'

const QUANTIDADE_MAXIMA = 100000
const LOCAIS = ['Almoxarifado A', 'Almoxarifado B', 'Centro Cirúrgico']

if (montarLayout()) {
  const $ = (id) => document.getElementById(id)
  const form = $('form-mov')
  const campoMaterial = $('material')
  const campoQuantidade = $('quantidade')
  const campoLocal = $('local')
  const aviso = criarAviso(form, form.querySelector('.acts'))

  const opcaoSelecionada = (idGrupo) => $(idGrupo).querySelector('button.on').textContent.trim()

  function validar() {
    const material = campoMaterial.value.trim()
    // value fica '' quando o navegador não consegue ler o número (ex.: "1e", "--2")
    const texto = campoQuantidade.value.trim()
    const quantidade = Number(texto)

    if (!material) {
      return { mensagem: 'Informe o material (leia o código de barras ou digite o nome).', campo: campoMaterial }
    }
    if (material.length < 2) {
      return { mensagem: 'O nome do material está muito curto.', campo: campoMaterial }
    }
    if (!texto) {
      const mensagem = campoQuantidade.validity.badInput ? 'A quantidade deve ser um número.' : 'Informe a quantidade.'
      return { mensagem, campo: campoQuantidade }
    }
    if (!Number.isInteger(quantidade)) {
      return { mensagem: 'A quantidade deve ser um número inteiro.', campo: campoQuantidade }
    }
    if (quantidade <= 0) {
      return { mensagem: 'A quantidade deve ser maior que zero.', campo: campoQuantidade }
    }
    if (quantidade > QUANTIDADE_MAXIMA) {
      return { mensagem: `A quantidade máxima por registro é ${QUANTIDADE_MAXIMA.toLocaleString('pt-BR')}.`, campo: campoQuantidade }
    }
    if (!LOCAIS.includes(campoLocal.value)) {
      return { mensagem: 'Selecione um local válido.', campo: campoLocal }
    }
    return null
  }

  form.addEventListener('input', aviso.limpar)
  form.addEventListener('submit', (e) => {
    e.preventDefault()
    aviso.limpar()

    const problema = validar()
    if (problema) {
      aviso.erro(problema.mensagem, problema.campo)
      return
    }

    const dados = {
      tipo: opcaoSelecionada('seg-tipo'),
      origem: opcaoSelecionada('seg-origem'),
      material: campoMaterial.value.trim(),
      quantidade: Number(campoQuantidade.value),
      local: campoLocal.value
    }

    // TODO: enviar para a API (ex.: POST /api/movimentacoes). Lá também é preciso
    // recusar saída maior que o saldo do material no local.
    console.log('Movimentação (demonstração):', dados)

    aviso.sucesso(`${dados.tipo} de ${dados.quantidade} un. de ${dados.material} registrada.`)
    campoMaterial.value = ''
    campoQuantidade.value = ''
    campoMaterial.focus()
  })
}
