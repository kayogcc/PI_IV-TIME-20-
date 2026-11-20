// Autor: Gabriel Figueira Albasini

// Palavras-chave: main do Servidor de sockets · porta 3000 · conecta no MongoDB · comando "desativar" · Fazedor de Continhas adaptado
package br.puc.estoquehosp.socket.servidor;

import br.puc.estoquehosp.banco.ConexaoMongo;
import br.puc.estoquehosp.socket.comum.ComunicadoDeDesligamento;
import br.puc.estoquehosp.socket.comum.Parceiro;
import br.puc.estoquehosp.socket.comum.Teclado;

import java.util.*;

// Servidor do Fazedor de Continhas adaptado ao EstoqueHosp.
// Os trechos marcados com "EstoqueHosp:" são as únicas mudanças em relação ao código original.
public class Servidor
{
	public static String PORTA_PADRAO = "3000";

    public static void main (String[] args)
    {
        if (args.length>1)
        {
            System.err.println ("Uso esperado: java Servidor [PORTA]\n");
            return;
        }

        String porta=Servidor.PORTA_PADRAO;

        if (args.length==1)
            porta = args[0];

        // EstoqueHosp: conecta no MongoDB e cria os serviços com as regras do estoque
        ConexaoMongo conexao=null;
        Servicos     servicos=null;
        try
        {
            conexao = ConexaoMongo.doAmbiente();
            conexao.testar();
            servicos = new Servicos (conexao.banco());
        }
        catch (Exception erro)
        {
            System.err.println ("Nao foi possivel conectar ao MongoDB: "+erro.getMessage()+"\n");
            if (conexao!=null) conexao.close();
            return;
        }

        ArrayList<Parceiro> usuarios =
        new ArrayList<Parceiro> ();

        AceitadoraDeConexao aceitadoraDeConexao=null;
        try
        {
            aceitadoraDeConexao =
            new AceitadoraDeConexao (porta, usuarios, servicos); // EstoqueHosp: + servicos
            aceitadoraDeConexao.start();
        }
        catch (Exception erro)
        {
            System.err.println ("Escolha uma porta apropriada e liberada para uso!\n");
            conexao.close(); // EstoqueHosp
            return;
        }

        for(;;)
        {
            System.out.println ("O servidor esta ativo na porta "+porta+"! Para desativa-lo,"); // EstoqueHosp: + porta
            System.out.println ("use o comando \"desativar\"\n");
            System.out.print   ("> ");

            String comando=null;
            try
            {
                comando = Teclado.getUmString();
            }
            catch (Exception erro)
            {}

            if (comando.toLowerCase().equals("desativar"))
            {
                synchronized (usuarios)
                {
					ComunicadoDeDesligamento comunicadoDeDesligamento =
                    new ComunicadoDeDesligamento ();

                    for (Parceiro usuario:usuarios)
                    {
                        try
                        {
                            usuario.receba (comunicadoDeDesligamento);
                            usuario.adeus  ();
                        }
                        catch (Exception erro)
                        {}
                    }
                }

                conexao.close(); // EstoqueHosp: fecha a conexão com o MongoDB
                System.out.println ("O servidor foi desativado!\n");
                System.exit(0);
            }
            else
                System.err.println ("Comando invalido!\n");
        }
    }
}
