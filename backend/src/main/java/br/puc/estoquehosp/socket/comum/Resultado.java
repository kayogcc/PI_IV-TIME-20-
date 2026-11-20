// Gabriel Figueira Albasini
// Palavras-chave: resposta do servidor · status (igual ao HTTP) · mensagem de erro · dados Serializable
package br.puc.estoquehosp.socket.comum;

// Resposta do servidor para qualquer pedido.
// No Fazedor de Continhas levava só o valor da conta; aqui leva:
//  - status:   mesmo código do HTTP (200 ok, 201 criado, 204 sem conteúdo, 4xx erro)
//  - mensagem: texto do erro, quando não deu certo
//  - dados:    o que foi pedido (Usuario, Material, lista de materiais...), que precisa ser Serializable
public class Resultado extends Comunicado
{
    private int    status;
    private String mensagem;
    private Object dados;

    public Resultado (int status, String mensagem, Object dados)
    {
        this.status   = status;
        this.mensagem = mensagem;
        this.dados    = dados;
    }

    public int getStatus ()
    {
        return this.status;
    }

    public String getMensagem ()
    {
        return this.mensagem;
    }

    public Object getDados ()
    {
        return this.dados;
    }

    public boolean getDeuCerto ()
    {
        return this.status < 400;
    }

    public String toString ()
    {
        return (this.getDeuCerto() ? "" + this.dados : this.mensagem);
    }
}
