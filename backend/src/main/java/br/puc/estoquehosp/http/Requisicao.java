package br.puc.estoquehosp.http;

import br.puc.estoquehosp.modelo.Usuario;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

// O que a ação do controlador recebe: parâmetros do caminho, da URL, corpo JSON e usuário logado.
public class Requisicao {

    private static final int TAMANHO_MAXIMO_CORPO = 64 * 1024;

    private final HttpExchange troca;
    private final Map<String, String> parametrosCaminho;
    private final Map<String, String> parametrosUrl;
    private final Usuario usuario;
    private final String token;
    private final Gson gson;

    Requisicao(HttpExchange troca, Map<String, String> parametrosCaminho, Usuario usuario, String token, Gson gson) {
        this.troca = troca;
        this.parametrosCaminho = parametrosCaminho;
        this.parametrosUrl = lerParametrosUrl(troca.getRequestURI().getRawQuery());
        this.usuario = usuario;
        this.token = token;
        this.gson = gson;
    }

    // Ex.: em /api/materiais/{id}, caminho("id")
    public String caminho(String nome) {
        return parametrosCaminho.get(nome);
    }

    // Ex.: em /api/materiais?busca=gaze, url("busca")
    public String url(String nome) {
        return parametrosUrl.get(nome);
    }

    public Usuario usuario() {
        return usuario;
    }

    public String token() {
        return token;
    }

    public <T> T corpo(Class<T> tipo) {
        try (InputStream entrada = troca.getRequestBody()) {
            byte[] bytes = entrada.readNBytes(TAMANHO_MAXIMO_CORPO + 1);
            if (bytes.length > TAMANHO_MAXIMO_CORPO) {
                throw new ErroApi(413, "Corpo da requisição muito grande.");
            }
            T objeto = gson.fromJson(new String(bytes, StandardCharsets.UTF_8), tipo);
            if (objeto == null) throw ErroApi.requisicaoInvalida("Envie os dados em JSON no corpo da requisição.");
            return objeto;
        } catch (JsonParseException e) {
            throw ErroApi.requisicaoInvalida("JSON inválido.");
        } catch (IOException e) {
            throw ErroApi.requisicaoInvalida("Não foi possível ler o corpo da requisição.");
        }
    }

    private static Map<String, String> lerParametrosUrl(String query) {
        Map<String, String> parametros = new HashMap<>();
        if (query == null || query.isBlank()) return parametros;
        for (String par : query.split("&")) {
            String[] partes = par.split("=", 2);
            String nome = URLDecoder.decode(partes[0], StandardCharsets.UTF_8);
            String valor = partes.length > 1 ? URLDecoder.decode(partes[1], StandardCharsets.UTF_8) : "";
            parametros.put(nome, valor);
        }
        return parametros;
    }
}
