const CHAVE = 'estoquehosp:usuario'

// Devolve o usuário salvo na sessão, ou null se não houver ou estiver corrompido
export function usuarioAtual() {
  try {
    const usuario = JSON.parse(sessionStorage.getItem(CHAVE))
    const campos = ['nome', 'perfil', 'hospital', 'unidade']
    if (usuario && campos.every((c) => typeof usuario[c] === 'string' && usuario[c].trim())) return usuario
  } catch {
    // JSON inválido: trata como sem login
  }
  sessionStorage.removeItem(CHAVE)
  return null
}

export function login(perfil) {
  const usuario = { nome: 'Ana Souza', perfil, hospital: 'Hospital Santa Clara', unidade: 'Unidade Centro' }
  sessionStorage.setItem(CHAVE, JSON.stringify(usuario))
  return usuario
}

export function logout() {
  sessionStorage.removeItem(CHAVE)
}

// Chame no início de cada página interna. Sem login, volta para /entrar.html.
export function exigirLogin() {
  const usuario = usuarioAtual()
  if (!usuario) {
    location.replace('entrar.html')
    return null
  }
  return usuario
}