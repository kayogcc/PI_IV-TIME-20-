// Autor: Gabriel Figueira Albasini

// Palavras-chave: pedido · login com matrícula e senha · devolve token
package br.puc.estoquehosp.socket.comum;

// Confere matrícula e senha. O Resultado traz o token, que vai nos próximos pedidos
public class PedidoDeLogin extends Comunicado
{
    private String  matricula;
    private String  senha;

    public PedidoDeLogin (String matricula, String senha)
    {
        this.matricula = matricula;
        this.senha = senha;
    }

    public String getMatricula ()
    {
        return this.matricula;
    }

    public String getSenha ()
    {
        return this.senha;
    }

    // Não mostra senha nem token: este texto pode aparecer no terminal
    public String toString ()
    {
        return ("Login: matricula "+this.matricula+" (senha oculta)");
    }
}
