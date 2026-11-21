// Autor: Gabriel Albasini RA: 25019916

// Palavras-chave: pedido · detalhe de um material · saldo por local
package br.puc.estoquehosp.socket.comum;

// Detalhe de um material, com o saldo por local
public class PedidoDeMaterial extends PedidoAutenticado
{
    private String  id;

    public PedidoDeMaterial (String token, String id)
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
        return ("Material: id "+this.id);
    }
}
