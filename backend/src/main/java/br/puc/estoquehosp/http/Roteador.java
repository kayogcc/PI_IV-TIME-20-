package br.puc.estoquehosp.http;

import br.puc.estoquehosp.modelo.Perfil;
import br.puc.estoquehosp.modelo.Usuario;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Recebe todas as chamadas em /api, encontra a rota, confere o login/perfil e devolve JSON.
public class Roteador implements HttpHandler {

    @FunctionalInterface
    public interface Acao {
        Object executar(Requisicao req);
    }

    // perfis == null: rota pública (não exige login)
    private record Rota(String metodo, Pattern padrao, List<String> nomes, Set<Perfil> perfis, Acao acao) {}

    private static final Pattern PARAMETRO = Pattern.compile("\\{(\\w+)}");

    private final List<Rota> rotas = new ArrayList<>();
    private final Sessoes sessoes;
    private final Gson gson;

    public Roteador(Sessoes sessoes, Gson gson) {
        this.sessoes = sessoes;
        this.gson = gson;
    }

    public void publica(String metodo, String caminho, Acao acao) {
        adicionar(metodo, caminho, null, acao);
    }

    // Sem perfis informados: qualquer usuário logado pode acessar
    public void logado(String metodo, String caminho, Acao acao, Perfil... perfis) {
        Set<Perfil> permitidos = perfis.length == 0 ? EnumSet.allOf(Perfil.class) : EnumSet.copyOf(List.of(perfis));
        adicionar(metodo, caminho, permitidos, acao);
    }

    private void adicionar(String metodo, String caminho, Set<Perfil> perfis, Acao acao) {
        List<String> nomes = new ArrayList<>();
        Matcher m = PARAMETRO.matcher(caminho);
        while (m.find()) nomes.add(m.group(1));
        Pattern padrao = Pattern.compile(m.replaceAll("([^/]+)"));
        rotas.add(new Rota(metodo, padrao, nomes, perfis, acao));
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

            String token = lerToken(troca);
            Usuario usuario = sessoes.usuarioDoToken(token).orElse(null);
            if (rota.perfis() != null) {
                if (usuario == null) throw new ErroApi(401, "Faça login para continuar.");
                if (!rota.perfis().contains(usuario.perfil())) {
                    throw new ErroApi(403, "Seu perfil não tem permissão para esta ação.");
                }
            }
            return rota.acao().executar(new Requisicao(troca, parametros, usuario, token, gson));
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
