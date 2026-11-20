// Gabriel Figueira Albasini
// Palavras-chave: controlador · rotas GET /api/alertas e /api/dashboard · HTTP -> pedido de socket
package br.puc.estoquehosp.controlador;

import br.puc.estoquehosp.http.PonteSocket;
import br.puc.estoquehosp.http.Roteador;
import br.puc.estoquehosp.socket.comum.PedidoDeAlertas;
import br.puc.estoquehosp.socket.comum.PedidoDeDashboard;

// Só traduz o HTTP em pedidos para o Servidor de sockets.
public class AlertaControlador {

    private final PonteSocket servidor;

    public AlertaControlador(PonteSocket servidor) {
        this.servidor = servidor;
    }

    public void registrar(Roteador r) {
        r.rota("GET", "/api/alertas", req -> servidor.pedir(new PedidoDeAlertas(req.token())));
        r.rota("GET", "/api/dashboard", req -> servidor.pedir(new PedidoDeDashboard(req.token())));
    }
}
