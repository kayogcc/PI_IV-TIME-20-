package br.puc.estoquehosp.controlador;

import br.puc.estoquehosp.http.Resposta;
import br.puc.estoquehosp.http.Roteador;
import br.puc.estoquehosp.modelo.Entradas.DadosMaterial;
import br.puc.estoquehosp.servico.MaterialServico;

import static br.puc.estoquehosp.modelo.Perfil.ALMOXARIFADO;
import static br.puc.estoquehosp.modelo.Perfil.GESTAO;

// Consulta: todos os perfis. Cadastro, edição e inativação: Almoxarifado e Gestão.
public class MaterialControlador {

    private final MaterialServico servico;

    public MaterialControlador(MaterialServico servico) {
        this.servico = servico;
    }

    public void registrar(Roteador r) {
        r.logado("GET", "/api/materiais", req -> servico.listar(req.url("busca")));
        r.logado("GET", "/api/materiais/{id}", req -> servico.buscar(req.caminho("id")));
        r.logado("POST", "/api/materiais",
                req -> Resposta.criado(servico.criar(req.corpo(DadosMaterial.class))), ALMOXARIFADO, GESTAO);
        r.logado("PUT", "/api/materiais/{id}",
                req -> servico.atualizar(req.caminho("id"), req.corpo(DadosMaterial.class)), ALMOXARIFADO, GESTAO);
        r.logado("DELETE", "/api/materiais/{id}", req -> {
            servico.inativar(req.caminho("id"));
            return Resposta.semConteudo();
        }, ALMOXARIFADO, GESTAO);
    }
}
