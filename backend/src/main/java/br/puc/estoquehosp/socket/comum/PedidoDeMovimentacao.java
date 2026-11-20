// Autor: Gabriel Figueira Albasini

// Palavras-chave: pedido · entrada ou saída de estoque · código de barras ou nome
package br.puc.estoquehosp.socket.comum;

// Entrada ou saída de estoque. material: código de barras ou nome; tipo: Entrada | Saída;
// origem: Código de barras | Manual; local: Almoxarifado A | Almoxarifado B | Centro Cirúrgico
public class PedidoDeMovimentacao extends PedidoAutenticado
{
    private String  material;
    private String  tipo;
    private String  origem;
    private Integer quantidade;
    private String  local;

    public PedidoDeMovimentacao (String token, String material, String tipo, String origem, Integer quantidade, String local)
    {
        super (token);
        this.material = material;
        this.tipo = tipo;
        this.origem = origem;
        this.quantidade = quantidade;
        this.local = local;
    }

    public String getMaterial ()
    {
        return this.material;
    }

    public String getTipo ()
    {
        return this.tipo;
    }

    public String getOrigem ()
    {
        return this.origem;
    }

    public Integer getQuantidade ()
    {
        return this.quantidade;
    }

    public String getLocal ()
    {
        return this.local;
    }

    // Não mostra senha nem token: este texto pode aparecer no terminal
    public String toString ()
    {
        return ("Movimentacao: "+this.tipo+" de "+this.quantidade+" un. de "+this.material+" | "+this.local+" | "+this.origem);
    }
}
