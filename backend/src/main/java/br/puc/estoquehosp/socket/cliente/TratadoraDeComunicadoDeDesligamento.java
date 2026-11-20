// Gabriel Figueira Albasini
// Palavras-chave: Thread · escuta o ComunicadoDeDesligamento · encerra o cliente
package br.puc.estoquehosp.socket.cliente;

import br.puc.estoquehosp.socket.comum.ComunicadoDeDesligamento;
import br.puc.estoquehosp.socket.comum.Parceiro;

import java.net.*;

// TratadoraDeComunicadoDeDesligamento do Fazedor de Continhas.
public class TratadoraDeComunicadoDeDesligamento extends Thread
{
    private Parceiro servidor;

    public TratadoraDeComunicadoDeDesligamento (Parceiro servidor) throws Exception
    {
        if (servidor==null)
            throw new Exception ("Porta invalida");

        this.servidor = servidor;
    }

    public void run ()
    {
        for(;;)
        {
			try
			{
				if (this.servidor.espie() instanceof ComunicadoDeDesligamento)
				{
					System.out.println ("\nO servidor vai ser desligado agora;");
				    System.err.println ("volte mais tarde!\n");
				    System.exit(0);
				}
			}
			catch (Exception erro)
			{
				// EstoqueHosp: se a conexão caiu, avisa e encerra (antes ficava tentando para sempre)
				System.err.println ("\nA conexao com o servidor foi perdida.");
				System.exit(1);
			}
        }
    }
}
