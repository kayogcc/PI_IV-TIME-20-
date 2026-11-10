package br.puc.estoquehosp.modelo;

// Corpos JSON que a API recebe. Campos ausentes chegam como null.
public final class Entradas {

    private Entradas() {}

    public record NovoUsuario(String nome, String matricula, String perfil, String senha) {}

    public record Login(String matricula, String senha) {}

    public record DadosMaterial(String nome, String codigoBarras, String lote, String validade, Integer quantidadeMinima) {}

    // material: código de barras ou nome do material
    public record NovaMovimentacao(String material, String tipo, String origem, Integer quantidade, String local) {}
}
