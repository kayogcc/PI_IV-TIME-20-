// Autor: Bruno Lobo de Jesus RA: 25019830

// Palavras-chave: pedido · mudar perfil · inativar/reativar conta · só Gestão
package br.puc.estoquehosp.socket.comum;

// Muda o perfil e/ou ativa ou inativa uma conta. perfil e ativo null = não muda. Só a Gestão pode pedir.
public class PedidoDeAlteracaoDeUsuario extends PedidoAutenticado
{
    private String  id;
    private String  perfil;
    private Boolean ativo;

    public PedidoDeAlteracaoDeUsuario (String token, String id, String perfil, Boolean ativo)
    {
        super (token);
        this.id = id;
        this.perfil = perfil;
        this.ativo = ativo;
    }

    public String getId ()
    {
        return this.id;
    }

    public String getPerfil ()
    {
        return this.perfil;
    }

    public Boolean getAtivo ()
    {
        return this.ativo;
    }

    // Não mostra senha nem token: este texto pode aparecer no terminal
    public String toString ()
    {
        return ("Alteracao de usuario: id "+this.id+" | perfil "+this.perfil+" | ativo "+this.ativo);
    }
}
