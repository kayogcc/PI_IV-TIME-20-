import { login, usuarioAtual } from '../servicos/autenticacao.js'
import { criarAviso, matriculaValida } from '../utilitarios/formulario.js'

// Já está logado? Vai direto para o dashboard.
if (usuarioAtual()) location.replace('index.html')

// ⚠️ Protótipo: o seletor de perfil existe só na demonstração.
// No sistema real o perfil vem da conta, definido pela Gestão (ver servicos/autenticacao.js).
let perfil = 'Almoxarifado'
const botoes = document.querySelectorAll('.role')

botoes.forEach((botao) => {
  botao.addEventListener('click', () => {
    botoes.forEach((b) => b.classList.remove('on'))
    botao.classList.add('on')
    perfil = botao.textContent.trim()
  })
})

const form = document.getElementById('form-login')
const campoMatricula = document.getElementById('mat')
const campoSenha = document.getElementById('pw')
const aviso = criarAviso(form, document.getElementById('enter'))

function validar() {
  if (!campoMatricula.value.trim()) {
    return { mensagem: 'Informe seu usuário ou matrícula.', campo: campoMatricula }
  }
  if (!matriculaValida(campoMatricula.value.trim())) {
    return { mensagem: 'Usuário ou matrícula inválido. Não use espaços nem símbolos.', campo: campoMatricula }
  }
  if (!campoSenha.value) {
    return { mensagem: 'Informe sua senha.', campo: campoSenha }
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

  // TODO: conferir matrícula e senha na API antes de entrar (ex.: POST /api/login)
  login(perfil)
  location.href = 'index.html'
})
