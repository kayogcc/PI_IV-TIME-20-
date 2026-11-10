const DIAS_SEMANA = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb']

// Date -> "09/11"
export const diaMes = (d) => d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' })
// Date -> "14:05"
export const hora = (d) => d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
// Date -> "Seg"
export const diaSemana = (d) => DIAS_SEMANA[d.getDay()]

// "2026-12-01" -> "01/12/2026"
export const dataIso = (iso) => (iso ? iso.split('-').reverse().join('/') : '—')

// Os últimos 7 dias (à meia-noite), do mais antigo para hoje — mesma janela do /api/dashboard
export function ultimosSeteDias() {
  const hoje = new Date()
  hoje.setHours(0, 0, 0, 0)
  return Array.from({ length: 7 }, (_, i) => new Date(hoje.getFullYear(), hoje.getMonth(), hoje.getDate() - 6 + i))
}

// Dias de hoje até a data "yyyy-MM-dd" (negativo = já passou)
export function diasAte(iso) {
  const [ano, mes, dia] = iso.split('-').map(Number)
  const hoje = new Date()
  hoje.setHours(0, 0, 0, 0)
  return Math.round((new Date(ano, mes - 1, dia) - hoje) / 86400000)
}
