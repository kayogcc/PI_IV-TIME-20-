// Autores: Leonardo Fonseca RA: 25024579 (build das páginas), Bruno Lobo de Jesus RA: 25019830 (proxy da API)

// Palavras-chave: Vite · proxy /api -> localhost:8080 · build multipágina (um HTML por tela)
import { defineConfig } from 'vite'
import { fileURLToPath } from 'node:url'

const pagina = (arquivo) => fileURLToPath(new URL(`./${arquivo}`, import.meta.url))

export default defineConfig({
  // Em desenvolvimento, as chamadas para /api vão para a API Java (mvn compile exec:java, na pasta backend)
  server: {
    proxy: {
      '/api': 'http://localhost:8080'
    }
  },
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
