import { api } from './api.js'

const CHAVE = 'estoquehosp:usuario'

// Código do perfil na API -> como aparece nas telas
export const NOMES_PERFIS = {
  ALMOXARIFADO: 'Almoxarifado',
  CENTRO_CIRURGICO: 'Centro Cirúrgico',
  GESTAO: 'Gestão'
}

// Devolve o usuário salvo na sessão, ou null se não houver ou estiver corrompido
export function usuarioAtual() {
  try {
    const usuario = JSON.parse(sessionStorage.getItem(CHAVE))
    const campos = ['nome', 'perfil', 'hospital', 'unidade', 'token']
    if (usuario && campos.every((c) => typeof usuario[c] === 'string' && usuario[c].trim())) return usuario
  } catch {
    // JSON inválido: trata como sem login
  }
  sessionStorage.removeItem(CHAVE)
  return null
}

export function tokenAtual() {
  return usuarioAtual()?.token ?? null
}

// Confere matrícula e senha na API e guarda o token. Lança ErroApi se não der certo.
export async function login(matricula, senha) {
  const resposta = await api('POST', '/login', { matricula, senha })
  // hospital e unidade ainda não existem na API: valores fixos até o cadastro de hospitais
  const usuario = {
    nome: resposta.usuario.nome,
    perfil: resposta.usuario.perfil,
    hospital: 'Hospital Santa Clara',
    unidade: 'Unidade Centro',
    token: resposta.token
  }
  sessionStorage.setItem(CHAVE, JSON.stringify(usuario))
  return usuario
}

export function logout() {
  const token = tokenAtual()
  sessionStorage.removeItem(CHAVE)
  // Avisa a API para encerrar a sessão; se falhar, o token vence sozinho
  if (token) fetch('/api/logout', { method: 'POST', headers: { Authorization: `Bearer ${token}` } }).catch(() => {})
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
