// Gabriel Figueira Albasini
// Palavras-chave: pedido · cadastrar material · exige login e perfil
package br.puc.estoquehosp.socket.comum;

// Cadastra um material. validade no formato AAAA-MM-DD
public class PedidoDeCadastroDeMaterial extends PedidoAutenticado
{
    private String  nome;
    private String  codigoBarras;
    private String  lote;
    private String  validade;
    private Integer quantidadeMinima;

    public PedidoDeCadastroDeMaterial (String token, String nome, String codigoBarras, String lote, String validade, Integer quantidadeMinima)
    {
        super (token);
        this.nome = nome;
        this.codigoBarras = codigoBarras;
        this.lote = lote;
        this.validade = validade;
        this.quantidadeMinima = quantidadeMinima;
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
        return ("Cadastro de material: "+this.nome+" | cod. "+this.codigoBarras+" | lote "+this.lote+" | validade "+this.validade+" | min. "+this.quantidadeMinima);
    }
}
