// Gabriel Figueira Albasini
package br.puc.estoquehosp.socket.comum;

// Lista os materiais ativos. busca: parte do nome ou código de barras (null = todos)
public class PedidoDeMateriais extends PedidoAutenticado
{
    private String  busca;

    public PedidoDeMateriais (String token, String busca)
    {
        super (token);
        this.busca = busca;
    }

    public String getBusca ()
    {
        return this.busca;
    }

    // Não mostra senha nem token: este texto pode aparecer no terminal
    public String toString ()
    {
        return ("Materiais: "+(this.busca==null ? "todos" : "busca \""+this.busca+"\""));
    }
}
