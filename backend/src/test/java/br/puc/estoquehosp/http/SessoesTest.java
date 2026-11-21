// Autor: Carlos Eduardo RA: 25020992

package br.puc.estoquehosp.http;

import br.puc.estoquehosp.modelo.Perfil;
import br.puc.estoquehosp.modelo.Usuario;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessoesTest {

    @Test
    void deveAbrirSessaoERecuperarUsuario() {
        Sessoes sessoes = new Sessoes();
        Usuario usuario = new Usuario("1", "Ana Souza", "ana", Perfil.ALMOXARIFADO);

        String token = sessoes.abrir(usuario);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals(usuario, sessoes.usuarioDoToken(token).orElseThrow());
    }

    @Test
    void deveEncerrarSessao() {
        Sessoes sessoes = new Sessoes();
        String token = sessoes.abrir(
                new Usuario("1", "Ana Souza", "ana", Perfil.GESTAO)
        );

        sessoes.fechar(token);

        assertTrue(sessoes.usuarioDoToken(token).isEmpty());
    }

    @Test
    void tokenNuloOuDesconhecidoNaoDeveAutenticar() {
        Sessoes sessoes = new Sessoes();

        assertTrue(sessoes.usuarioDoToken(null).isEmpty());
        assertTrue(sessoes.usuarioDoToken("token-inexistente").isEmpty());
    }
}
