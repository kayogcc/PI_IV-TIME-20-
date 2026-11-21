// Autores: Bruno Lobo de Jesus RA: 25019830 (registro da movimentação), Kayo Gabriel RA: 25022660 (leitura pela câmera), Leonardo Fonseca RA: 25024579 (sugestões de materiais pelo nome)

// Palavras-chave: registro de entrada/saída · leitura de código de barras pela câmera (ZXing) · sugestões de materiais
import { BrowserMultiFormatReader } from '@zxing/browser'
import { montarLayout } from '../componentes/estrutura.js'
import { api } from '../servicos/api.js'
import { criarAviso } from '../utilitarios/formulario.js'
import { esc } from '../utilitarios/textos.js'

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

  // ---------- Sugestões de materiais (busca pelo nome) ----------

  // Preenche a lista de sugestões do campo Material com os materiais ativos
  api('GET', '/materiais')
    .then((materiais) => {
      $('lista-materiais').innerHTML = materiais
        .map((m) => `<option value="${esc(m.nome)}">${m.codigoBarras ? esc(m.codigoBarras) : ''}</option>`)
        .join('')
    })
    .catch(() => {}) // sem sugestões, o campo continua funcionando digitando o nome

  // ---------- Leitura do código de barras pela câmera ----------

  const leitorDeCodigo = new BrowserMultiFormatReader()
  let camera = null // controles da câmera enquanto ela está ligada

  function fecharCamera() {
    if (camera) camera.stop()
    camera = null
    $('leitor').hidden = true
  }

  $('abrir-camera').addEventListener('click', async () => {
    aviso.limpar()
    $('leitor').hidden = false
    try {
      // facingMode "environment": câmera de trás no celular; no notebook, usa a webcam
      camera = await leitorDeCodigo.decodeFromConstraints(
        { video: { facingMode: 'environment' } },
        $('video-leitor'),
        (resultado, erro, controles) => {
          if (!resultado) return // ainda não achou um código neste quadro
          controles.stop()
          camera = null
          $('leitor').hidden = true
          campoMaterial.value = resultado.getText()
          // Marca a origem como "Código de barras"
          $('seg-origem').querySelectorAll('button').forEach((b) => b.classList.toggle('on', b.textContent.trim() === 'Código de barras'))
          campoQuantidade.focus()
        }
      )
    } catch {
      fecharCamera()
      aviso.erro('Não foi possível abrir a câmera. Verifique se o navegador tem permissão para usá-la.')
    }
  })

  $('fechar-camera').addEventListener('click', fecharCamera)

  // ---------- Registro da movimentação ----------

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
  form.addEventListener('submit', async (e) => {
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

    // A API confere o saldo do local e recusa saída maior que ele (409)
    const botao = $('confirmar')
    botao.disabled = true
    try {
      const mov = await api('POST', '/movimentacoes', dados)
      aviso.sucesso(`${mov.tipo} de ${mov.quantidade} un. de ${mov.material} registrada.`)
      campoMaterial.value = ''
      campoQuantidade.value = ''
      campoMaterial.focus()
    } catch (erro) {
      const campo = erro.status === 404 ? campoMaterial : erro.status === 409 ? campoQuantidade : null
      aviso.erro(erro.message, campo)
    }
    botao.disabled = false
  })
}