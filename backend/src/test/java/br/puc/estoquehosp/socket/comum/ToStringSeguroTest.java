// Gabriel Figueira Albasini
// Palavras-chave: teste de segurança · JUnit · toString não vaza senha, token nem hash
package br.puc.estoquehosp.socket.comum;

import br.puc.estoquehosp.modelo.Entradas.Login;
import br.puc.estoquehosp.modelo.Entradas.NovoUsuario;
import br.puc.estoquehosp.modelo.Perfil;
import br.puc.estoquehosp.modelo.Usuario;
import br.puc.estoquehosp.repositorio.UsuarioRepositorio.Credencial;
import br.puc.estoquehosp.servico.UsuarioServico.LoginFeito;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Garante que nenhum toString mostra senha, token ou hash de senha.
// Esses textos podem ir parar no terminal (ex.: System.out.println(pedido)) e vazar os dados.
class ToStringSeguroTest {

    private static final String SENHA = "SenhaSecreta123";
    private static final String TOKEN = "TokenSecreto-abc123";
    private static final String HASH = "pbkdf2$210000$SalSecreto$HashSecreto";

    private static final Usuario USUARIO = new Usuario("1", "Ana Souza", "ana.souza", Perfil.GESTAO);

    private static void semSegredos(Object objeto) {
        String texto = objeto.toString();
        assertFalse(texto.contains(SENHA), "senha apareceu em: " + texto);
        assertFalse(texto.contains(TOKEN), "token apareceu em: " + texto);
        assertFalse(texto.contains(HASH), "hash da senha apareceu em: " + texto);
    }

    @Test
    void pedidosNaoMostramSenhaNemToken() {
        List<Object> pedidos = List.of(
                new PedidoDeLogin("ana.souza", SENHA),
                new PedidoDeCadastroDeUsuario("Ana Souza", "ana.souza", "GESTAO", SENHA),
                new PedidoAutenticado(TOKEN),
                new PedidoDeLogout(TOKEN),
                new PedidoDeUsuarioAtual(TOKEN),
                new PedidoDeMateriais(TOKEN, "gaze"),
                new PedidoDeMateriais(TOKEN, null),
                new PedidoDeMaterial(TOKEN, "id1"),
                new PedidoDeCadastroDeMaterial(TOKEN, "Gaze", "GZ1", "L1", "2027-01-31", 10),
                new PedidoDeEdicaoDeMaterial(TOKEN, "id1", "Gaze", "GZ1", "L1", "2027-01-31", 10),
                new PedidoDeInativacaoDeMaterial(TOKEN, "id1"),
                new PedidoDeMovimentacao(TOKEN, "GZ1", "Entrada", "Manual", 5, "Almoxarifado A"),
                new PedidoDeHistorico(TOKEN, "7", "id1"),
                new PedidoDeHistorico(TOKEN, null, null),
                new PedidoDeAlertas(TOKEN),
                new PedidoDeDashboard(TOKEN));
        pedidos.forEach(ToStringSeguroTest::semSegredos);
    }

    @Test
    void pedidosContinuamInformativos() {
        assertTrue(new PedidoDeLogin("ana.souza", SENHA).toString().contains("ana.souza"));
        assertTrue(new PedidoDeMovimentacao(TOKEN, "GZ1", "Entrada", "Manual", 5, "Almoxarifado A").toString().contains("GZ1"));
    }

    @Test
    void respostasEDadosNaoMostramSegredos() {
        LoginFeito login = new LoginFeito(TOKEN, USUARIO);
        semSegredos(login);
        semSegredos(new Resultado(200, null, login)); // o Resultado mostra os dados no toString
        semSegredos(new NovoUsuario("Ana Souza", "ana.souza", "GESTAO", SENHA));
        semSegredos(new Login("ana.souza", SENHA));
        semSegredos(new Credencial(USUARIO, HASH));
    }
}
