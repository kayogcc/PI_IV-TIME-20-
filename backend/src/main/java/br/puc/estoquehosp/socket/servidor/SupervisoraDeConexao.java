// Gabriel Figueira Albasini
// Palavras-chave: Thread por cliente · ObjectInputFilter (lista branca contra desserialização maliciosa) · atenda · confere token e perfil (exija)
package br.puc.estoquehosp.socket.servidor;

import br.puc.estoquehosp.http.ErroApi;
import br.puc.estoquehosp.modelo.Entradas.DadosMaterial;
import br.puc.estoquehosp.modelo.Entradas.Login;
import br.puc.estoquehosp.modelo.Entradas.NovaMovimentacao;
import br.puc.estoquehosp.modelo.Entradas.NovoUsuario;
import br.puc.estoquehosp.modelo.Perfil;
import br.puc.estoquehosp.modelo.Usuario;
import br.puc.estoquehosp.socket.comum.*;

import java.io.*;
import java.net.*;
import java.util.*;

// SupervisoraDeConexao do Fazedor de Continhas adaptada ao EstoqueHosp.
// O laço que recebe os comunicados é o mesmo; mudou o que é feito com cada pedido:
// no lugar das contas (+ - * /), o método atenda chama as regras do estoque e devolve um Resultado.
public class SupervisoraDeConexao extends Thread
{
    // EstoqueHosp: só aceita objetos das classes do projeto e tipos básicos do Java.
    // Sem isso, qualquer um que se conectasse na porta poderia mandar um objeto malicioso.
    private static final ObjectInputFilter FILTRO =
    ObjectInputFilter.Config.createFilter ("br.puc.estoquehosp.**;java.lang.*;java.util.*;!*");

    private Parceiro            usuario;
    private Socket              conexao;
    private ArrayList<Parceiro> usuarios;
    private Servicos            servicos; // EstoqueHosp: no lugar de "double valor"

    public SupervisoraDeConexao
    (Socket conexao, ArrayList<Parceiro> usuarios, Servicos servicos) // EstoqueHosp: + servicos
    throws Exception
    {
        if (conexao==null)
            throw new Exception ("Conexao ausente");

        if (usuarios==null)
            throw new Exception ("Usuarios ausentes");

        if (servicos==null) // EstoqueHosp
            throw new Exception ("Servicos ausentes");

        this.conexao  = conexao;
        this.usuarios = usuarios;
        this.servicos = servicos; // EstoqueHosp
    }

    public void run ()
    {

        ObjectOutputStream transmissor;
        try
        {
            transmissor =
            new ObjectOutputStream(
            this.conexao.getOutputStream());
        }
        catch (Exception erro)
        {
            return;
        }

        ObjectInputStream receptor=null;
        try
        {
            receptor=
            new ObjectInputStream(
            this.conexao.getInputStream());
            receptor.setObjectInputFilter (FILTRO); // EstoqueHosp
        }
        catch (Exception err0)
        {
            try
            {
                transmissor.close();
            }
            catch (Exception falha)
            {} // so tentando fechar antes de acabar a thread

            return;
        }

        try
        {
            this.usuario =
            new Parceiro (this.conexao,
                          receptor,
                          transmissor);
        }
        catch (Exception erro)
        {} // sei que passei os parametros corretos

        try
        {
            synchronized (this.usuarios)
            {
                this.usuarios.add (this.usuario);
            }


            for(;;)
            {
                Comunicado comunicado = this.usuario.envie ();

                if (comunicado==null)
                    return;
                else if (comunicado instanceof PedidoParaSair)
                {
                    synchronized (this.usuarios)
                    {
                        this.usuarios.remove (this.usuario);
                    }
                    this.usuario.adeus();
                }
                else // EstoqueHosp: qualquer outro pedido é atendido e respondido com um Resultado
                {
                    this.usuario.receba (this.atenda (comunicado));
                }
            }
        }
        catch (Exception erro)
        {
            synchronized (this.usuarios) // EstoqueHosp: tira da lista quem caiu sem pedir para sair
            {
                this.usuarios.remove (this.usuario);
            }

            try
            {
                transmissor.close ();
                receptor   .close ();
            }
            catch (Exception falha)
            {} // so tentando fechar antes de acabar a thread

            return;
        }
    }

    // EstoqueHosp: no Fazedor de Continhas, este era o switch com + - * /.
    // Aqui cada tipo de pedido chama a regra de negócio correspondente.
    private Resultado atenda (Comunicado comunicado)
    {
        try
        {
            // ----- pedidos que não exigem login -----

            if (comunicado instanceof PedidoDeCadastroDeUsuario)
            {
                PedidoDeCadastroDeUsuario pedido = (PedidoDeCadastroDeUsuario)comunicado;
                Usuario novo = this.servicos.getUsuarios().cadastrar (
                new NovoUsuario (pedido.getNome(), pedido.getMatricula(), pedido.getPerfil(), pedido.getSenha()));
                return new Resultado (201, null, novo);
            }

            if (comunicado instanceof PedidoDeLogin)
            {
                PedidoDeLogin pedido = (PedidoDeLogin)comunicado;
                return new Resultado (200, null, this.servicos.getUsuarios().entrar (
                new Login (pedido.getMatricula(), pedido.getSenha())));
            }

            // ----- daqui para baixo, todos os pedidos exigem login -----

            if (!(comunicado instanceof PedidoAutenticado))
                return new Resultado (400, "Pedido desconhecido.", null);

            String  token  = ((PedidoAutenticado)comunicado).getToken();
            Usuario logado = this.servicos.getSessoes().usuarioDoToken(token).orElse(null);
            if (logado==null)
                return new Resultado (401, "Faça login para continuar.", null);

            if (comunicado instanceof PedidoDeLogout)
            {
                this.servicos.getUsuarios().sair (token);
                return new Resultado (204, null, null);
            }

            if (comunicado instanceof PedidoDeUsuarioAtual)
                return new Resultado (200, null, logado);

            if (comunicado instanceof PedidoDeMateriais)
            {
                PedidoDeMateriais pedido = (PedidoDeMateriais)comunicado;
                return new Resultado (200, null, new ArrayList<> (this.servicos.getMateriais().listar (pedido.getBusca())));
            }

            if (comunicado instanceof PedidoDeMaterial)
            {
                PedidoDeMaterial pedido = (PedidoDeMaterial)comunicado;
                return new Resultado (200, null, this.servicos.getMateriais().buscar (pedido.getId()));
            }

            if (comunicado instanceof PedidoDeCadastroDeMaterial)
            {
                exija (logado, Perfil.ALMOXARIFADO, Perfil.GESTAO);
                PedidoDeCadastroDeMaterial pedido = (PedidoDeCadastroDeMaterial)comunicado;
                return new Resultado (201, null, this.servicos.getMateriais().criar (
                new DadosMaterial (pedido.getNome(), pedido.getCodigoBarras(), pedido.getLote(),
                                   pedido.getValidade(), pedido.getQuantidadeMinima())));
            }

            if (comunicado instanceof PedidoDeEdicaoDeMaterial)
            {
                exija (logado, Perfil.ALMOXARIFADO, Perfil.GESTAO);
                PedidoDeEdicaoDeMaterial pedido = (PedidoDeEdicaoDeMaterial)comunicado;
                return new Resultado (200, null, this.servicos.getMateriais().atualizar (pedido.getId(),
                new DadosMaterial (pedido.getNome(), pedido.getCodigoBarras(), pedido.getLote(),
                                   pedido.getValidade(), pedido.getQuantidadeMinima())));
            }

            if (comunicado instanceof PedidoDeInativacaoDeMaterial)
            {
                exija (logado, Perfil.ALMOXARIFADO, Perfil.GESTAO);
                PedidoDeInativacaoDeMaterial pedido = (PedidoDeInativacaoDeMaterial)comunicado;
                this.servicos.getMateriais().inativar (pedido.getId());
                return new Resultado (204, null, null);
            }

            if (comunicado instanceof PedidoDeMovimentacao)
            {
                exija (logado, Perfil.ALMOXARIFADO, Perfil.CENTRO_CIRURGICO);
                PedidoDeMovimentacao pedido = (PedidoDeMovimentacao)comunicado;
                return new Resultado (201, null, this.servicos.getMovimentacoes().registrar (
                new NovaMovimentacao (pedido.getMaterial(), pedido.getTipo(), pedido.getOrigem(),
                                      pedido.getQuantidade(), pedido.getLocal()),
                logado));
            }

            if (comunicado instanceof PedidoDeHistorico)
            {
                PedidoDeHistorico pedido = (PedidoDeHistorico)comunicado;
                return new Resultado (200, null, new ArrayList<> (
                this.servicos.getMovimentacoes().listar (pedido.getDias(), pedido.getMaterialId())));
            }

            if (comunicado instanceof PedidoDeAlertas)
                return new Resultado (200, null, new ArrayList<> (this.servicos.getAlertas().listar()));

            if (comunicado instanceof PedidoDeDashboard)
                return new Resultado (200, null, this.servicos.getAlertas().resumo());

            return new Resultado (400, "Pedido desconhecido.", null);
        }
        catch (ErroApi erro) // regra de negócio: saldo insuficiente, material não encontrado, matrícula repetida...
        {
            return new Resultado (erro.status(), erro.getMessage(), null);
        }
        catch (Exception erro)
        {
            erro.printStackTrace();
            return new Resultado (500, "Erro interno no servidor.", null);
        }
    }

    // EstoqueHosp: confere se o perfil do usuário pode fazer a ação
    private static void exija (Usuario usuario, Perfil... perfis)
    {
        if (!Arrays.asList(perfis).contains(usuario.perfil()))
            throw new ErroApi (403, "Seu perfil não tem permissão para esta ação.");
    }
}
