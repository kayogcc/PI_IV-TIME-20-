// Autor: Gabriel Albasini RA: 25019916

// Palavras-chave: pedido · encerra a sessão · token oculto no toString
package br.puc.estoquehosp.socket.comum;

// Encerra a sessão do token
public class PedidoDeLogout extends PedidoAutenticado
{
    public PedidoDeLogout (String token)
    {
        super (token);
    }

    // Não mostra senha nem token: este texto pode aparecer no terminal
    public String toString ()
    {
        return ("Logout (token oculto)");
    }
}
