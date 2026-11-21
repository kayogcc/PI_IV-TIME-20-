// Autores: Bruno Lobo de Jesus RA: 25019830 (rotas da API), Gabriel Albasini RA: 25019916 (envio dos pedidos ao servidor de sockets)

// Palavras-chave: controlador · entrada/saída · GET/POST /api/movimentacoes · histórico
package br.puc.estoquehosp.controlador;

import br.puc.estoquehosp.http.PonteSocket;
import br.puc.estoquehosp.http.Roteador;
import br.puc.estoquehosp.modelo.Entradas.NovaMovimentacao;
import br.puc.estoquehosp.socket.comum.PedidoDeHistorico;
import br.puc.estoquehosp.socket.comum.PedidoDeMovimentacao;

// Só traduz o HTTP em pedidos para o Servidor de sockets.
// Quem confere perfil e saldo é o Servidor.
public class MovimentacaoControlador {

    private final PonteSocket servidor;

    public MovimentacaoControlador(PonteSocket servidor) {
        this.servidor = servidor;
    }

    public void registrar(Roteador r) {
        r.rota("GET", "/api/movimentacoes",
                req -> servidor.pedir(new PedidoDeHistorico(req.token(), req.url("dias"), req.url("materialId"))));
        r.rota("POST", "/api/movimentacoes", req -> {
            NovaMovimentacao d = req.corpo(NovaMovimentacao.class);
            return servidor.pedir(new PedidoDeMovimentacao(req.token(),
                    d.material(), d.tipo(), d.origem(), d.quantidade(), d.local()));
        });
    }
}
