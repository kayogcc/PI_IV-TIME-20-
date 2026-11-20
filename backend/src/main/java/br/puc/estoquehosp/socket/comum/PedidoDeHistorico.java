// Gabriel Figueira Albasini
package br.puc.estoquehosp.socket.comum;

// Movimentações dos últimos dias (null = 30). materialId null = todos os materiais
public class PedidoDeHistorico extends PedidoAutenticado
{
    private String  dias;
    private String  materialId;

    public PedidoDeHistorico (String token, String dias, String materialId)
    {
        super (token);
        this.dias = dias;
        this.materialId = materialId;
    }

    public String getDias ()
    {
        return this.dias;
    }

    public String getMaterialId ()
    {
        return this.materialId;
    }

    // Não mostra senha nem token: este texto pode aparecer no terminal
    public String toString ()
    {
        return ("Historico: "+(this.dias==null ? "30" : this.dias)+" dias"+(this.materialId==null ? "" : " | material "+this.materialId));
    }
}
