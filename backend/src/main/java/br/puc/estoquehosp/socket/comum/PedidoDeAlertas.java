// Autor: Gabriel Albasini RA: 25019916

// Palavras-chave: pedido · lista de alertas · exige login
package br.puc.estoquehosp.socket.comum;

// Alertas de estoque baixo e validade próxima
public class PedidoDeAlertas extends PedidoAutenticado
{
    public PedidoDeAlertas (String token)
    {
        super (token);
    }

    // Não mostra senha nem token: este texto pode aparecer no terminal
    public String toString ()
    {
        return ("Alertas");
    }
}
