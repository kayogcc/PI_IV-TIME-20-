// Autor: Gabriel Figueira Albasini

// Palavras-chave: herança · pedidos que exigem login · carregam o token
package br.puc.estoquehosp.socket.comum;

// Pedido que exige login: leva o token devolvido pelo PedidoDeLogin.
// O servidor confere o token e o perfil do usuário antes de atender.
public class PedidoAutenticado extends Comunicado
{
    private String token;

    public PedidoAutenticado (String token)
    {
        this.token = token;
    }

    public String getToken ()
    {
        return this.token;
    }

    // Não mostra senha nem token: este texto pode aparecer no terminal
    public String toString ()
    {
        return ("Pedido autenticado (token oculto)");
    }
}
