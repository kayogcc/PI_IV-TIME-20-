package br.puc.estoquehosp.http;

// Use quando a ação precisa devolver um status diferente de 200 (ex.: 201 Created, 204 No Content).
public record Resposta(int status, Object corpo) {

    public static Resposta criado(Object corpo) {
        return new Resposta(201, corpo);
    }

    public static Resposta semConteudo() {
        return new Resposta(204, null);
    }
}
