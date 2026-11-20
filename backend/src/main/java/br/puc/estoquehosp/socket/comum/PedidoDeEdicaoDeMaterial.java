// Autor: Gabriel Figueira Albasini

// Palavras-chave: pedido · editar material · exige login e perfil
package br.puc.estoquehosp.socket.comum;

// Edita um material (mesmos campos do cadastro)
public class PedidoDeEdicaoDeMaterial extends PedidoAutenticado
{
    private String  id;
    private String  nome;
    private String  codigoBarras;
    private String  lote;
    private String  validade;
    private Integer quantidadeMinima;

    public PedidoDeEdicaoDeMaterial (String token, String id, String nome, String codigoBarras, String lote, String validade, Integer quantidadeMinima)
    {
        super (token);
        this.id = id;
        this.nome = nome;
        this.codigoBarras = codigoBarras;
        this.lote = lote;
        this.validade = validade;
        this.quantidadeMinima = quantidadeMinima;
    }

    public String getId ()
    {
        return this.id;
    }

    public String getNome ()
    {
        return this.nome;
    }

    public String getCodigoBarras ()
    {
        return this.codigoBarras;
    }

    public String getLote ()
    {
        return this.lote;
    }

    public String getValidade ()
    {
        return this.validade;
    }

    public Integer getQuantidadeMinima ()
    {
        return this.quantidadeMinima;
    }

    // Não mostra senha nem token: este texto pode aparecer no terminal
    public String toString ()
    {
        return ("Edicao de material "+this.id+": "+this.nome+" | cod. "+this.codigoBarras+" | lote "+this.lote+" | validade "+this.validade+" | min. "+this.quantidadeMinima);
    }
}
