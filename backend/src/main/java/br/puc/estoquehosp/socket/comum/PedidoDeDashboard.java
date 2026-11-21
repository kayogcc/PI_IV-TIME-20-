// Autor: Gabriel Albasini RA: 25019916

// Palavras-chave: pedido · números do dashboard · últimos 7 dias
package br.puc.estoquehosp.socket.comum;

// Totais e entradas/saídas dos últimos 7 dias
public class PedidoDeDashboard extends PedidoAutenticado
{
    public PedidoDeDashboard (String token)
    {
        super (token);
    }

    // Não mostra senha nem token: este texto pode aparecer no terminal
    public String toString ()
    {
        return ("Dashboard");
    }
}
