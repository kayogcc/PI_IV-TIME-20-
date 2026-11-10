package br.puc.estoquehosp.http;

import br.puc.estoquehosp.modelo.Usuario;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// Guarda os tokens de login em memória. Reiniciar o servidor desloga todo mundo.
public class Sessoes {

    private static final Duration VALIDADE = Duration.ofHours(8);

    private record Sessao(Usuario usuario, Instant expiraEm) {}

    private final Map<String, Sessao> sessoes = new ConcurrentHashMap<>();
    private final SecureRandom aleatorio = new SecureRandom();

    public String abrir(Usuario usuario) {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessoes.put(token, new Sessao(usuario, Instant.now().plus(VALIDADE)));
        return token;
    }

    public Optional<Usuario> usuarioDoToken(String token) {
        if (token == null) return Optional.empty();
        Sessao sessao = sessoes.get(token);
        if (sessao == null) return Optional.empty();
        if (sessao.expiraEm().isBefore(Instant.now())) {
            sessoes.remove(token);
            return Optional.empty();
        }
        return Optional.of(sessao.usuario());
    }

    public void fechar(String token) {
        if (token != null) sessoes.remove(token);
    }
}
