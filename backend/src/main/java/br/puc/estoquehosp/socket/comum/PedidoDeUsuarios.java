// Autor: Bruno Lobo de Jesus RA: 25019830

// Palavras-chave: pedido · listar usuários (ativos e inativos) · só Gestão
package br.puc.estoquehosp.socket.comum;

// Lista todos os usuários. Só a Gestão pode pedir.
public class PedidoDeUsuarios extends PedidoAutenticado
{
    public PedidoDeUsuarios (String token)
    {
        super (token);
    }

    // Não mostra senha nem token: este texto pode aparecer no terminal
    public String toString ()
    {
        return ("Usuarios: todos");
    }
}
