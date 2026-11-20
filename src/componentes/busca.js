// Autores: Leonardo Fonseca de Oliveira, Carlos Eduardo Marins Fonseca (busca do cabeçalho)

// Palavras-chave: busca do cabeçalho · sugestões enquanto digita · sem acento · Ctrl+K · setas e Enter · abre em itens cadastrados
import { api } from '../servicos/api.js'
import { esc, normalizar } from '../utilitarios/textos.js'

const MAXIMO_SUGESTOES = 6
const PAGINA_DE_ITENS = 'itens-cadastrados.html'

// Leva para a tela de itens já filtrada pelo texto buscado
const urlDaBusca = (texto) => `${PAGINA_DE_ITENS}?busca=${encodeURIComponent(texto)}`

// Mesmas regras da API (AlertaServico.java) para o selo de cada sugestão
function situacao(m) {
  if (m.quantidadeMinima > 0 && m.quantidade <= m.quantidadeMinima) return { classe: 'baixo', texto: 'Estoque baixo' }
  return { classe: 'ok', texto: 'Normal' }
}

// Liga a busca do cabeçalho. "caixa" é o elemento .search já colocado na página.
export function ligarBusca(caixa) {
  const campo = caixa.querySelector('input')
  const lista = caixa.querySelector('.search-resultados')
  const atalho = caixa.querySelector('kbd')

  // Mostra o atalho certo para o sistema: ⌘K no Mac, Ctrl K no resto
  const ehMac = /Mac|iPhone|iPad/.test(navigator.platform)
  atalho.textContent = ehMac ? '⌘K' : 'Ctrl K'

  let materiais = null // carregados uma vez, na primeira vez que a busca é usada
  let carregando = null // pedido em andamento (evita pedir a lista duas vezes ao mesmo tempo)
  let sugestoes = [] // materiais mostrados na lista agora
  let selecionada = -1 // posição escolhida com as setas (-1 = nenhuma)

  // Busca a lista de materiais na API só uma vez e guarda para as próximas buscas
  function carregarMateriais() {
    if (materiais) return Promise.resolve(materiais)
    if (!carregando) {
      carregando = api('GET', '/materiais')
        .then((dados) => (materiais = dados))
        .catch((erro) => {
          carregando = null // deixa tentar de novo na próxima vez
          throw erro
        })
    }
    return carregando
  }

  // Esconde a lista de sugestões e volta ao estado inicial
  function fechar() {
    lista.hidden = true
    lista.innerHTML = ''
    campo.setAttribute('aria-expanded', 'false')
    campo.removeAttribute('aria-activedescendant')
    sugestoes = []
    selecionada = -1
  }

  // Mostra a lista com o conteúdo recebido
  function abrir(html) {
    lista.innerHTML = html
    lista.hidden = false
    campo.setAttribute('aria-expanded', 'true')
  }

  // Destaca a sugestão escolhida com as setas e rola a lista até ela
  function marcar(indice) {
    const opcoes = lista.querySelectorAll('[role="option"]')
    opcoes.forEach((o, i) => o.setAttribute('aria-selected', String(i === indice)))
    selecionada = indice
    if (indice >= 0) {
      campo.setAttribute('aria-activedescendant', opcoes[indice].id)
      opcoes[indice].scrollIntoView({ block: 'nearest' })
    } else {
      campo.removeAttribute('aria-activedescendant')
    }
  }

  // Roda a cada tecla digitada: filtra os materiais e monta a lista de sugestões
  async function atualizar() {
    const termo = normalizar(campo.value)
    if (!termo) return fechar()

    try {
      await carregarMateriais()
    } catch (erro) {
      abrir(`<li class="search-aviso">${esc(erro.message)}</li>`)
      return
    }
    // O usuário pode ter continuado digitando enquanto os materiais carregavam
    if (normalizar(campo.value) !== termo) return

    // Busca por nome ou código de barras, ignorando maiúsculas e acentos.
    // Quem começa com o termo aparece antes de quem só contém o termo.
    const encontrados = materiais
      .filter((m) => normalizar(m.nome).includes(termo) || normalizar(m.codigoBarras).includes(termo))
      .sort((a, b) => {
        const comecaA = normalizar(a.nome).startsWith(termo) ? 0 : 1
        const comecaB = normalizar(b.nome).startsWith(termo) ? 0 : 1
        return comecaA - comecaB || a.nome.localeCompare(b.nome, 'pt-BR')
      })

    sugestoes = encontrados.slice(0, MAXIMO_SUGESTOES)
    selecionada = -1

    if (!sugestoes.length) {
      abrir(`<li class="search-aviso">Nenhum material encontrado para "${esc(campo.value.trim())}".</li>`)
      return
    }

    const itens = sugestoes.map((m, i) => {
      const s = situacao(m)
      const codigo = m.codigoBarras ? ` · ${esc(m.codigoBarras)}` : ''
      return `
        <li id="busca-opcao-${i}" role="option" aria-selected="false" data-i="${i}">
          <div>
            <b>${esc(m.nome)}</b>
            <small>${esc(m.quantidade.toLocaleString('pt-BR'))} un.${codigo}</small>
          </div>
          <span class="search-selo ${s.classe}">${s.texto}</span>
        </li>`
    })
    const restantes = encontrados.length - sugestoes.length
    const rodape = `<li class="search-todos" data-todos>Ver ${restantes > 0 ? `todos os ${encontrados.length} resultados` : 'na lista de itens'} →</li>`
    abrir(itens.join('') + rodape)
  }

  // Abre a tela de itens: com o material escolhido ou, sem escolha, com o texto digitado
  function irPara(indice) {
    const termo = indice >= 0 ? sugestoes[indice].nome : campo.value.trim()
    if (termo) location.href = urlDaBusca(termo)
  }

  campo.addEventListener('input', atualizar)
  campo.addEventListener('focus', () => {
    carregarMateriais().catch(() => {}) // já adianta o carregamento
    if (campo.value.trim()) atualizar()
  })

  // Teclado: ↑/↓ escolhem, Enter abre, Esc fecha a lista (ou sai da busca se já estiver fechada)
  campo.addEventListener('keydown', (e) => {
    const total = sugestoes.length
    if (e.key === 'ArrowDown' && total) {
      e.preventDefault()
      marcar((selecionada + 1) % total)
    } else if (e.key === 'ArrowUp' && total) {
      e.preventDefault()
      marcar(selecionada <= 0 ? total - 1 : selecionada - 1)
    } else if (e.key === 'Enter') {
      e.preventDefault()
      irPara(selecionada)
    } else if (e.key === 'Escape') {
      if (!lista.hidden) fechar()
      else campo.blur()
    }
  })

  // mousedown (e não click) para agir antes de o campo perder o foco
  lista.addEventListener('mousedown', (e) => {
    const opcao = e.target.closest('[data-i], [data-todos]')
    if (!opcao) return
    e.preventDefault()
    irPara(opcao.dataset.todos !== undefined ? -1 : Number(opcao.dataset.i))
  })

  // Clicou fora da busca: fecha a lista
  campo.addEventListener('blur', fechar)

  // Atalho Ctrl+K / ⌘K de qualquer lugar da página
  document.addEventListener('keydown', (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key?.toLowerCase() === 'k') {
      e.preventDefault()
      campo.focus()
      campo.select()
    }
  })
}
