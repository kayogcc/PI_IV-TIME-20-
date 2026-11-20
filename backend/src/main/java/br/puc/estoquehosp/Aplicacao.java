// Autores: Bruno (servidor HTTP da API), Gabriel Figueira Albasini (ligação com o servidor de sockets)

// Palavras-chave: ponto de entrada da API · HttpServer na porta 8080 · /api · registra controladores · virtual threads · ponte para o Servidor (3000)
package br.puc.estoquehosp;

import br.puc.estoquehosp.controlador.AlertaControlador;
import br.puc.estoquehosp.controlador.MaterialControlador;
import br.puc.estoquehosp.controlador.MovimentacaoControlador;
import br.puc.estoquehosp.controlador.UsuarioControlador;
import br.puc.estoquehosp.http.PonteSocket;
import br.puc.estoquehosp.http.Roteador;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

// API HTTP do EstoqueHosp: é só a ponte entre o site e o Servidor de sockets.
// Não conecta no MongoDB nem tem regras de negócio: cada requisição vira um pedido para o Servidor
// (br.puc.estoquehosp.socket.servidor.Servidor), que precisa estar rodando.
public class Aplicacao {

    public static void main(String[] args) throws IOException {
        int porta = Integer.parseInt(System.getenv().getOrDefault("PORTA", "8080"));
        String hostServidor = System.getenv().getOrDefault("SERVIDOR_HOST", "localhost");
        int portaServidor = Integer.parseInt(System.getenv().getOrDefault("SERVIDOR_PORTA", "3000"));

        PonteSocket servidor = new PonteSocket(hostServidor, portaServidor);
        Roteador roteador = new Roteador(new Gson());

        new UsuarioControlador(servidor).registrar(roteador);
        new MaterialControlador(servidor).registrar(roteador);
        new MovimentacaoControlador(servidor).registrar(roteador);
        new AlertaControlador(servidor).registrar(roteador);

        HttpServer api = HttpServer.create(new InetSocketAddress(porta), 0);
        api.createContext("/api", roteador);
        api.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        api.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> api.stop(1)));
        System.out.println("API do EstoqueHosp rodando em http://localhost:" + porta + "/api");
        System.out.println("Repassando os pedidos ao Servidor de sockets em " + hostServidor + ":" + portaServidor);
    }
}
