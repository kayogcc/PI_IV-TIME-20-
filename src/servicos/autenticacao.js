const CHAVE = 'estoquehosp:usuario'

export function usuarioAtual() {
  try {
    return JSON.parse(sessionStorage.getItem(CHAVE))
  } catch {
    return null
  }
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