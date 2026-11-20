// Gabriel Figueira Albasini
// Palavras-chave: controlador · cadastro · login · logout · /api/eu (usuário atual)
package br.puc.estoquehosp.controlador;

import br.puc.estoquehosp.http.PonteSocket;
import br.puc.estoquehosp.http.Roteador;
import br.puc.estoquehosp.modelo.Entradas.Login;
import br.puc.estoquehosp.modelo.Entradas.NovoUsuario;
import br.puc.estoquehosp.socket.comum.PedidoDeCadastroDeUsuario;
import br.puc.estoquehosp.socket.comum.PedidoDeLogin;
import br.puc.estoquehosp.socket.comum.PedidoDeLogout;
import br.puc.estoquehosp.socket.comum.PedidoDeUsuarioAtual;

// Só traduz o HTTP em pedidos para o Servidor de sockets. As regras ficam lá.
public class UsuarioControlador {

    private final PonteSocket servidor;

    public UsuarioControlador(PonteSocket servidor) {
        this.servidor = servidor;
    }

    public void registrar(Roteador r) {
        r.rota("POST", "/api/usuarios", req -> {
            NovoUsuario dados = req.corpo(NovoUsuario.class);
            return servidor.pedir(new PedidoDeCadastroDeUsuario(dados.nome(), dados.matricula(), dados.perfil(), dados.senha()));
        });
        r.rota("POST", "/api/login", req -> {
            Login dados = req.corpo(Login.class);
            return servidor.pedir(new PedidoDeLogin(dados.matricula(), dados.senha()));
        });
        r.rota("POST", "/api/logout", req -> servidor.pedir(new PedidoDeLogout(req.token())));
        r.rota("GET", "/api/eu", req -> servidor.pedir(new PedidoDeUsuarioAtual(req.token())));
    }
}
