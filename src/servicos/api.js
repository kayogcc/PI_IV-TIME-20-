import { tokenAtual, logout } from './autenticacao.js'

// Erro devolvido pela API. status = 0 quando nem chegou no servidor.
export class ErroApi extends Error {
  constructor(status, mensagem) {
    super(mensagem)
    this.status = status
  }
}

// Chama a API Java (o Vite repassa /api para http://localhost:8080, ver vite.config.mjs).
// Devolve o JSON da resposta, ou null quando não há corpo (204).
export async function api(metodo, caminho, corpo) {
  const headers = {}
  const token = tokenAtual()
  if (token) headers.Authorization = `Bearer ${token}`
  if (corpo !== undefined) headers['Content-Type'] = 'application/json'

  let resposta
  try {
    resposta = await fetch(`/api${caminho}`, {
      method: metodo,
      headers,
      body: corpo === undefined ? undefined : JSON.stringify(corpo)
    })
  } catch {
    throw new ErroApi(0, 'Não foi possível conectar ao servidor. Verifique se a API está rodando.')
  }

  const dados = resposta.status === 204 ? null : await resposta.json().catch(() => null)
  if (resposta.ok) return dados

  // Token vencido ou servidor reiniciado: volta para o login
  if (resposta.status === 401 && token) {
    logout()
    location.replace('entrar.html')
  }
  throw new ErroApi(resposta.status, dados?.mensagem || 'Não foi possível concluir a operação. Tente novamente.')
}
