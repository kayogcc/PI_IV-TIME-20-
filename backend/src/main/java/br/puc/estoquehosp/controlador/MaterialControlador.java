// Autores: Bruno Lobo(rotas da API), Gabriel Figueira Albasini (envio dos pedidos ao servidor de sockets)

// Palavras-chave: controlador · CRUD de materiais · GET/POST/PUT/DELETE /api/materiais · perfil conferido no Servidor
package br.puc.estoquehosp.controlador;

import br.puc.estoquehosp.http.PonteSocket;
import br.puc.estoquehosp.http.Roteador;
import br.puc.estoquehosp.modelo.Entradas.DadosMaterial;
import br.puc.estoquehosp.socket.comum.PedidoDeCadastroDeMaterial;
import br.puc.estoquehosp.socket.comum.PedidoDeEdicaoDeMaterial;
import br.puc.estoquehosp.socket.comum.PedidoDeInativacaoDeMaterial;
import br.puc.estoquehosp.socket.comum.PedidoDeMaterial;
import br.puc.estoquehosp.socket.comum.PedidoDeMateriais;

// Só traduz o HTTP em pedidos para o Servidor de sockets.
// Quem confere o perfil (Almoxarifado e Gestão para cadastrar/editar/inativar) é o Servidor.
public class MaterialControlador {

    private final PonteSocket servidor;

    public MaterialControlador(PonteSocket servidor) {
        this.servidor = servidor;
    }

    public void registrar(Roteador r) {
        r.rota("GET", "/api/materiais", req -> servidor.pedir(new PedidoDeMateriais(req.token(), req.url("busca"))));
        r.rota("GET", "/api/materiais/{id}", req -> servidor.pedir(new PedidoDeMaterial(req.token(), req.caminho("id"))));
        r.rota("POST", "/api/materiais", req -> {
            DadosMaterial d = req.corpo(DadosMaterial.class);
            return servidor.pedir(new PedidoDeCadastroDeMaterial(req.token(),
                    d.nome(), d.codigoBarras(), d.lote(), d.validade(), d.quantidadeMinima()));
        });
        r.rota("PUT", "/api/materiais/{id}", req -> {
            DadosMaterial d = req.corpo(DadosMaterial.class);
            return servidor.pedir(new PedidoDeEdicaoDeMaterial(req.token(), req.caminho("id"),
                    d.nome(), d.codigoBarras(), d.lote(), d.validade(), d.quantidadeMinima()));
        });
        r.rota("DELETE", "/api/materiais/{id}", req -> servidor.pedir(new PedidoDeInativacaoDeMaterial(req.token(), req.caminho("id"))));
    }
}
