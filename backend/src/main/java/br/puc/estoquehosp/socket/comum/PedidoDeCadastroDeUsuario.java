// Autor: Gabriel Figueira Albasini

// Palavras-chave: pedido · criar conta · não exige login · toString sem senha
package br.puc.estoquehosp.socket.comum;

// Cria uma conta. perfil: ALMOXARIFADO, CENTRO_CIRURGICO ou GESTAO
public class PedidoDeCadastroDeUsuario extends Comunicado
{
    private String  nome;
    private String  matricula;
    private String  perfil;
    private String  senha;

    public PedidoDeCadastroDeUsuario (String nome, String matricula, String perfil, String senha)
    {
        this.nome = nome;
        this.matricula = matricula;
        this.perfil = perfil;
        this.senha = senha;
    }

    public String getNome ()
    {
        return this.nome;
    }

    public String getMatricula ()
    {
        return this.matricula;
    }

    public String getPerfil ()
    {
        return this.perfil;
    }

    public String getSenha ()
    {
        return this.senha;
    }

    // Não mostra senha nem token: este texto pode aparecer no terminal
    public String toString ()
    {
        return ("Cadastro de usuario: "+this.nome+" | matricula "+this.matricula+" | perfil "+this.perfil+" (senha oculta)");
    }
}
