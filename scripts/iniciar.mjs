// Autores: Bruno Lobo de Jesus RA: 25019830 (inicia a API e o front-end), Gabriel Albasini RA: 25019916 (inicia também o servidor de sockets)

// Palavras-chave: npm start · sobe os 3 processos (Servidor 3000, API 8080, Vite) · spawn · Ctrl+C encerra tudo
// npm start: sobe o Servidor de sockets, a API HTTP (ponte) e o front-end juntos.
// Site → API HTTP (8080) → Servidor de sockets (3000) → MongoDB.
// Ctrl+C (ou qualquer um dos três parar) encerra todos.
import { spawn, spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const raiz = fileURLToPath(new URL('..', import.meta.url))
const backend = `${raiz}backend`

// teclado: só o Servidor recebe o que é digitado (para o comando "desativar").
// Se os três lessem o teclado, o Vite pegaria as teclas e o "desativar" nunca chegaria ao Servidor.
const partes = [
  { nome: 'Servidor de sockets', comando: 'mvn -q exec:java@servidor', pasta: backend, teclado: true },
  { nome: 'API HTTP (ponte)', comando: 'mvn -q exec:java@api', pasta: backend },
  { nome: 'Front-end', comando: 'npm run dev', pasta: raiz }
]

if (!process.env.MONGO_URI) {
  console.error('A variável de ambiente MONGO_URI não está definida. Veja "Configuração do Banco de Dados" no README.')
  process.exit(1)
}

// Compila uma vez antes, para os dois programas Java não compilarem ao mesmo tempo
console.log('> Compilando o back-end: mvn -q compile')
if (spawnSync('mvn -q compile', { cwd: backend, shell: true, stdio: 'inherit' }).status !== 0) {
  console.error('A compilação do back-end falhou.')
  process.exit(1)
}

let encerrando = false

// No Windows o processo roda dentro de um cmd, então é preciso matar a árvore inteira
function parar(filho) {
  if (filho.exitCode !== null) return
  if (process.platform === 'win32') spawnSync('taskkill', ['/pid', String(filho.pid), '/T', '/F'])
  else filho.kill('SIGINT')
}

function encerrarTudo(codigo) {
  if (encerrando) return
  encerrando = true
  filhos.forEach(parar)
  process.exitCode = codigo
}

const filhos = partes.map(({ nome, comando, pasta, teclado }) => {
  console.log(`> Iniciando ${nome}: ${comando}`)
  const filho = spawn(comando, { cwd: pasta, shell: true, stdio: [teclado ? 'inherit' : 'ignore', 'inherit', 'inherit'] })
  filho.on('exit', (codigo) => {
    if (!encerrando) {
      if (teclado && codigo === 0) console.log(`\n> ${nome} desativado. Encerrando a API e o front-end...`)
      else console.error(`\n> ${nome} parou (código ${codigo}). Encerrando o resto...`)
    }
    encerrarTudo(codigo ?? 1)
  })
  return filho
})

console.log('\n> Para desligar: digite "desativar" e Enter (desliga o Servidor e avisa os clientes) ou aperte Ctrl+C.\n')

process.on('SIGINT', () => encerrarTudo(0))
