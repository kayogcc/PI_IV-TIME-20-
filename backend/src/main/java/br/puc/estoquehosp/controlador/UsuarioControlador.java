package br.puc.estoquehosp.controlador;

import br.puc.estoquehosp.http.Resposta;
import br.puc.estoquehosp.http.Roteador;
import br.puc.estoquehosp.modelo.Entradas.Login;
import br.puc.estoquehosp.modelo.Entradas.NovoUsuario;
import br.puc.estoquehosp.servico.UsuarioServico;

public class UsuarioControlador {

    private final UsuarioServico servico;

    public UsuarioControlador(UsuarioServico servico) {
        this.servico = servico;
    }

    public void registrar(Roteador r) {
        r.publica("POST", "/api/usuarios", req -> Resposta.criado(servico.cadastrar(req.corpo(NovoUsuario.class))));
        r.publica("POST", "/api/login", req -> servico.entrar(req.corpo(Login.class)));
        r.logado("POST", "/api/logout", req -> {
            servico.sair(req.token());
            return Resposta.semConteudo();
        });
        r.logado("GET", "/api/eu", req -> req.usuario());
    }
}
