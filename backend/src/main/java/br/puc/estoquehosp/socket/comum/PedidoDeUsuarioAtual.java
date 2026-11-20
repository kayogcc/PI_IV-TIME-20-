// Gabriel Figueira Albasini
// Palavras-chave: pedido · quem é o usuário do token
package br.puc.estoquehosp.socket.comum;

// Devolve o usuário dono do token
public class PedidoDeUsuarioAtual extends PedidoAutenticado
{
    public PedidoDeUsuarioAtual (String token)
    {
        super (token);
    }

    // Não mostra senha nem token: este texto pode aparecer no terminal
    public String toString ()
    {
        return ("Usuario atual (token oculto)");
    }
}
