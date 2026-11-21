// Autor: Gabriel Albasini RA: 25019916

// Palavras-chave: Thread · ServerSocket.accept · cria uma SupervisoraDeConexao por cliente · Fazedor de Continhas
package br.puc.estoquehosp.socket.servidor;

import br.puc.estoquehosp.socket.comum.Parceiro;

import java.net.*;
import java.util.*;

// AceitadoraDeConexao do Fazedor de Continhas. Única mudança: recebe e repassa os serviços do estoque.
public class AceitadoraDeConexao extends Thread
{
    private ServerSocket        pedido;
    private ArrayList<Parceiro> usuarios;
    private Servicos            servicos; // EstoqueHosp

    public AceitadoraDeConexao
    (String porta, ArrayList<Parceiro> usuarios, Servicos servicos) // EstoqueHosp: + servicos
    throws Exception
    {
        if (porta==null)
            throw new Exception ("Porta ausente");

        try
        {
            this.pedido =
            new ServerSocket (Integer.parseInt(porta));
        }
        catch (Exception  erro)
        {
            throw new Exception ("Porta invalida");
        }

        if (usuarios==null)
            throw new Exception ("Usuarios ausentes");

        if (servicos==null) // EstoqueHosp
            throw new Exception ("Servicos ausentes");

        this.usuarios = usuarios;
        this.servicos = servicos; // EstoqueHosp
    }

    public void run ()
    {
        for(;;)
        {
            Socket conexao=null;
            try
            {
                conexao = this.pedido.accept();
            }
            catch (Exception erro)
            {
                continue;
            }

            SupervisoraDeConexao supervisoraDeConexao=null;
            try
            {
                supervisoraDeConexao =
                new SupervisoraDeConexao (conexao, usuarios, servicos); // EstoqueHosp: + servicos
            }
            catch (Exception erro)
            {} // sei que passei parametros corretos para o construtor
            supervisoraDeConexao.start();
        }
    }
}
