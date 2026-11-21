// Autor: Gabriel Albasini RA: 25019916

// Palavras-chave: ponte HTTP -> socket · ObjectOutputStream/ObjectInputStream · Parceiro · espie/envie · Resultado · erro 503
package br.puc.estoquehosp.http;

import br.puc.estoquehosp.socket.comum.Comunicado;
import br.puc.estoquehosp.socket.comum.ComunicadoDeDesligamento;
import br.puc.estoquehosp.socket.comum.Parceiro;
import br.puc.estoquehosp.socket.comum.PedidoParaSair;
import br.puc.estoquehosp.socket.comum.Resultado;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

// Cliente do servidor de sockets usado pela API HTTP.
// O navegador não fala socket com objetos Java, então a API faz a ponte:
// recebe o pedido HTTP do site, manda um Comunicado ao Servidor e devolve o Resultado como resposta HTTP.
// Cada requisição HTTP abre uma conexão, como um Cliente do Fazedor de Continhas que faz um pedido e sai.
public class PonteSocket {

    private static final int ESPERA_MAXIMA_MS = 30_000;

    private final String host;
    private final int porta;

    public PonteSocket(String host, int porta) {
        this.host = host;
        this.porta = porta;
    }

    // Envia o pedido e devolve a resposta HTTP: Resposta com o status do Resultado, ou ErroApi se deu erro
    public Resposta pedir(Comunicado pedido) {
        Resultado resultado = enviar(pedido);
        if (!resultado.getDeuCerto()) throw new ErroApi(resultado.getStatus(), resultado.getMensagem());
        return new Resposta(resultado.getStatus(), resultado.getDados());
    }

    private Resultado enviar(Comunicado pedido) {
        Parceiro servidor;
        try {
            Socket conexao = new Socket(host, porta);
            conexao.setSoTimeout(ESPERA_MAXIMA_MS);
            ObjectOutputStream transmissor = new ObjectOutputStream(conexao.getOutputStream());
            ObjectInputStream receptor = new ObjectInputStream(conexao.getInputStream());
            servidor = new Parceiro(conexao, receptor, transmissor);
        } catch (Exception erro) {
            throw new ErroApi(503, "O servidor do estoque não está respondendo. Verifique se ele está rodando.");
        }

        try {
            // Mesmo jeito do Cliente do Fazedor de Continhas: envia e espia até chegar o Resultado
            servidor.receba(pedido);
            Comunicado comunicado;
            do {
                comunicado = servidor.espie();
                if (comunicado instanceof ComunicadoDeDesligamento) {
                    throw new ErroApi(503, "O servidor do estoque foi desligado.");
                }
            } while (!(comunicado instanceof Resultado));
            return (Resultado) servidor.envie();
        } catch (ErroApi erro) {
            throw erro;
        } catch (Exception erro) {
            throw new ErroApi(503, "Falha na comunicação com o servidor do estoque.");
        } finally {
            try {
                servidor.receba(new PedidoParaSair());
                servidor.adeus();
            } catch (Exception erro) {
                // a conexão já pode ter caído; não há o que fazer
            }
        }
    }
}
