// Autor: Bruno Lobo de Jesus RA: 25019830

// Palavras-chave: tela de usuários · só Gestão · GET/PATCH /api/usuarios · mudar perfil · inativar/reativar · busca
import { montarLayout } from '../componentes/estrutura.js'
import { api } from '../servicos/api.js'
import { NOMES_PERFIS } from '../servicos/autenticacao.js'
import { criarAviso } from '../utilitarios/formulario.js'
import { esc, normalizar } from '../utilitarios/textos.js'

const usuario = montarLayout()
// Só a Gestão entra aqui (a API também confere e responde 403 para os outros perfis)
if (usuario && usuario.perfil !== 'GESTAO') location.replace('index.html')
else if (usuario) {
  const $ = (id) => document.getElementById(id)
  const secao = $('t-usuarios').closest('section')
  const aviso = criarAviso(secao, secao.querySelector('.tabela-venc'))
  const TOTAL_COLUNAS = 5

  let usuarios = []

  // A própria conta não pode ser alterada (a API também recusa): assim a Gestão não perde o acesso
  const ehVoce = (u) => u.id === usuario.id

  function seletorDePerfil(u) {
    const opcoes = Object.entries(NOMES_PERFIS)
      .map(([codigo, nome]) => `<option value="${esc(codigo)}"${codigo === u.perfil ? ' selected' : ''}>${esc(nome)}</option>`)
      .join('')
    return `<select class="field perfil-usuario" data-id="${esc(u.id)}" aria-label="Perfil de ${esc(u.nome)}"${ehVoce(u) ? ' disabled' : ''}>${opcoes}</select>`
  }

  function botaoDoUsuario(u) {
    if (ehVoce(u)) return ''
    return u.ativo
      ? `<button type="button" class="btn-texto perigo" data-acao="inativar" data-id="${esc(u.id)}">Inativar</button>`
      : `<button type="button" class="btn-texto" data-acao="reativar" data-id="${esc(u.id)}">Reativar</button>`
  }

  function renderizar() {
    // Ignora maiúsculas, acentos e espaços extras, como na tela de itens
    const termo = normalizar($('busca-usuarios').value)
    const filtrados = usuarios.filter((u) =>
      !termo || normalizar(u.nome).includes(termo) || normalizar(u.matricula).includes(termo)
    )
    const ativos = usuarios.filter((u) => u.ativo).length

    $('qtd-usuarios').textContent =
      `${filtrados.length} ${filtrados.length === 1 ? 'usuário encontrado' : 'usuários encontrados'} · ${ativos} ${ativos === 1 ? 'ativo' : 'ativos'}`
    $('t-usuarios').innerHTML = filtrados.length
      ? filtrados.map((u) => `
          <tr${u.ativo ? '' : ' class="inativo"'}>
            <td>${esc(u.nome)}${ehVoce(u) ? ' <small>(você)</small>' : ''}</td>
            <td>${esc(u.matricula)}</td>
            <td>${seletorDePerfil(u)}</td>
            <td><span class="tag" style="color:${u.ativo ? 'var(--green)' : 'var(--mute)'}">${u.ativo ? 'Ativa' : 'Inativa'}</span></td>
            <td><div class="acoes-item">${botaoDoUsuario(u)}</div></td>
          </tr>`).join('')
      : `<tr><td class="vazio" colspan="${TOTAL_COLUNAS}">Nenhum usuário encontrado.</td></tr>`
  }

  async function carregar() {
    try {
      usuarios = await api('GET', '/usuarios')
      renderizar()
    } catch (erro) {
      $('qtd-usuarios').textContent = ''
      $('t-usuarios').innerHTML = `<tr><td class="vazio" colspan="${TOTAL_COLUNAS}">${esc(erro.message)}</td></tr>`
    }
  }

  // Manda a alteração para a API. Se der errado, recarrega para a tela voltar ao que está no banco.
  async function alterar(u, dados, mensagemDeSucesso) {
    aviso.limpar()
    try {
      await api('PATCH', `/usuarios/${encodeURIComponent(u.id)}`, dados)
      aviso.sucesso(mensagemDeSucesso)
    } catch (erro) {
      aviso.erro(erro.message)
    }
    await carregar()
  }

  $('t-usuarios').addEventListener('change', (e) => {
    const seletor = e.target.closest('select.perfil-usuario')
    if (!seletor) return
    const u = usuarios.find((i) => i.id === seletor.dataset.id)
    if (!u) return

    const novo = NOMES_PERFIS[seletor.value]
    if (!confirm(`Mudar o perfil de ${u.nome} de ${NOMES_PERFIS[u.perfil]} para ${novo}?\n\nSe a pessoa estiver logada, ela precisa entrar de novo.`)) {
      seletor.value = u.perfil
      return
    }
    alterar(u, { perfil: seletor.value }, `${u.nome} agora tem o perfil ${novo}.`)
  })

  $('t-usuarios').addEventListener('click', (e) => {
    const botao = e.target.closest('button[data-acao]')
    if (!botao) return
    const u = usuarios.find((i) => i.id === botao.dataset.id)
    if (!u) return

    if (botao.dataset.acao === 'inativar') {
      if (!confirm(`Inativar a conta de ${u.nome}?\n\nA pessoa sai do sistema e não consegue mais entrar. O histórico de movimentações continua guardado.`)) return
      alterar(u, { ativo: false }, `Conta de ${u.nome} inativada.`)
    } else {
      alterar(u, { ativo: true }, `Conta de ${u.nome} reativada.`)
    }
  })

  $('busca-usuarios').addEventListener('input', renderizar)
  carregar()
}
