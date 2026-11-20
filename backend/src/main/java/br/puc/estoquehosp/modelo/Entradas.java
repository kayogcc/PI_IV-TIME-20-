// Autores: Bruno Lobo(dados de entrada da API), Gabriel Figueira Albasini (senha oculta no toString)

// Palavras-chave: DTOs · corpos JSON recebidos · toString esconde a senha
package br.puc.estoquehosp.modelo;

// Corpos JSON que a API recebe. Campos ausentes chegam como null.
public final class Entradas {

    private Entradas() {}

    // Nos dois records com senha, o toString é reescrito para a senha não aparecer no terminal
    public record NovoUsuario(String nome, String matricula, String perfil, String senha) {
        @Override
        public String toString() {
            return "NovoUsuario[nome=" + nome + ", matricula=" + matricula + ", perfil=" + perfil + ", senha=(oculta)]";
        }
    }

    public record Login(String matricula, String senha) {
        @Override
        public String toString() {
            return "Login[matricula=" + matricula + ", senha=(oculta)]";
        }
    }

    public record DadosMaterial(String nome, String codigoBarras, String lote, String validade, Integer quantidadeMinima) {}

    // material: código de barras ou nome do material
    public record NovaMovimentacao(String material, String tipo, String origem, Integer quantidade, String local) {}
}
