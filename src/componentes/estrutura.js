// Autores: Gabriel Albasini RA: 25019916, Leonardo Fonseca RA: 25024579 e Carlos Eduardo RA: 25020992 (busca do cabeçalho), Bruno Lobo de Jesus RA: 25019830 (aba de usuários só para a Gestão)

// Palavras-chave: layout comum · cabeçalho e menu de abas (filtradas por perfil) · exige login · data-href · controle segmentado
import { exigirLogin, logout } from '../servicos/autenticacao.js'
import { esc } from '../utilitarios/textos.js'
import { ligarBusca } from './busca.js'

// Abas do menu. perfis: só aparece para esses perfis (sem perfis = todos)
const menu = [
  { href: 'index.html', rotulo: 'Dashboard', ativoEm: ['index.html', ''] },
  { href: 'movimentacao.html', rotulo: 'Movimentação', ativoEm: ['movimentacao.html'] },
  { href: 'alertas.html', rotulo: 'Alertas', ativoEm: ['alertas.html'] },
  { href: 'usuarios.html', rotulo: 'Usuários', ativoEm: ['usuarios.html'], perfis: ['GESTAO'] }
]

function htmlDoCabecalho(usuario) {
  const iniciais = usuario.nome.split(' ').map((p) => p[0]).join('').slice(0, 2).toUpperCase()
  const paginaAtual = location.pathname.split('/').pop()
  const abas = menu
    .filter((m) => !m.perfis || m.perfis.includes(usuario.perfil))
    .map((m) => `<a href="${m.href}"${m.ativoEm.includes(paginaAtual) ? ' class="on"' : ''}>${m.rotulo}</a>`)
    .join('')

  return `
    <div class="bar">
      <div class="logo">
        <i><svg viewBox="0 0 24 24"><rect x="5" y="3" width="14" height="18" rx="2"/><path d="M12 8v6M9 11h6M10 21v-3h4v3"/></svg></i>
        <span>Estoque<b>Hosp</b></span>
      </div>
      <div class="search">
        <svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></svg>
        <!-- Busca: as sugestões aparecem na lista abaixo (ver busca.js). role/aria-* ajudam leitores de tela -->
        <input type="search" placeholder="Buscar no estoque..." autocomplete="off" aria-label="Buscar material no estoque"
          role="combobox" aria-autocomplete="list" aria-expanded="false" aria-controls="busca-resultados"><kbd>⌘K</kbd>
        <ul class="search-resultados" id="busca-resultados" role="listbox" hidden></ul>
      </div>
      <div class="user" id="sair" title="Sair">
        <div class="av">${esc(iniciais)}</div>
        <div><strong>${esc(usuario.hospital)}</strong><small>${esc(usuario.unidade)}</small></div>
      </div>
    </div>
    <nav>${abas}</nav>`
}

// Chame no início de cada página interna. Devolve o usuário logado
// (ou null se a pessoa foi redirecionada para o login).
export function montarLayout() {
  const usuario = exigirLogin()
  if (!usuario) return null

  document.getElementById('header').innerHTML = htmlDoCabecalho(usuario)
  // Busca do cabeçalho funcionando: sugestões enquanto digita, Ctrl+K, setas e Enter (ver busca.js)
  ligarBusca(document.querySelector('#header .search'))

  document.getElementById('sair').addEventListener('click', () => {
    logout()
    location.href = 'entrar.html'
  })

  // Qualquer elemento com data-href="pagina.html" funciona como link
  document.addEventListener('click', (e) => {
    const alvo = e.target.closest('[data-href]')
    if (alvo) location.href = alvo.dataset.href
  })

  // Controles segmentados (.seg): só um botão fica ativo
  document.querySelectorAll('.seg').forEach((grupo) => {
    grupo.addEventListener('click', (e) => {
      const botao = e.target.closest('button')
      if (!botao) return
      grupo.querySelectorAll('button').forEach((b) => b.classList.remove('on'))
      botao.classList.add('on')
    })
  })

  return usuario
}