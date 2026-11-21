// Autores: Bruno Lobo de Jesus RA: 25019830 (rotas da API e gestão de usuários), Gabriel Albasini RA: 25019916 (envio dos pedidos ao servidor de sockets)

// Palavras-chave: controlador · cadastro · login · logout · /api/eu (usuário atual) · listar/alterar usuários (Gestão)
package br.puc.estoquehosp.controlador;

import br.puc.estoquehosp.http.PonteSocket;
import br.puc.estoquehosp.http.Roteador;
import br.puc.estoquehosp.modelo.Entradas.AlteracaoDeUsuario;
import br.puc.estoquehosp.modelo.Entradas.Login;
import br.puc.estoquehosp.modelo.Entradas.NovoUsuario;
import br.puc.estoquehosp.socket.comum.PedidoDeAlteracaoDeUsuario;
import br.puc.estoquehosp.socket.comum.PedidoDeCadastroDeUsuario;
import br.puc.estoquehosp.socket.comum.PedidoDeLogin;
import br.puc.estoquehosp.socket.comum.PedidoDeLogout;
import br.puc.estoquehosp.socket.comum.PedidoDeUsuarioAtual;
import br.puc.estoquehosp.socket.comum.PedidoDeUsuarios;

// Só traduz o HTTP em pedidos para o Servidor de sockets. As regras ficam lá.
// Quem confere o perfil (só Gestão lista e altera usuários) é o Servidor.
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
        r.rota("GET", "/api/usuarios", req -> servidor.pedir(new PedidoDeUsuarios(req.token())));
        r.rota("PATCH", "/api/usuarios/{id}", req -> {
            AlteracaoDeUsuario dados = req.corpo(AlteracaoDeUsuario.class);
            return servidor.pedir(new PedidoDeAlteracaoDeUsuario(req.token(), req.caminho("id"), dados.perfil(), dados.ativo()));
        });
        r.rota("POST", "/api/login", req -> {
            Login dados = req.corpo(Login.class);
            return servidor.pedir(new PedidoDeLogin(dados.matricula(), dados.senha()));
        });
        r.rota("POST", "/api/logout", req -> servidor.pedir(new PedidoDeLogout(req.token())));
        r.rota("GET", "/api/eu", req -> servidor.pedir(new PedidoDeUsuarioAtual(req.token())));
    }
}
