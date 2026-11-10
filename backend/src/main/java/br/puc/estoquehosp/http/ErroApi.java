package br.puc.estoquehosp.http;

// Erro de regra de negócio ou de requisição. Vira uma resposta JSON { "mensagem": ... } com o status informado.
public class ErroApi extends RuntimeException {

    private final int status;

    public ErroApi(int status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public int status() {
        return status;
    }

    public static ErroApi requisicaoInvalida(String mensagem) {
        return new ErroApi(400, mensagem);
    }

    public static ErroApi naoEncontrado(String mensagem) {
        return new ErroApi(404, mensagem);
    }

    public static ErroApi conflito(String mensagem) {
        return new ErroApi(409, mensagem);
    }
}
