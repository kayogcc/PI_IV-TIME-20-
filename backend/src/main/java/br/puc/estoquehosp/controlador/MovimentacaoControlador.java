package br.puc.estoquehosp.controlador;

import br.puc.estoquehosp.http.Resposta;
import br.puc.estoquehosp.http.Roteador;
import br.puc.estoquehosp.modelo.Entradas.NovaMovimentacao;
import br.puc.estoquehosp.servico.MovimentacaoServico;

import static br.puc.estoquehosp.modelo.Perfil.ALMOXARIFADO;
import static br.puc.estoquehosp.modelo.Perfil.CENTRO_CIRURGICO;

// Registro: Almoxarifado (entrada e saída) e Centro Cirúrgico (só saída, conferido no serviço).
public class MovimentacaoControlador {

    private final MovimentacaoServico servico;

    public MovimentacaoControlador(MovimentacaoServico servico) {
        this.servico = servico;
    }

    public void registrar(Roteador r) {
        r.logado("GET", "/api/movimentacoes", req -> servico.listar(req.url("dias"), req.url("materialId")));
        r.logado("POST", "/api/movimentacoes",
                req -> Resposta.criado(servico.registrar(req.corpo(NovaMovimentacao.class), req.usuario())),
                ALMOXARIFADO, CENTRO_CIRURGICO);
    }
}
