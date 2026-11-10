package br.puc.estoquehosp.servico;

import br.puc.estoquehosp.http.ErroApi;
import br.puc.estoquehosp.modelo.Entradas.NovaMovimentacao;
import br.puc.estoquehosp.modelo.Material;
import br.puc.estoquehosp.modelo.Movimentacao;
import br.puc.estoquehosp.modelo.Perfil;
import br.puc.estoquehosp.modelo.Usuario;
import br.puc.estoquehosp.repositorio.MaterialRepositorio;
import br.puc.estoquehosp.repositorio.MovimentacaoRepositorio;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

// Entradas e saídas de estoque. Toda alteração de saldo passa por aqui e gera um registro no histórico.
public class MovimentacaoServico {

    public static final String ENTRADA = "Entrada";
    public static final String SAIDA = "Saída";
    public static final List<String> TIPOS = List.of(ENTRADA, SAIDA);
    public static final List<String> ORIGENS = List.of("Código de barras", "Manual");
    public static final List<String> LOCAIS = List.of("Almoxarifado A", "Almoxarifado B", "Centro Cirúrgico");

    private static final int QUANTIDADE_MAXIMA = 100_000;
    private static final int DIAS_PADRAO = 30;
    private static final int DIAS_MAXIMO = 365;
    private static final int LIMITE_REGISTROS = 500;

    private final MaterialRepositorio materiais;
    private final MovimentacaoRepositorio movimentacoes;

    public MovimentacaoServico(MaterialRepositorio materiais, MovimentacaoRepositorio movimentacoes) {
        this.materiais = materiais;
        this.movimentacoes = movimentacoes;
    }

    public Movimentacao registrar(NovaMovimentacao dados, Usuario usuario) {
        String textoMaterial = dados.material() == null ? "" : dados.material().trim();
        Integer quantidade = dados.quantidade();

        if (!TIPOS.contains(dados.tipo())) throw ErroApi.requisicaoInvalida("Tipo inválido. Use Entrada ou Saída.");
        if (!ORIGENS.contains(dados.origem())) throw ErroApi.requisicaoInvalida("Origem inválida. Use Código de barras ou Manual.");
        if (textoMaterial.isEmpty()) throw ErroApi.requisicaoInvalida("Informe o material (código de barras ou nome).");
        if (quantidade == null || quantidade <= 0 || quantidade > QUANTIDADE_MAXIMA) {
            throw ErroApi.requisicaoInvalida("A quantidade deve ser um número inteiro de 1 a " + QUANTIDADE_MAXIMA + ".");
        }
        if (!LOCAIS.contains(dados.local())) throw ErroApi.requisicaoInvalida("Selecione um local válido.");
        if (usuario.perfil() == Perfil.CENTRO_CIRURGICO && dados.tipo().equals(ENTRADA)) {
            throw new ErroApi(403, "O perfil Centro Cirúrgico só pode registrar saídas.");
        }

        Material material = materiais.buscarAtivoPorCodigoOuNome(textoMaterial)
                .orElseThrow(() -> ErroApi.naoEncontrado("Material não encontrado: " + textoMaterial));

        int delta = dados.tipo().equals(SAIDA) ? -quantidade : quantidade;
        if (!materiais.ajustarSaldo(material.id(), dados.local(), delta)) {
            int saldo = material.saldos().getOrDefault(dados.local(), 0);
            throw ErroApi.conflito("Saldo insuficiente: há " + saldo + " un. de " + material.nome() + " em " + dados.local() + ".");
        }

        try {
            return movimentacoes.inserir(Instant.now(), material.id(), material.nome(), dados.tipo(), quantidade,
                    dados.origem(), dados.local(), usuario.nome());
        } catch (RuntimeException e) {
            // Não conseguiu gravar o histórico: desfaz o saldo para os dois não ficarem diferentes
            materiais.ajustarSaldo(material.id(), dados.local(), -delta);
            throw e;
        }
    }

    public List<Movimentacao> listar(String dias, String materialId) {
        int periodo = DIAS_PADRAO;
        if (dias != null && !dias.isBlank()) {
            try {
                periodo = Integer.parseInt(dias);
            } catch (NumberFormatException e) {
                periodo = -1;
            }
            if (periodo < 1 || periodo > DIAS_MAXIMO) {
                throw ErroApi.requisicaoInvalida("O parâmetro dias deve ser um número de 1 a " + DIAS_MAXIMO + ".");
            }
        }
        Instant desde = Instant.now().minus(Duration.ofDays(periodo));
        return movimentacoes.listar(desde, materialId, LIMITE_REGISTROS);
    }
}
