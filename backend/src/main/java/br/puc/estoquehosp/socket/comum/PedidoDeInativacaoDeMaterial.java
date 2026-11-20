// Autor: Gabriel Figueira Albasini

// Palavras-chave: pedido · inativar material (exclusão lógica) · histórico preservado
package br.puc.estoquehosp.socket.comum;

// Inativa um material (o histórico é mantido)
public class PedidoDeInativacaoDeMaterial extends PedidoAutenticado
{
    private String  id;

    public PedidoDeInativacaoDeMaterial (String token, String id)
    {
        super (token);
        this.id = id;
    }

    public String getId ()
    {
        return this.id;
    }

    // Não mostra senha nem token: este texto pode aparecer no terminal
    public String toString ()
    {
        return ("Inativacao de material: id "+this.id);
    }
}
