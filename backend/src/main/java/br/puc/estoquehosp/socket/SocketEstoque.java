package br.puc.estoquehosp.socket;

import br.puc.estoquehosp.http.ErroApi;
import br.puc.estoquehosp.http.Sessoes;
import br.puc.estoquehosp.banco.ConexaoMongo;
import br.puc.estoquehosp.modelo.Alerta;
import br.puc.estoquehosp.modelo.Entradas.Login;
import br.puc.estoquehosp.modelo.Entradas.NovaMovimentacao;
import br.puc.estoquehosp.modelo.Material;
import br.puc.estoquehosp.modelo.Movimentacao;
import br.puc.estoquehosp.modelo.Perfil;
import br.puc.estoquehosp.modelo.Usuario;
import br.puc.estoquehosp.repositorio.MaterialRepositorio;
import br.puc.estoquehosp.repositorio.MovimentacaoRepositorio;
import br.puc.estoquehosp.repositorio.UsuarioRepositorio;
import br.puc.estoquehosp.servico.AlertaServico;
import br.puc.estoquehosp.servico.MaterialServico;
import br.puc.estoquehosp.servico.MovimentacaoServico;
import br.puc.estoquehosp.servico.UsuarioServico;
import com.mongodb.client.MongoDatabase;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Semaphore;

// Servidor e cliente de sockets do EstoqueHosp, no modelo do "Fazedor de Continhas".
// Uso: SocketEstoque servidor [PORTA]   ou   SocketEstoque cliente [HOST [PORTA]]
public class SocketEstoque {

    public static final String PORTA_PADRAO = "3000";
    public static final String HOST_PADRAO  = "localhost";

    public static void main(String[] args) {
        String modo = args.length > 0 ? args[0].toLowerCase() : "";
        String[] resto = args.length > 0 ? Arrays.copyOfRange(args, 1, args.length) : args;

        if (modo.equals("cliente"))       Cliente.executar(resto);
        else if (modo.equals("servidor")) Servidor.executar(resto);
        else System.err.println("Uso esperado: SocketEstoque cliente [HOST [PORTA]]  ou  SocketEstoque servidor [PORTA]");
    }

    // =====================================================================
    //  CLASSES COMUNS AO CLIENTE E AO SERVIDOR
    // =====================================================================

    public static class Comunicado implements Serializable, Cloneable {
        private static final long serialVersionUID = 1L;
    }

    public static class ComunicadoDeDesligamento extends Comunicado {
        private static final long serialVersionUID = 1L;
    }

    public static class PedidoParaSair extends Comunicado {
        private static final long serialVersionUID = 1L;
    }

    public static class PedidoDeAlertas extends Comunicado {
        private static final long serialVersionUID = 1L;
    }

    public static class PedidoDeLogin extends Comunicado {
        private static final long serialVersionUID = 1L;
        private final String matricula;
        private final String senha;

        public PedidoDeLogin(String matricula, String senha) {
            this.matricula = matricula;
            this.senha     = senha;
        }

        public String getMatricula() { return this.matricula; }
        public String getSenha()     { return this.senha; }
    }

    // busca: parte do nome ou código de barras; null lista todos
    public static class PedidoDeMateriais extends Comunicado {
        private static final long serialVersionUID = 1L;
        private final String busca;

        public PedidoDeMateriais(String busca) { this.busca = busca; }

        public String getBusca() { return this.busca; }
    }

    // tipo = "Entrada" | "Saída"; origem = "Código de barras" | "Manual"
    // local = "Almoxarifado A" | "Almoxarifado B" | "Centro Cirúrgico"
    public static class PedidoDeMovimentacao extends Comunicado {
        private static final long serialVersionUID = 1L;
        private final String  material;
        private final String  tipo;
        private final String  origem;
        private final Integer quantidade;
        private final String  local;

        public PedidoDeMovimentacao(String material, String tipo, String origem, Integer quantidade, String local) {
            this.material   = material;
            this.tipo       = tipo;
            this.origem     = origem;
            this.quantidade = quantidade;
            this.local      = local;
        }

        public String  getMaterial()   { return this.material; }
        public String  getTipo()       { return this.tipo; }
        public String  getOrigem()     { return this.origem; }
        public Integer getQuantidade() { return this.quantidade; }
        public String  getLocal()      { return this.local; }
    }

    // Resposta do servidor para qualquer pedido
    public static class Resultado extends Comunicado {
        private static final long serialVersionUID = 1L;
        private final boolean           sucesso;
        private final String            mensagem;
        private final ArrayList<String> linhas;

        private Resultado(boolean sucesso, String mensagem, ArrayList<String> linhas) {
            this.sucesso  = sucesso;
            this.mensagem = mensagem;
            this.linhas   = linhas;
        }

        public static Resultado ok(String mensagem)                     { return new Resultado(true, mensagem, new ArrayList<>()); }
        public static Resultado ok(String mensagem, ArrayList<String> l) { return new Resultado(true, mensagem, l); }
        public static Resultado falha(String mensagem)                  { return new Resultado(false, mensagem, new ArrayList<>()); }

        public boolean           isSucesso()   { return this.sucesso; }
        public String            getMensagem() { return this.mensagem; }
        public ArrayList<String> getLinhas()   { return this.linhas; }
    }

    public static class Parceiro {
        private Socket             conexao;
        private ObjectInputStream  receptor;
        private ObjectOutputStream transmissor;
        private Comunicado         proximoComunicado = null;
        private Semaphore          mutEx = new Semaphore(1, true);

        public Parceiro(Socket conexao, ObjectInputStream receptor, ObjectOutputStream transmissor) throws Exception {
            if (conexao == null)     throw new Exception("Conexao ausente");
            if (receptor == null)    throw new Exception("Receptor ausente");
            if (transmissor == null) throw new Exception("Transmissor ausente");

            this.conexao     = conexao;
            this.receptor    = receptor;
            this.transmissor = transmissor;
        }

        public void receba(Comunicado x) throws Exception {
            try {
                synchronized (this.transmissor) {
                    this.transmissor.writeObject(x);
                    this.transmissor.flush();
                    this.transmissor.reset();
                }
            } catch (IOException erro) {
                throw new Exception("Erro de transmissao");
            }
        }

        public Comunicado espie() throws Exception {
            try {
                this.mutEx.acquireUninterruptibly();
                if (this.proximoComunicado == null) this.proximoComunicado = (Comunicado) this.receptor.readObject();
                return this.proximoComunicado;
            } catch (Exception erro) {
                throw new Exception("Erro de recepcao");
            } finally {
                this.mutEx.release();
            }
        }

        public Comunicado envie() throws Exception {
            try {
                if (this.proximoComunicado == null) this.proximoComunicado = (Comunicado) this.receptor.readObject();
                Comunicado ret = this.proximoComunicado;
                this.proximoComunicado = null;
                return ret;
            } catch (Exception erro) {
                throw new Exception("Erro de recepcao");
            }
        }

        public void adeus() throws Exception {
            try {
                this.transmissor.close();
                this.receptor.close();
                this.conexao.close();
            } catch (Exception erro) {
                throw new Exception("Erro de desconexao");
            }
        }
    }

    public static class Teclado {
        private static BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));

        public static String getUmString() {
            try {
                return teclado.readLine();
            } catch (IOException erro) {
                return null;
            }
        }

        public static int getUmInt() throws Exception {
            try {
                return Integer.parseInt(teclado.readLine().trim());
            } catch (Exception erro) {
                throw new Exception("Int invalido!");
            }
        }

        public static char getUmChar() throws Exception {
            String str = getUmString();
            if (str == null || str.trim().length() != 1) throw new Exception("Char invalido!");
            return str.trim().charAt(0);
        }
    }

    // =====================================================================
    //  SERVIDOR
    // =====================================================================

    // Os serviços do EstoqueHosp (as mesmas regras da API REST)
    public record Servicos(UsuarioServico usuarios, MaterialServico materiais,
                           MovimentacaoServico movimentacoes, AlertaServico alertas) {

        public static Servicos criar(MongoDatabase banco, Sessoes sessoes) {
            MaterialRepositorio materiais = new MaterialRepositorio(banco);
            MovimentacaoRepositorio movimentacoes = new MovimentacaoRepositorio(banco);
            return new Servicos(
                    new UsuarioServico(new UsuarioRepositorio(banco), sessoes),
                    new MaterialServico(materiais),
                    new MovimentacaoServico(materiais, movimentacoes),
                    new AlertaServico(materiais, movimentacoes));
        }
    }

    public static class AceitadoraDeConexao extends Thread {
        private ServerSocket        pedido;
        private ArrayList<Parceiro> usuarios;
        private Servicos            servicos;

        public AceitadoraDeConexao(String porta, ArrayList<Parceiro> usuarios, Servicos servicos) throws Exception {
            if (porta == null) throw new Exception("Porta ausente");
            try {
                this.pedido = new ServerSocket(Integer.parseInt(porta));
            } catch (Exception erro) {
                throw new Exception("Porta invalida");
            }
            if (usuarios == null) throw new Exception("Usuarios ausentes");
            if (servicos == null) throw new Exception("Servicos ausentes");

            this.usuarios = usuarios;
            this.servicos = servicos;
        }

        public void run() {
            for (;;) {
                Socket conexao = null;
                try {
                    conexao = this.pedido.accept();
                } catch (Exception erro) {
                    continue;
                }

                SupervisoraDeConexao supervisoraDeConexao = null;
                try {
                    supervisoraDeConexao = new SupervisoraDeConexao(conexao, usuarios, servicos);
                } catch (Exception erro) {} // sei que passei parametros corretos
                supervisoraDeConexao.start();
            }
        }
    }

    public static class SupervisoraDeConexao extends Thread {

        // Só aceita objetos das classes do projeto e tipos básicos (segurança)
        private static final ObjectInputFilter FILTRO =
                ObjectInputFilter.Config.createFilter("br.puc.estoquehosp.**;java.lang.*;java.util.*;!*");

        private Parceiro            usuario;
        private Socket              conexao;
        private ArrayList<Parceiro> usuarios;
        private Servicos            servicos;
        private Usuario             usuarioLogado = null;

        public SupervisoraDeConexao(Socket conexao, ArrayList<Parceiro> usuarios, Servicos servicos) throws Exception {
            if (conexao == null)  throw new Exception("Conexao ausente");
            if (usuarios == null) throw new Exception("Usuarios ausentes");
            if (servicos == null) throw new Exception("Servicos ausentes");

            this.conexao  = conexao;
            this.usuarios = usuarios;
            this.servicos = servicos;
        }

        public void run() {
            ObjectOutputStream transmissor;
            try {
                transmissor = new ObjectOutputStream(this.conexao.getOutputStream());
            } catch (Exception erro) {
                return;
            }

            ObjectInputStream receptor = null;
            try {
                receptor = new ObjectInputStream(this.conexao.getInputStream());
                receptor.setObjectInputFilter(FILTRO);
            } catch (Exception erro) {
                try { transmissor.close(); } catch (Exception falha) {}
                return;
            }

            try {
                this.usuario = new Parceiro(this.conexao, receptor, transmissor);
            } catch (Exception erro) {} // sei que passei os parametros corretos

            try {
                synchronized (this.usuarios) {
                    this.usuarios.add(this.usuario);
                }

                for (;;) {
                    Comunicado comunicado = this.usuario.envie();

                    if (comunicado == null) {
                        return;
                    } else if (comunicado instanceof PedidoParaSair) {
                        synchronized (this.usuarios) {
                            this.usuarios.remove(this.usuario);
                        }
                        this.usuario.adeus();
                        return;
                    } else {
                        this.usuario.receba(this.atender(comunicado));
                    }
                }
            } catch (Exception erro) {
                synchronized (this.usuarios) {
                    this.usuarios.remove(this.usuario);
                }
                try {
                    transmissor.close();
                    receptor.close();
                } catch (Exception falha) {}
            }
        }

        private Resultado atender(Comunicado comunicado) {
            try {
                if (comunicado instanceof PedidoDeLogin pedido) {
                    this.usuarioLogado = servicos.usuarios()
                            .entrar(new Login(pedido.getMatricula(), pedido.getSenha())).usuario();
                    return Resultado.ok("Bem-vindo(a), " + this.usuarioLogado.nome() + "!");
                }

                if (this.usuarioLogado == null)
                    return Resultado.falha("Faça login para continuar.");

                if (comunicado instanceof PedidoDeMateriais pedido) {
                    ArrayList<String> linhas = new ArrayList<>();
                    for (Material m : servicos.materiais().listar(pedido.getBusca()))
                        linhas.add(m.nome() + " | cód. " + (m.codigoBarras() == null ? "-" : m.codigoBarras())
                                + " | total " + m.quantidade() + " un. (mín. " + m.quantidadeMinima() + ") | " + m.saldos());
                    return Resultado.ok(linhas.isEmpty() ? "Nenhum material encontrado." : null, linhas);
                }

                if (comunicado instanceof PedidoDeAlertas) {
                    ArrayList<String> linhas = new ArrayList<>();
                    for (Alerta a : servicos.alertas().listar())
                        linhas.add("[" + a.prioridade() + "] " + a.titulo() + " - " + a.detalhe());
                    return Resultado.ok(linhas.isEmpty() ? "Nenhum alerta." : null, linhas);
                }

                if (comunicado instanceof PedidoDeMovimentacao pedido) {
                    if (this.usuarioLogado.perfil() == Perfil.GESTAO)
                        return Resultado.falha("Seu perfil não tem permissão para esta ação.");

                    Movimentacao mov = servicos.movimentacoes().registrar(
                            new NovaMovimentacao(pedido.getMaterial(), pedido.getTipo(), pedido.getOrigem(),
                                    pedido.getQuantidade(), pedido.getLocal()),
                            this.usuarioLogado);
                    return Resultado.ok("Movimentação registrada: " + mov.tipo() + " de " + mov.quantidade()
                            + " un. de " + mov.material() + " (" + mov.local() + ").");
                }

                return Resultado.falha("Pedido desconhecido.");
            } catch (ErroApi erro) {
                return Resultado.falha(erro.getMessage()); // saldo insuficiente, material não encontrado...
            } catch (Exception erro) {
                erro.printStackTrace();
                return Resultado.falha("Erro interno no servidor.");
            }
        }
    }

    public static class Servidor {

        // Chamado pela Aplicacao: sobe o socket junto com a API HTTP
        public static ArrayList<Parceiro> iniciar(String porta, MongoDatabase banco, Sessoes sessoes) throws Exception {
            ArrayList<Parceiro> usuarios = new ArrayList<Parceiro>();
            AceitadoraDeConexao aceitadoraDeConexao =
                    new AceitadoraDeConexao(porta, usuarios, Servicos.criar(banco, sessoes));
            aceitadoraDeConexao.setDaemon(true);
            aceitadoraDeConexao.start();
            return usuarios;
        }

        // Envia ComunicadoDeDesligamento a todos os clientes
        public static void desligar(ArrayList<Parceiro> usuarios) {
            synchronized (usuarios) {
                ComunicadoDeDesligamento comunicadoDeDesligamento = new ComunicadoDeDesligamento();
                for (Parceiro usuario : usuarios) {
                    try {
                        usuario.receba(comunicadoDeDesligamento);
                        usuario.adeus();
                    } catch (Exception erro) {}
                }
                usuarios.clear();
            }
        }

        // Modo "aula": só o servidor de sockets, com o comando desativar
        public static void executar(String[] args) {
            if (args.length > 1) {
                System.err.println("Uso esperado: SocketEstoque servidor [PORTA]\n");
                return;
            }
            String porta = args.length == 1 ? args[0] : PORTA_PADRAO;

            ConexaoMongo conexao = ConexaoMongo.doAmbiente();
            conexao.testar();

            ArrayList<Parceiro> usuarios;
            try {
                usuarios = iniciar(porta, conexao.banco(), new Sessoes());
            } catch (Exception erro) {
                System.err.println("Escolha uma porta apropriada e liberada para uso!\n");
                return;
            }

            for (;;) {
                System.out.println("O servidor esta ativo na porta " + porta + "! Para desativa-lo,");
                System.out.println("use o comando \"desativar\"\n");
                System.out.print("> ");

                String comando = Teclado.getUmString();
                if (comando == null) continue;

                if (comando.trim().equalsIgnoreCase("desativar")) {
                    desligar(usuarios);
                    conexao.close();
                    System.out.println("O servidor foi desativado!\n");
                    System.exit(0);
                } else {
                    System.err.println("Comando invalido!\n");
                }
            }
        }
    }

    // =====================================================================
    //  CLIENTE
    // =====================================================================

    public static class TratadoraDeComunicadoDeDesligamento extends Thread {
        private Parceiro servidor;

        public TratadoraDeComunicadoDeDesligamento(Parceiro servidor) throws Exception {
            if (servidor == null) throw new Exception("Servidor ausente");
            this.servidor = servidor;
        }

        public void run() {
            for (;;) {
                try {
                    if (this.servidor.espie() instanceof ComunicadoDeDesligamento) {
                        System.out.println("\nO servidor vai ser desligado agora;");
                        System.err.println("volte mais tarde!\n");
                        System.exit(0);
                    }
                } catch (Exception erro) {
                    System.err.println("\nConexao com o servidor perdida.");
                    System.exit(1);
                }
            }
        }
    }

    public static class Cliente {
        private static final String[] LOCAIS = { "Almoxarifado A", "Almoxarifado B", "Centro Cirúrgico" };

        public static void executar(String[] args) {
            if (args.length > 2) {
                System.err.println("Uso esperado: SocketEstoque cliente [HOST [PORTA]]\n");
                return;
            }

            Parceiro servidor = null;
            try {
                String host  = args.length > 0 ? args[0] : HOST_PADRAO;
                int    porta = Integer.parseInt(args.length == 2 ? args[1] : PORTA_PADRAO);

                Socket conexao = new Socket(host, porta);
                ObjectOutputStream transmissor = new ObjectOutputStream(conexao.getOutputStream());
                ObjectInputStream  receptor    = new ObjectInputStream(conexao.getInputStream());
                servidor = new Parceiro(conexao, receptor, transmissor);
            } catch (Exception erro) {
                System.err.println("Indique o servidor e a porta corretos!\n");
                return;
            }

            try {
                new TratadoraDeComunicadoDeDesligamento(servidor).start();
            } catch (Exception erro) {} // sei que servidor foi instanciado

            try {
                // ----- LOGIN -----
                for (;;) {
                    System.out.print("Matricula? ");
                    String matricula = Teclado.getUmString();
                    String senha = lerSenha();

                    Resultado r = pedir(servidor, new PedidoDeLogin(matricula, senha));
                    System.out.println(r.getMensagem() + "\n");
                    if (r.isSucesso()) break;
                }

                // ----- MENU -----
                char opcao = ' ';
                do {
                    System.out.print("Sua opcao ([M]ateriais, [E]ntrada, [S]aida, [A]lertas, [T]erminar)? ");
                    try {
                        opcao = Character.toUpperCase(Teclado.getUmChar());
                    } catch (Exception erro) {
                        System.err.println("Opcao invalida!\n");
                        continue;
                    }

                    if ("MESAT".indexOf(opcao) == -1) {
                        System.err.println("Opcao invalida!\n");
                        continue;
                    }

                    try {
                        switch (opcao) {
                            case 'M':
                                System.out.print("Buscar (enter = todos)? ");
                                String busca = Teclado.getUmString();
                                mostrar(pedir(servidor, new PedidoDeMateriais(
                                        busca == null || busca.isBlank() ? null : busca)));
                                break;
                            case 'E': movimentar(servidor, "Entrada"); break;
                            case 'S': movimentar(servidor, "Saída");   break;
                            case 'A': mostrar(pedir(servidor, new PedidoDeAlertas())); break;
                        }
                    } catch (Exception erro) {
                        System.err.println("Erro de comunicacao com o servidor;");
                        System.err.println("Tente novamente!\n");
                    }
                } while (opcao != 'T');
            } catch (Exception erro) {
                System.err.println("Erro de comunicacao com o servidor.");
            }

            try {
                servidor.receba(new PedidoParaSair());
            } catch (Exception erro) {}

            System.out.println("Obrigado por usar este programa!");
            System.exit(0);
        }

        // Envia o pedido e espera o Resultado (espie/envie, como no Fazedor de Continhas)
        private static Resultado pedir(Parceiro servidor, Comunicado pedido) throws Exception {
            servidor.receba(pedido);
            Comunicado comunicado = null;
            do {
                comunicado = servidor.espie();
            } while (!(comunicado instanceof Resultado));
            return (Resultado) servidor.envie();
        }

        private static void mostrar(Resultado r) {
            if (!r.isSucesso()) {
                System.err.println(r.getMensagem() + "\n");
                return;
            }
            if (r.getMensagem() != null) System.out.println(r.getMensagem());
            for (String linha : r.getLinhas()) System.out.println("- " + linha);
            System.out.println();
        }

        private static void movimentar(Parceiro servidor, String tipo) throws Exception {
            System.out.print("Codigo de barras ou nome do material? ");
            String material = Teclado.getUmString();

            System.out.print("Origem ([C]odigo de barras / [M]anual)? ");
            String origem = "C".equalsIgnoreCase(Teclado.getUmString()) ? "Código de barras" : "Manual";

            int quantidade;
            try {
                System.out.print("Quantidade? ");
                quantidade = Teclado.getUmInt();
            } catch (Exception erro) {
                System.err.println("Quantidade invalida!\n");
                return;
            }

            int local;
            try {
                System.out.print("Local (1 = Almoxarifado A, 2 = Almoxarifado B, 3 = Centro Cirurgico)? ");
                local = Teclado.getUmInt();
                if (local < 1 || local > 3) throw new Exception();
            } catch (Exception erro) {
                System.err.println("Local invalido!\n");
                return;
            }

            mostrar(pedir(servidor, new PedidoDeMovimentacao(material, tipo, origem, quantidade, LOCAIS[local - 1])));
        }

        private static String lerSenha() {
            Console console = System.console();
            if (console != null) return new String(console.readPassword("Senha? "));
            System.out.print("Senha? ");
            return Teclado.getUmString();
        }
    }
}
