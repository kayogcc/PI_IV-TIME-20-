package br.puc.estoquehosp;

import br.puc.estoquehosp.banco.ConexaoMongo;
import br.puc.estoquehosp.controlador.AlertaControlador;
import br.puc.estoquehosp.controlador.MaterialControlador;
import br.puc.estoquehosp.controlador.MovimentacaoControlador;
import br.puc.estoquehosp.controlador.UsuarioControlador;
import br.puc.estoquehosp.http.Roteador;
import br.puc.estoquehosp.http.Sessoes;
import br.puc.estoquehosp.repositorio.MaterialRepositorio;
import br.puc.estoquehosp.repositorio.MovimentacaoRepositorio;
import br.puc.estoquehosp.repositorio.UsuarioRepositorio;
import br.puc.estoquehosp.servico.AlertaServico;
import br.puc.estoquehosp.servico.MaterialServico;
import br.puc.estoquehosp.servico.MovimentacaoServico;
import br.puc.estoquehosp.servico.UsuarioServico;
import com.google.gson.Gson;
import com.mongodb.client.MongoDatabase;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

// Ponto de entrada da API. Monta as camadas (repositório -> serviço -> controlador) e sobe o servidor.
public class Aplicacao {

    public static void main(String[] args) throws IOException {
        int porta = Integer.parseInt(System.getenv().getOrDefault("PORTA", "8080"));

        ConexaoMongo conexao = ConexaoMongo.doAmbiente();
        conexao.testar();
        MongoDatabase banco = conexao.banco();

        Sessoes sessoes = new Sessoes();
        Roteador roteador = new Roteador(sessoes, new Gson());

        MaterialRepositorio materiais = new MaterialRepositorio(banco);
        MovimentacaoRepositorio movimentacoes = new MovimentacaoRepositorio(banco);

        new UsuarioControlador(new UsuarioServico(new UsuarioRepositorio(banco), sessoes)).registrar(roteador);
        new MaterialControlador(new MaterialServico(materiais)).registrar(roteador);
        new MovimentacaoControlador(new MovimentacaoServico(materiais, movimentacoes)).registrar(roteador);
        new AlertaControlador(new AlertaServico(materiais, movimentacoes)).registrar(roteador);

        HttpServer servidor = HttpServer.create(new InetSocketAddress(porta), 0);
        servidor.createContext("/api", roteador);
        servidor.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        servidor.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            servidor.stop(1);
            conexao.close();
        }));
        System.out.println("API do EstoqueHosp rodando em http://localhost:" + porta + "/api");
    }
}
