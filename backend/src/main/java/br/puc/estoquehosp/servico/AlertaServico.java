package br.puc.estoquehosp.servico;

import br.puc.estoquehosp.modelo.Alerta;
import br.puc.estoquehosp.modelo.Material;
import br.puc.estoquehosp.modelo.Movimentacao;
import br.puc.estoquehosp.repositorio.MaterialRepositorio;
import br.puc.estoquehosp.repositorio.MovimentacaoRepositorio;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Alertas de estoque baixo e validade próxima, e os números do dashboard.
public class AlertaServico {

    public record DiaMovimentado(String dia, int entradas, int saidas) {}

    public record Resumo(int itensCadastrados, int estoqueBaixo, int vencendoLogo, List<DiaMovimentado> movimentacaoSemanal) {}

    public static final int DIAS_VALIDADE_PROXIMA = 30;
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");
    private static final List<String> ORDEM_PRIORIDADE = List.of("Alta", "Média", "Baixa");

    private final MaterialRepositorio materiais;
    private final MovimentacaoRepositorio movimentacoes;

    public AlertaServico(MaterialRepositorio materiais, MovimentacaoRepositorio movimentacoes) {
        this.materiais = materiais;
        this.movimentacoes = movimentacoes;
    }

    // Mais urgentes primeiro
    public List<Alerta> listar() {
        LocalDate hoje = LocalDate.now(FUSO);
        List<Alerta> alertas = new ArrayList<>();
        for (Material m : materiais.listarAtivos(null)) {
            alertaDeEstoque(m).ifPresent(alertas::add);
            alertaDeValidade(m, hoje).ifPresent(alertas::add);
        }
        alertas.sort(Comparator.comparingInt(a -> ORDEM_PRIORIDADE.indexOf(a.prioridade())));
        return alertas;
    }

    public Resumo resumo() {
        List<Material> ativos = materiais.listarAtivos(null);
        LocalDate hoje = LocalDate.now(FUSO);
        int estoqueBaixo = (int) ativos.stream().filter(m -> alertaDeEstoque(m).isPresent()).count();
        int vencendo = (int) ativos.stream().filter(m -> alertaDeValidade(m, hoje).isPresent()).count();
        return new Resumo(ativos.size(), estoqueBaixo, vencendo, ultimosSeteDias(hoje));
    }

    // Estoque baixo: quantidade total menor ou igual ao mínimo. Alta se estiver em até 25% do mínimo.
    static Optional<Alerta> alertaDeEstoque(Material m) {
        if (m.quantidadeMinima() <= 0 || m.quantidade() > m.quantidadeMinima()) return Optional.empty();
        String prioridade = m.quantidade() * 4 <= m.quantidadeMinima() ? "Alta" : "Média";
        String detalhe = "Estoque baixo · " + m.quantidade() + " un. (mín. " + m.quantidadeMinima() + ")";
        return Optional.of(new Alerta("estoque", prioridade, m.id(), m.nome(), detalhe));
    }

    // Validade próxima: vence em até 30 dias e ainda tem saldo. Alta até 7 dias (ou vencido), Média até 15.
    static Optional<Alerta> alertaDeValidade(Material m, LocalDate hoje) {
        if (m.validade() == null || m.quantidade() <= 0) return Optional.empty();
        long dias = ChronoUnit.DAYS.between(hoje, LocalDate.parse(m.validade()));
        if (dias > DIAS_VALIDADE_PROXIMA) return Optional.empty();

        String prioridade = dias <= 7 ? "Alta" : dias <= 15 ? "Média" : "Baixa";
        String quando = dias < 0 ? "vencido há " + (-dias) + (dias == -1 ? " dia" : " dias")
                : dias == 0 ? "vence hoje"
                : "vence em " + dias + (dias == 1 ? " dia" : " dias");
        String detalhe = (dias < 0 ? "Vencido · " : "Validade próxima · ") + quando;
        return Optional.of(new Alerta("validade", prioridade, m.id(), m.nome(), detalhe));
    }

    // Soma de entradas e saídas (em unidades) de cada um dos últimos 7 dias, do mais antigo para hoje
    private List<DiaMovimentado> ultimosSeteDias(LocalDate hoje) {
        LocalDate inicio = hoje.minusDays(6);
        Map<LocalDate, int[]> totais = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) totais.put(inicio.plusDays(i), new int[2]);

        Instant desde = inicio.atStartOfDay(FUSO).toInstant();
        for (Movimentacao mov : movimentacoes.listar(desde, null, Integer.MAX_VALUE)) {
            LocalDate dia = Instant.parse(mov.data()).atZone(FUSO).toLocalDate();
            int[] total = totais.get(dia);
            if (total == null) continue;
            if (mov.tipo().equals(MovimentacaoServico.ENTRADA)) total[0] += mov.quantidade();
            else total[1] += mov.quantidade();
        }

        List<DiaMovimentado> dias = new ArrayList<>();
        totais.forEach((dia, t) -> dias.add(new DiaMovimentado(dia.format(DIA_MES), t[0], t[1])));
        return dias;
    }
}
