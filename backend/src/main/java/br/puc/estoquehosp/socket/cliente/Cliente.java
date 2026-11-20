// Gabriel Figueira Albasini
package br.puc.estoquehosp.socket.cliente;

import br.puc.estoquehosp.modelo.Alerta;
import br.puc.estoquehosp.modelo.Material;
import br.puc.estoquehosp.modelo.Movimentacao;
import br.puc.estoquehosp.servico.UsuarioServico.LoginFeito;
import br.puc.estoquehosp.socket.comum.*;

import java.net.*;
import java.io.*;
import java.util.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

// Cliente do Fazedor de Continhas adaptado ao EstoqueHosp.
// A conexão é a mesma; mudou o menu: no lugar de + - * / =, faz login e consulta/movimenta o estoque.
public class Cliente
{
	public static final String HOST_PADRAO  = "localhost";
	public static final int    PORTA_PADRAO = 3000;

	// EstoqueHosp: locais aceitos pelo servidor
	private static final String[] LOCAIS = { "Almoxarifado A", "Almoxarifado B", "Centro Cirúrgico" };

	// EstoqueHosp: validade digitada como DD/MM/AAAA (o servidor recebe AAAA-MM-DD)
	private static final DateTimeFormatter DATA_DIGITADA =
	DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
	private static final DateTimeFormatter DATA_MOSTRADA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	public static void main (String[] args)
	{
        if (args.length>2)
        {
            System.err.println ("Uso esperado: java Cliente [HOST [PORTA]]\n");
            return;
        }

        Socket conexao=null;
        try
        {
            String host = Cliente.HOST_PADRAO;
            int    porta= Cliente.PORTA_PADRAO;

            if (args.length>0)
                host = args[0];

            if (args.length==2)
                porta = Integer.parseInt(args[1]);

            conexao = new Socket (host, porta);
        }
        catch (Exception erro)
        {
            System.err.println ("Indique o servidor e a porta corretos!\n");
            return;
        }

        ObjectOutputStream transmissor=null;
        try
        {
            transmissor =
            new ObjectOutputStream(
            conexao.getOutputStream());
        }
        catch (Exception erro)
        {
            System.err.println ("Indique o servidor e a porta corretos!\n");
            return;
        }

        ObjectInputStream receptor=null;
        try
        {
            receptor =
            new ObjectInputStream(
            conexao.getInputStream());
        }
        catch (Exception erro)
        {
            System.err.println ("Indique o servidor e a porta corretos!\n");
            return;
        }

        Parceiro servidor=null;
        try
        {
            servidor =
            new Parceiro (conexao, receptor, transmissor);
        }
        catch (Exception erro)
        {
            System.err.println ("Indique o servidor e a porta corretos!\n");
            return;
        }

        TratadoraDeComunicadoDeDesligamento tratadoraDeComunicadoDeDesligamento = null;
        try
        {
			tratadoraDeComunicadoDeDesligamento = new TratadoraDeComunicadoDeDesligamento (servidor);
		}
		catch (Exception erro)
		{} // sei que servidor foi instanciado

        tratadoraDeComunicadoDeDesligamento.start();

        // EstoqueHosp: login antes do menu; o token vai em todos os pedidos seguintes
        String token=null;
        while (token==null)
        {
            System.out.print ("Matricula? ");
            String matricula = Teclado.getUmString();
            System.out.print ("Senha? ");
            String senha = Teclado.getUmString();

            try
            {
                Resultado resultado = pergunte (servidor, new PedidoDeLogin (matricula, senha));
                if (resultado.getDeuCerto())
                {
                    LoginFeito login = (LoginFeito)resultado.getDados();
                    token = login.token();
                    System.out.println ("Bem-vindo(a), "+login.usuario().nome()+"!\n");
                }
                else
                    System.err.println (resultado.getMensagem()+"\n");
            }
            catch (Exception erro)
            {
                System.err.println ("Erro de comunicacao com o servidor!\n");
            }
        }

        char opcao=' ';
        do
        {
            System.out.print ("Sua opcao ([M]ateriais, [C]adastrar, [E]ntrada, [S]aida, [A]lertas, [H]istorico, [T]erminar)? ");

            try
            {
				opcao = Character.toUpperCase(Teclado.getUmChar());
		    }
		    catch (Exception erro)
		    {
				System.err.println ("Opcao invalida!\n");
				continue;
			}

			if ("MCESAHT".indexOf(opcao)==-1)
		    {
				System.err.println ("Opcao invalida!\n");
				continue;
			}

			try
			{
				if (opcao=='M')
				{
					System.out.print ("Buscar (enter = todos)? ");
					String busca = Teclado.getUmString();
					Resultado resultado = pergunte (servidor, new PedidoDeMateriais (token, busca==null || busca.isBlank() ? null : busca));
					if (mostreErro (resultado)) continue;

					List<?> materiais = (List<?>)resultado.getDados();
					if (materiais.isEmpty()) System.out.println ("Nenhum material encontrado.");
					for (Object item : materiais)
					{
						Material m = (Material)item;
						System.out.println ("- "+m.nome()+" | cod. "+(m.codigoBarras()==null ? "-" : m.codigoBarras())+
						                    " | lote "+(m.lote()==null ? "-" : m.lote())+" | validade "+formateValidade(m.validade())+
						                    " | total "+m.quantidade()+" un. (min. "+m.quantidadeMinima()+") | "+m.saldos());
					}
					System.out.println();
				}
				else if (opcao=='C')
				{
					PedidoDeCadastroDeMaterial pedido = leiaCadastro (token);
					if (pedido==null) continue;

					Resultado resultado = pergunte (servidor, pedido);
					if (mostreErro (resultado)) continue;

					Material m = (Material)resultado.getDados();
					System.out.println ("Material cadastrado: "+m.nome()+" (validade "+formateValidade(m.validade())+
					                    "). Registre uma entrada para lancar o estoque inicial.\n");
				}
				else if (opcao=='E' || opcao=='S')
				{
					PedidoDeMovimentacao pedido = leiaMovimentacao (token, opcao=='E' ? "Entrada" : "Saída");
					if (pedido==null) continue;

					Resultado resultado = pergunte (servidor, pedido);
					if (mostreErro (resultado)) continue;

					Movimentacao mov = (Movimentacao)resultado.getDados();
					System.out.println ("Registrado: "+mov.tipo()+" de "+mov.quantidade()+" un. de "+mov.material()+" ("+mov.local()+")\n");
				}
				else if (opcao=='A')
				{
					Resultado resultado = pergunte (servidor, new PedidoDeAlertas (token));
					if (mostreErro (resultado)) continue;

					List<?> alertas = (List<?>)resultado.getDados();
					if (alertas.isEmpty()) System.out.println ("Nenhum alerta.");
					for (Object item : alertas)
					{
						Alerta a = (Alerta)item;
						System.out.println ("- ["+a.prioridade()+"] "+a.titulo()+" - "+a.detalhe());
					}
					System.out.println();
				}
				else if (opcao=='H')
				{
					Resultado resultado = pergunte (servidor, new PedidoDeHistorico (token, "30", null));
					if (mostreErro (resultado)) continue;

					List<?> movimentacoes = (List<?>)resultado.getDados();
					if (movimentacoes.isEmpty()) System.out.println ("Nenhuma movimentacao nos ultimos 30 dias.");
					for (Object item : movimentacoes)
					{
						Movimentacao mov = (Movimentacao)item;
						System.out.println ("- "+mov.data().substring(0,10)+" | "+mov.tipo()+" de "+mov.quantidade()+" un. | "+
						                    mov.material()+" | "+mov.local()+" | "+mov.usuario());
					}
					System.out.println();
				}
			}
			catch (Exception erro)
			{
				System.err.println ("Erro de comunicacao com o servidor;");
				System.err.println ("Tente novamente!");
				System.err.println ("Caso o erro persista, termine o programa");
				System.err.println ("e volte a tentar mais tarde!\n");
			}
        }
        while (opcao != 'T');

		try
		{
			servidor.receba (new PedidoDeLogout (token)); // EstoqueHosp: encerra a sessão
			servidor.receba (new PedidoParaSair ());
		}
		catch (Exception erro)
		{}

		System.out.println ("Obrigado por usar este programa!");
		System.exit(0);
	}

	// EstoqueHosp: envia o pedido e espera o Resultado, como o Fazedor de Continhas fazia na opcao '='
	private static Resultado pergunte (Parceiro servidor, Comunicado pedido) throws Exception
	{
		servidor.receba (pedido);
		Comunicado comunicado = null;
		do
		{
			comunicado = (Comunicado)servidor.espie ();
		}
		while (!(comunicado instanceof Resultado));
		return (Resultado)servidor.envie ();
	}

	// EstoqueHosp: mostra a mensagem de erro do servidor (saldo insuficiente, sem permissao...)
	private static boolean mostreErro (Resultado resultado)
	{
		if (resultado.getDeuCerto()) return false;
		System.err.println (resultado.getMensagem()+"\n");
		return true;
	}

	// EstoqueHosp: pergunta os dados de um material novo (codigo de barras, lote e validade sao opcionais)
	private static PedidoDeCadastroDeMaterial leiaCadastro (String token)
	{
		System.out.print ("Nome do material? ");
		String nome = Teclado.getUmString();

		System.out.print ("Codigo de barras (enter = sem codigo)? ");
		String codigo = Teclado.getUmString();

		System.out.print ("Lote (enter = sem lote)? ");
		String lote = Teclado.getUmString();

		String validade = null;
		for(;;)
		{
			System.out.print ("Validade DD/MM/AAAA (enter = sem validade)? ");
			String digitada = Teclado.getUmString();
			if (digitada==null || digitada.isBlank()) break;
			try
			{
				validade = LocalDate.parse(digitada.trim(), DATA_DIGITADA).toString(); // AAAA-MM-DD
				break;
			}
			catch (Exception erro)
			{
				System.err.println ("Data invalida! Use o formato DD/MM/AAAA, ex.: 31/12/2027.");
			}
		}

		int minimo;
		try
		{
			System.out.print ("Quantidade minima? ");
			minimo = Teclado.getUmInt();
		}
		catch (Exception erro)
		{
			System.err.println ("Quantidade minima invalida!\n");
			return null;
		}

		return new PedidoDeCadastroDeMaterial (token, nome, vazioParaNull(codigo), vazioParaNull(lote), validade, minimo);
	}

	// EstoqueHosp: "2027-12-31" -> "31/12/2027"
	private static String formateValidade (String validade)
	{
		if (validade==null) return "-";
		try
		{
			return LocalDate.parse(validade).format(DATA_MOSTRADA);
		}
		catch (Exception erro)
		{
			return validade;
		}
	}

	private static String vazioParaNull (String texto)
	{
		return texto==null || texto.isBlank() ? null : texto.trim();
	}

	// EstoqueHosp: pergunta os dados de uma entrada ou saída
	private static PedidoDeMovimentacao leiaMovimentacao (String token, String tipo)
	{
		System.out.print ("Codigo de barras ou nome do material? ");
		String material = Teclado.getUmString();

		System.out.print ("Origem ([C]odigo de barras / [M]anual)? ");
		String origem = "C".equalsIgnoreCase(Teclado.getUmString()) ? "Código de barras" : "Manual";

		int quantidade;
		try
		{
			System.out.print ("Quantidade? ");
			quantidade = Teclado.getUmInt();
		}
		catch (Exception erro)
		{
			System.err.println ("Quantidade invalida!\n");
			return null;
		}

		int local;
		try
		{
			System.out.print ("Local (1 = Almoxarifado A, 2 = Almoxarifado B, 3 = Centro Cirurgico)? ");
			local = Teclado.getUmInt();
			if (local<1 || local>3) throw new Exception ();
		}
		catch (Exception erro)
		{
			System.err.println ("Local invalido!\n");
			return null;
		}

		return new PedidoDeMovimentacao (token, material, tipo, origem, quantidade, LOCAIS[local-1]);
	}
}
