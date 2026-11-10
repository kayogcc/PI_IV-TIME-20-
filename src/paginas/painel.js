import { montarLayout } from '../componentes/estrutura.js'
import { desenharGrafico } from '../componentes/grafico.js'
import { NIVEL_PRIORIDADE } from '../componentes/selo.js'
import { api } from '../servicos/api.js'
import { diaMes, diaSemana, hora, ultimosSeteDias } from '../utilitarios/datas.js'
import { esc } from '../utilitarios/textos.js'

const usuario = montarLayout()

if (usuario) {
  const $ = (id) => document.getElementById(id)
  const corNivel = { ex: 'var(--red)', lo: 'var(--amber)', in: 'var(--blue)' }
  const vazio = (texto) => `<li><div><small>${esc(texto)}</small></div></li>`

  $('primeiro-nome').textContent = usuario.nome.split(' ')[0]

  async function carregar() {
    let resumo, alertas, movimentacoes
    try {
      [resumo, alertas, movimentacoes] = await Promise.all([
        api('GET', '/dashboard'),
        api('GET', '/alertas'),
        api('GET', '/movimentacoes?dias=30')
      ])
    } catch (erro) {
      $('l-alertas').innerHTML = vazio(erro.message)
      $('l-mov').innerHTML = vazio(erro.message)
      return
    }

    $('atualizado').textContent = `Atualizado às ${hora(new Date())}`
    $('res-itens').textContent = resumo.itensCadastrados.toLocaleString('pt-BR')
    $('res-baixo').textContent = resumo.estoqueBaixo
    $('res-venc').textContent = resumo.vencendoLogo

    // A API manda os mesmos 7 dias, do mais antigo para hoje
    const semana = ultimosSeteDias()
    desenharGrafico('chart', 'days', resumo.movimentacaoSemanal.map((d, i) => ({ ...d, rotulo: diaSemana(semana[i]) })))

    $('l-alertas').innerHTML = alertas.slice(0, 4).map((a) => {
      const nivel = NIVEL_PRIORIDADE[a.prioridade]
      return `
      <li>
        <a class="alerta-link" href="${a.tipoAlerta === 'estoque' ? 'estoque-baixo.html' : 'vencendo-logo.html'}" aria-label="${esc(`${a.titulo}: ${a.detalhe}`)}" title="Ver itens relacionados">
          <div class="dot ${nivel}">!</div>
          <div><b>${esc(a.titulo)}</b><small>${esc(a.detalhe)}</small></div>
          <span class="tag" style="color:${corNivel[nivel]}">${esc(a.prioridade)}</span>
        </a>
      </li>`
    }).join('') || vazio('Nenhum alerta no momento.')

    $('l-mov').innerHTML = movimentacoes.slice(0, 3).map((h) => {
      const quando = new Date(h.data)
      return `
      <li>
        <div class="dot ${h.tipo === 'Entrada' ? 'in' : 'nt'}">${h.tipo === 'Entrada' ? '↓' : '↑'}</div>
        <div><b>${esc(h.material)} — ${esc(h.quantidade)} un.</b><small>${esc(h.tipo)} · ${esc(h.origem)} · ${esc(h.local)}</small></div>
        <span class="tag" style="color:var(--mute)">${esc(`${diaMes(quando)} ${hora(quando)}`)}</span>
      </li>`
    }).join('') || vazio('Nenhuma movimentação nos últimos 30 dias.')
  }

  carregar()
}
