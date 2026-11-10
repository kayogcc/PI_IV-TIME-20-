import { login, usuarioAtual } from '../servicos/autenticacao.js'
import { criarAviso, matriculaValida } from '../utilitarios/formulario.js'

// Já está logado? Vai direto para o dashboard.
if (usuarioAtual()) location.replace('index.html')

// O perfil não é escolhido aqui: vem da conta, definido no cadastro.
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
form.addEventListener('submit', async (e) => {
  e.preventDefault()
  aviso.limpar()

  const problema = validar()
  if (problema) {
    aviso.erro(problema.mensagem, problema.campo)
    return
  }

  const botao = document.getElementById('enter')
  botao.disabled = true
  botao.textContent = 'Entrando...'
  try {
    await login(campoMatricula.value.trim(), campoSenha.value)
    location.href = 'index.html'
  } catch (erro) {
    aviso.erro(erro.message, erro.status === 401 ? campoSenha : null)
    botao.disabled = false
    botao.textContent = 'Entrar'
  }
})
