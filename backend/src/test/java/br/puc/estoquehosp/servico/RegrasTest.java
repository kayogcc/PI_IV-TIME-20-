package br.puc.estoquehosp.servico;

import br.puc.estoquehosp.modelo.Alerta;
import br.puc.estoquehosp.modelo.Material;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Regras que não precisam do banco: hash de senha e cálculo dos alertas.
class RegrasTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 18);

    private static Material material(int quantidade, int minimo, String validade) {
        return new Material("1", "Gaze estéril", null, null, validade, minimo, Map.of("Almoxarifado A", quantidade), quantidade, true);
    }

    @Test
    void senhaConfereSoComASenhaCerta() {
        String hash = Senhas.gerarHash("senha123");
        assertTrue(Senhas.confere("senha123", hash));
        assertFalse(Senhas.confere("senha124", hash));
        assertNotEquals(hash, Senhas.gerarHash("senha123"), "cada hash deve usar um sal diferente");
    }

    @Test
    void estoqueBaixoSoQuandoChegaNoMinimo() {
        assertTrue(AlertaServico.alertaDeEstoque(material(121, 120, null)).isEmpty());
        assertEquals("Média", AlertaServico.alertaDeEstoque(material(60, 120, null)).orElseThrow().prioridade());
        assertEquals("Alta", AlertaServico.alertaDeEstoque(material(4, 30, null)).orElseThrow().prioridade());
    }

    @Test
    void validadeProximaPorFaixaDeDias() {
        assertTrue(AlertaServico.alertaDeValidade(material(10, 0, "2026-11-30"), HOJE).isEmpty());
        assertEquals("Baixa", AlertaServico.alertaDeValidade(material(10, 0, "2026-11-11"), HOJE).orElseThrow().prioridade());
        assertEquals("Média", AlertaServico.alertaDeValidade(material(10, 0, "2026-10-28"), HOJE).orElseThrow().prioridade());

        Alerta vencido = AlertaServico.alertaDeValidade(material(10, 0, "2026-10-16"), HOJE).orElseThrow();
        assertEquals("Alta", vencido.prioridade());
        assertEquals("Vencido · vencido há 2 dias", vencido.detalhe());
    }

    @Test
    void materialSemSaldoNaoGeraAlertaDeValidade() {
        assertTrue(AlertaServico.alertaDeValidade(material(0, 0, "2026-10-20"), HOJE).isEmpty());
    }
}
