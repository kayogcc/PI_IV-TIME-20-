// Autores: Carlos Eduardo RA: 25020992, Bruno Lobo de Jesus RA: 25019830 (encerrar as sessões de um usuário)

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
    void deveEncerrarTodasAsSessoesDoUsuario() {
        Sessoes sessoes = new Sessoes();
        Usuario ana = new Usuario("1", "Ana Souza", "ana", Perfil.ALMOXARIFADO);
        String celular = sessoes.abrir(ana);
        String computador = sessoes.abrir(ana);
        String outraPessoa = sessoes.abrir(new Usuario("2", "Carlos Lima", "carlos", Perfil.GESTAO));

        sessoes.fecharDoUsuario("1");

        assertTrue(sessoes.usuarioDoToken(celular).isEmpty());
        assertTrue(sessoes.usuarioDoToken(computador).isEmpty());
        assertTrue(sessoes.usuarioDoToken(outraPessoa).isPresent());
    }

    @Test
    void tokenNuloOuDesconhecidoNaoDeveAutenticar() {
        Sessoes sessoes = new Sessoes();

        assertTrue(sessoes.usuarioDoToken(null).isEmpty());
        assertTrue(sessoes.usuarioDoToken("token-inexistente").isEmpty());
    }
}
