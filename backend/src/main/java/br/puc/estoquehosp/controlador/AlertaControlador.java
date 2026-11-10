package br.puc.estoquehosp.controlador;

import br.puc.estoquehosp.http.Roteador;
import br.puc.estoquehosp.servico.AlertaServico;

public class AlertaControlador {

    private final AlertaServico servico;

    public AlertaControlador(AlertaServico servico) {
        this.servico = servico;
    }

    public void registrar(Roteador r) {
        r.logado("GET", "/api/alertas", req -> servico.listar());
        r.logado("GET", "/api/dashboard", req -> servico.resumo());
    }
}
