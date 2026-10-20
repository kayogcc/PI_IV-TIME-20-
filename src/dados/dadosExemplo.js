//Dados de Exemplo(ficticios) somente para deixar uma base mais estabilizada

export const diasSemana = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom']

// Indicadores do dashboard
export const resumo = {
  itensCadastrados: '1.248',
  crescimentoMes: '+8,4% neste mês',
  estoqueBaixo: 23,
  vencendoLogo: 12
}

// Datas dos últimos 7 dias, na mesma ordem de diasSemana
export const datasSemana = ['12/10', '13/10', '14/10', '15/10', '16/10', '17/10', '18/10']

// Movimentações dos últimos 7 dias (base da tela "Movimentação do estoque")
export const movimentacoesSemana = [
  { data: '12/10 08:10', material: 'Máscara cirúrgica', tipo: 'Entrada', quantidade: 200, origem: 'Código de barras', local: 'Almoxarifado A' },
  { data: '12/10 10:35', material: 'Seringa 10 ml', tipo: 'Saída', quantidade: 60, origem: 'Manual', local: 'Almoxarifado A' },
  { data: '12/10 15:20', material: 'Luva cirúrgica M', tipo: 'Saída', quantidade: 40, origem: 'Código de barras', local: 'Centro Cirúrgico' },
  { data: '13/10 09:05', material: 'Gaze estéril', tipo: 'Entrada', quantidade: 120, origem: 'Código de barras', local: 'Almoxarifado B' },
  { data: '13/10 11:40', material: 'Máscara cirúrgica', tipo: 'Saída', quantidade: 80, origem: 'Manual', local: 'Almoxarifado A' },
  { data: '13/10 16:55', material: 'Fio de sutura 3-0', tipo: 'Saída', quantidade: 25, origem: 'Código de barras', local: 'Centro Cirúrgico' },
  { data: '14/10 08:30', material: 'Seringa 10 ml', tipo: 'Entrada', quantidade: 300, origem: 'Código de barras', local: 'Almoxarifado A' },
  { data: '14/10 13:15', material: 'Soro fisiológico 500 ml', tipo: 'Entrada', quantidade: 60, origem: 'Manual', local: 'Almoxarifado B' },
  { data: '14/10 17:00', material: 'Gaze estéril', tipo: 'Saída', quantidade: 70, origem: 'Código de barras', local: 'Centro Cirúrgico' },
  { data: '15/10 07:50', material: 'Luva cirúrgica M', tipo: 'Entrada', quantidade: 150, origem: 'Código de barras', local: 'Almoxarifado A' },
  { data: '15/10 12:25', material: 'Seringa 10 ml', tipo: 'Saída', quantidade: 110, origem: 'Código de barras', local: 'Almoxarifado A' },
  { data: '15/10 18:10', material: 'Campo cirúrgico', tipo: 'Saída', quantidade: 30, origem: 'Manual', local: 'Centro Cirúrgico' },
  { data: '16/10 08:45', material: 'Cateter venoso 20G', tipo: 'Entrada', quantidade: 50, origem: 'Manual', local: 'Almoxarifado B' },
  { data: '16/10 10:00', material: 'Máscara cirúrgica', tipo: 'Entrada', quantidade: 250, origem: 'Código de barras', local: 'Almoxarifado A' },
  { data: '16/10 14:30', material: 'Soro fisiológico 500 ml', tipo: 'Saída', quantidade: 40, origem: 'Código de barras', local: 'Centro Cirúrgico' },
  { data: '16/10 19:05', material: 'Luva cirúrgica M', tipo: 'Saída', quantidade: 90, origem: 'Código de barras', local: 'Centro Cirúrgico' },
  { data: '17/10 09:30', material: 'Campo cirúrgico', tipo: 'Entrada', quantidade: 40, origem: 'Manual', local: 'Almoxarifado B' },
  { data: '17/10 15:45', material: 'Máscara cirúrgica', tipo: 'Saída', quantidade: 60, origem: 'Código de barras', local: 'Almoxarifado A' },
  { data: '18/10 08:20', material: 'Fio de sutura 3-0', tipo: 'Entrada', quantidade: 80, origem: 'Código de barras', local: 'Almoxarifado B' },
  { data: '18/10 11:10', material: 'Cateter venoso 20G', tipo: 'Saída', quantidade: 26, origem: 'Manual', local: 'Centro Cirúrgico' },
  { data: '18/10 16:40', material: 'Gaze estéril', tipo: 'Saída', quantidade: 50, origem: 'Código de barras', local: 'Centro Cirúrgico' }
]

// Soma de entradas e saídas (em unidades) de cada dia da semana
export const totaisPorDia = datasSemana.map((dia) => {
  const doDia = movimentacoesSemana.filter((m) => m.data.startsWith(dia))
  const somar = (tipo) => doDia.filter((m) => m.tipo === tipo).reduce((t, m) => t + m.quantidade, 0)
  return { dia, entradas: somar('Entrada'), saidas: somar('Saída') }
})

// Gráfico: volume diário em % do maior dia (deixa uma folga no topo)
const maiorDia = Math.max(...totaisPorDia.flatMap((d) => [d.entradas, d.saidas]))
const emPercentual = (v) => Math.round((v / maiorDia) * 90)

export const movimentacaoSemanal = {
  entradas: totaisPorDia.map((d) => emPercentual(d.entradas)),
  saidas: totaisPorDia.map((d) => emPercentual(d.saidas))
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