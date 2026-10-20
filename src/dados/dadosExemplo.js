//Dados de Exemplo(ficticios) somente para deixar uma base mais estabilizada

export const diasSemana = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom']

// Indicadores do dashboard
export const resumo = {
  itensCadastrados: '1.248',
  crescimentoMes: '+8,4% neste mês',
  estoqueBaixo: 23,
  vencendoLogo: 12
}

// Gráfico do dashboard: volume diário em % (últimos 7 dias)
export const movimentacaoSemanal = {
  entradas: [62, 48, 80, 55, 90, 40, 70],
  saidas: [45, 60, 52, 70, 58, 35, 50]
}

// tipo: Entrada | Saída — origem: Código de barras | Manual
export const historico = [
  { data: '06/10 09:42', material: 'Máscara cirúrgica', tipo: 'Entrada', quantidade: 200, origem: 'Código de barras', local: 'Almoxarifado A' },
  { data: '06/10 09:15', material: 'Fio de sutura 3-0', tipo: 'Saída', quantidade: 15, origem: 'Código de barras', local: 'Centro Cirúrgico' },
  { data: '06/10 08:50', material: 'Campo cirúrgico', tipo: 'Saída', quantidade: 8, origem: 'Manual', local: 'Centro Cirúrgico' }
]

// nivel: ex = alta, lo = média, in = baixa — tipoAlerta: estoque | validade
export const alertas = [
  { nivel: 'ex', titulo: 'Luva cirúrgica M', detalhe: 'Validade próxima · vence em 6 dias', prioridade: 'Alta', tipoAlerta: 'validade' },
  { nivel: 'ex', titulo: 'Cateter venoso 20G', detalhe: 'Estoque baixo · 4 un. (mín. 30)', prioridade: 'Alta', tipoAlerta: 'estoque' },
  { nivel: 'lo', titulo: 'Seringa 10 ml', detalhe: 'Estoque baixo · 85 un. (mín. 150)', prioridade: 'Média', tipoAlerta: 'estoque' },
  { nivel: 'lo', titulo: 'Gaze estéril', detalhe: 'Estoque baixo · 60 un. (mín. 120)', prioridade: 'Média', tipoAlerta: 'estoque' },
  { nivel: 'in', titulo: 'Soro fisiológico 500 ml', detalhe: 'Validade próxima · vence em 24 dias', prioridade: 'Baixa', tipoAlerta: 'validade' }
]