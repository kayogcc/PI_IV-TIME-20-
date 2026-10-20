import { montarLayout } from '../componentes/estrutura.js'
import { desenharGrafico } from '../componentes/grafico.js'
import { esc } from '../utilitarios/textos.js'
import { alertas, historico, movimentacaoSemanal, resumo } from '../dados/dadosExemplo.js'

const usuario = montarLayout()

if (usuario) {
  const $ = (id) => document.getElementById(id)
  const corNivel = { ex: 'var(--red)', lo: 'var(--amber)', in: 'var(--blue)' }

  $('primeiro-nome').textContent = usuario.nome.split(' ')[0]
  $('res-itens').textContent = resumo.itensCadastrados
  $('res-cresc').textContent = resumo.crescimentoMes
  $('res-baixo').textContent = resumo.estoqueBaixo
  $('res-venc').textContent = resumo.vencendoLogo

  desenharGrafico('chart', 'days', movimentacaoSemanal.entradas, movimentacaoSemanal.saidas)

  $('l-alertas').innerHTML = alertas.slice(0, 4).map((a) => `
    <li>
      <div class="dot ${a.nivel}">!</div>
      <div><b>${esc(a.titulo)}</b><small>${esc(a.detalhe)}</small></div>
      <span class="tag" style="color:${corNivel[a.nivel]}">${a.prioridade}</span>
    </li>`).join('')

  $('l-mov').innerHTML = historico.slice(0, 3).map((h) => `
    <li>
      <div class="dot ${h.tipo === 'Entrada' ? 'in' : 'nt'}">${h.tipo === 'Entrada' ? '↓' : '↑'}</div>
      <div><b>${esc(h.material)} — ${h.quantidade} un.</b><small>${h.tipo} · ${esc(h.origem)} · ${esc(h.local)}</small></div>
      <span class="tag" style="color:var(--mute)">${h.data.split(' ')[1]}</span>
    </li>`).join('')
}