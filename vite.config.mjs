import { defineConfig } from 'vite'
import { fileURLToPath } from 'node:url'

const pagina = (arquivo) => fileURLToPath(new URL(`./${arquivo}`, import.meta.url))

export default defineConfig({
  build: {
    rollupOptions: {
      input: {
        alertas: pagina('alertas.html'),
        'criar-conta': pagina('criar-conta.html'),
        entrar: pagina('entrar.html'),
        'estoque-baixo': pagina('estoque-baixo.html'),
        historico: pagina('historico.html'),
        index: pagina('index.html'),
        inicio: pagina('inicio.html'),
        'itens-cadastrados': pagina('itens-cadastrados.html'),
        'movimentacao-estoque': pagina('movimentacao-estoque.html'),
        movimentacao: pagina('movimentacao.html'),
        'vencendo-logo': pagina('vencendo-logo.html')
      }
    }
  }
})
