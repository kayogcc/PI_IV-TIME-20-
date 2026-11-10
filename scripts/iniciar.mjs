// npm start: sobe a API Java e o front-end juntos. Ctrl+C (ou um dos dois parar) encerra os dois.
import { spawn, spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const raiz = fileURLToPath(new URL('..', import.meta.url))

const partes = [
  { nome: 'API', comando: 'mvn -q compile exec:java', pasta: `${raiz}backend` },
  { nome: 'Front-end', comando: 'npm run dev', pasta: raiz }
]

if (!process.env.MONGO_URI) {
  console.error('A variável de ambiente MONGO_URI não está definida. Veja "Configuração do Banco de Dados" no README.')
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

const filhos = partes.map(({ nome, comando, pasta }) => {
  console.log(`> Iniciando ${nome}: ${comando}`)
  const filho = spawn(comando, { cwd: pasta, shell: true, stdio: 'inherit' })
  filho.on('exit', (codigo) => {
    if (!encerrando) console.error(`\n> ${nome} parou (código ${codigo}). Encerrando o resto...`)
    encerrarTudo(codigo ?? 1)
  })
  return filho
})

process.on('SIGINT', () => encerrarTudo(0))
