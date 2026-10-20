import { login, usuarioAtual } from '../servicos/autenticacao.js'

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
    perfil = botao.textContent
  })
})

document.getElementById('form-login').addEventListener('submit', (e) => {
  e.preventDefault()
  login(perfil)
  location.href = 'index.html'
})
