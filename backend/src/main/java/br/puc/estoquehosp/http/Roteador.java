// Autor: Gabriel Albasini RA: 25019916
// Palavras-chave: roteamento · HttpHandler · regex nas rotas · 404/405 · Authorization: Bearer · resposta JSON
package br.puc.estoquehosp.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Recebe todas as chamadas em /api, encontra a rota e devolve JSON.
// Não confere login nem perfil aqui. Quem decide é o Servidor de sockets (SupervisoraDeConexao),
// que recebe o token dentro de cada pedido.
public class Roteador implements HttpHandler {

    @FunctionalInterface
    public interface Acao {
        Object executar(Requisicao req);
    }

    private record Rota(String metodo, Pattern padrao, List<String> nomes, Acao acao) {}

    private static final Pattern PARAMETRO = Pattern.compile("\\{(\\w+)}");

    private final List<Rota> rotas = new ArrayList<>();
    private final Gson gson;

    public Roteador(Gson gson) {
        this.gson = gson;
    }

    public void rota(String metodo, String caminho, Acao acao) {
        List<String> nomes = new ArrayList<>();
        Matcher m = PARAMETRO.matcher(caminho);
        while (m.find()) nomes.add(m.group(1));
        Pattern padrao = Pattern.compile(m.replaceAll("([^/]+)"));
        rotas.add(new Rota(metodo, padrao, nomes, acao));
    }

    @Override
    public void handle(HttpExchange troca) throws IOException {
        try {
            Object resultado = despachar(troca);
            if (resultado instanceof Resposta r) enviar(troca, r.status(), r.corpo());
            else enviar(troca, 200, resultado);
        } catch (ErroApi e) {
            enviar(troca, e.status(), Map.of("mensagem", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Erro em " + troca.getRequestMethod() + " " + troca.getRequestURI());
            e.printStackTrace();
            enviar(troca, 500, Map.of("mensagem", "Erro interno no servidor."));
        } finally {
            troca.close();
        }
    }

    private Object despachar(HttpExchange troca) {
        String caminho = troca.getRequestURI().getPath().replaceAll("/+$", "");
        boolean caminhoExiste = false;

        for (Rota rota : rotas) {
            Matcher m = rota.padrao().matcher(caminho);
            if (!m.matches()) continue;
            caminhoExiste = true;
            if (!rota.metodo().equals(troca.getRequestMethod())) continue;

            Map<String, String> parametros = new HashMap<>();
            for (int i = 0; i < rota.nomes().size(); i++) parametros.put(rota.nomes().get(i), m.group(i + 1));

            return rota.acao().executar(new Requisicao(troca, parametros, lerToken(troca), gson));
        }

        if (caminhoExiste) throw new ErroApi(405, "Método não permitido.");
        throw ErroApi.naoEncontrado("Rota não encontrada.");
    }

    // Header esperado: Authorization: Bearer <token>
    private static String lerToken(HttpExchange troca) {
        String header = troca.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return null;
        return header.substring(7).trim();
    }

    private void enviar(HttpExchange troca, int status, Object corpo) throws IOException {
        if (corpo == null) {
            troca.sendResponseHeaders(status, -1);
            return;
        }
        byte[] bytes = gson.toJson(corpo).getBytes(StandardCharsets.UTF_8);
        troca.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        troca.sendResponseHeaders(status, bytes.length);
        try (OutputStream saida = troca.getResponseBody()) {
            saida.write(bytes);
        }
    }
}
