// Autores: Carlos Eduardo RA: 25020992, Gabriel Albasini RA: 25019916, Bruno Lobo de Jesus RA: 25019830 (testes da gestão de usuários)

package br.puc.estoquehosp.servico;

import br.puc.estoquehosp.banco.ConexaoMongo;
import br.puc.estoquehosp.controlador.MaterialControlador;
import br.puc.estoquehosp.controlador.MovimentacaoControlador;
import br.puc.estoquehosp.controlador.UsuarioControlador;
import br.puc.estoquehosp.http.ErroApi;
import br.puc.estoquehosp.http.PonteSocket;
import br.puc.estoquehosp.http.Roteador;
import br.puc.estoquehosp.http.Sessoes;
import br.puc.estoquehosp.modelo.Entradas.AlteracaoDeUsuario;
import br.puc.estoquehosp.modelo.Entradas.DadosMaterial;
import br.puc.estoquehosp.modelo.Entradas.Login;
import br.puc.estoquehosp.modelo.Entradas.NovaMovimentacao;
import br.puc.estoquehosp.modelo.Entradas.NovoUsuario;
import br.puc.estoquehosp.modelo.Material;
import br.puc.estoquehosp.modelo.Movimentacao;
import br.puc.estoquehosp.modelo.Perfil;
import br.puc.estoquehosp.modelo.Usuario;
import br.puc.estoquehosp.repositorio.MaterialRepositorio;
import br.puc.estoquehosp.repositorio.MovimentacaoRepositorio;
import br.puc.estoquehosp.repositorio.UsuarioRepositorio;
import br.puc.estoquehosp.socket.comum.Parceiro;
import br.puc.estoquehosp.socket.servidor.AceitadoraDeConexao;
import br.puc.estoquehosp.socket.servidor.Servicos;
import com.google.gson.Gson;
import com.mongodb.client.MongoDatabase;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Testes das regras de negócio do back-end, agrupados em um único arquivo:
 * materiais, movimentações, usuários/login, permissões por perfil e histórico.
 *
 * Dois tipos de teste:
 *  - SEM BANCO: validações que acontecem antes de qualquer acesso ao MongoDB (rodam em qualquer computador).
 *  - COM BANCO: chamam ambiente(), que só roda se a variável MONGO_URI estiver definida (senão o teste é
 *    ignorado, como no ConexaoTest). Usam o banco "estoquehosp_teste" e limpam as coleções a cada teste,
 *    então nunca encostam nos dados reais.
 */
class ServicosTest {

    // ---------------------------------------------------------------- apoio

    private static final String ENTRADA = MovimentacaoServico.ENTRADA;
    private static final String SAIDA = MovimentacaoServico.SAIDA;
    private static final String LOCAL_A = "Almoxarifado A";
    private static final String LOCAL_B = "Almoxarifado B";

    private static final Usuario ALMOX = new Usuario("1", "Ana Souza", "ana.souza", Perfil.ALMOXARIFADO);
    private static final Usuario CENTRO = new Usuario("2", "Carlos Lima", "carlos.lima", Perfil.CENTRO_CIRURGICO);

    private static ConexaoMongo conexao;

    /** Serviços e repositórios ligados ao banco de teste, já com as coleções vazias. */
    record Ambiente(MaterialRepositorio materiais, MovimentacaoRepositorio historico, UsuarioRepositorio usuarios,
                    Sessoes sessoes, MaterialServico materialServico, MovimentacaoServico movimentacaoServico,
                    UsuarioServico usuarioServico) {}

    private static synchronized Ambiente ambiente() {
        String uri = System.getenv("MONGO_URI");
        assumeTrue(uri != null && !uri.isBlank(), "MONGO_URI não definida: teste com banco ignorado");
        if (conexao == null) conexao = new ConexaoMongo(uri, "estoquehosp_teste");

        MongoDatabase banco = conexao.banco();
        banco.getCollection("materiais").drop();
        banco.getCollection("movimentacoes").drop();
        banco.getCollection("usuarios").drop();

        MaterialRepositorio materiais = new MaterialRepositorio(banco);
        MovimentacaoRepositorio historico = new MovimentacaoRepositorio(banco);
        UsuarioRepositorio usuarios = new UsuarioRepositorio(banco);
        Sessoes sessoes = new Sessoes();
        return new Ambiente(materiais, historico, usuarios, sessoes,
                new MaterialServico(materiais),
                new MovimentacaoServico(materiais, historico),
                new UsuarioServico(usuarios, sessoes));
    }

    @AfterAll
    static void fecharConexao() {
        if (conexao != null) conexao.close();
    }

    private static void assertStatus(int esperado, Executable acao) {
        ErroApi erro = assertThrows(ErroApi.class, acao);
        assertEquals(esperado, erro.status(), "mensagem recebida: " + erro.getMessage());
    }

    private static Material criarMaterial(Ambiente a, String nome, String codigo) {
        return a.materialServico().criar(new DadosMaterial(nome, codigo, null, null, 10));
    }

    private static NovaMovimentacao mov(String material, String tipo, int quantidade, String local) {
        return new NovaMovimentacao(material, tipo, "Manual", quantidade, local);
    }

    private static int saldo(Ambiente a, String idMaterial, String local) {
        return a.materialServico().buscar(idMaterial).saldos().getOrDefault(local, 0);
    }

    // ============================================================ 1. MATERIAIS

    @Nested
    @DisplayName("Cadastro de materiais")
    class Materiais {

        // ---- sem banco: validações de campo

        @ParameterizedTest(name = "nome \"{0}\" é rejeitado")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "A"})
        void nomeObrigatorio(String nome) {
            MaterialServico servico = new MaterialServico(null);
            assertStatus(400, () -> servico.criar(new DadosMaterial(nome, null, null, null, 10)));
        }

        @Test
        @DisplayName("Nome com mais de 100 caracteres é rejeitado")
        void nomeMuitoLongo() {
            MaterialServico servico = new MaterialServico(null);
            assertStatus(400, () -> servico.criar(new DadosMaterial("x".repeat(101), null, null, null, 10)));
        }

        @ParameterizedTest(name = "quantidade mínima {0} é rejeitada")
        @NullSource
        @ValueSource(ints = {-1, 100_001})
        void quantidadeMinimaInvalida(Integer minimo) {
            MaterialServico servico = new MaterialServico(null);
            assertStatus(400, () -> servico.criar(new DadosMaterial("Gaze estéril", null, null, null, minimo)));
        }

        @ParameterizedTest(name = "código de barras \"{0}\" é rejeitado")
        @ValueSource(strings = {"abc", "ab cd", "abc#123"})
        void codigoDeBarrasInvalido(String codigo) {
            MaterialServico servico = new MaterialServico(null);
            assertStatus(400, () -> servico.criar(new DadosMaterial("Gaze estéril", codigo, null, null, 10)));
        }

        @ParameterizedTest(name = "validade \"{0}\" é rejeitada")
        @ValueSource(strings = {"31/12/2026", "2026-13-01", "amanhã"})
        void validadeInvalida(String validade) {
            MaterialServico servico = new MaterialServico(null);
            assertStatus(400, () -> servico.criar(new DadosMaterial("Gaze estéril", null, null, validade, 10)));
        }

        @Test
        @DisplayName("Lote com mais de 30 caracteres é rejeitado")
        void loteMuitoLongo() {
            MaterialServico servico = new MaterialServico(null);
            assertStatus(400, () -> servico.criar(new DadosMaterial("Gaze estéril", null, "L".repeat(31), null, 10)));
        }

        // ---- com banco

        @Test
        @DisplayName("Cadastra material válido, com espaços limpos e saldo zerado")
        void cadastraMaterialValido() {
            Ambiente a = ambiente();

            Material criado = a.materialServico().criar(
                    new DadosMaterial("  Gaze   estéril ", "GAZE-001", "L123", "2027-05-10", 20));

            assertNotNull(criado.id());
            assertEquals("Gaze estéril", criado.nome());
            assertEquals("GAZE-001", criado.codigoBarras());
            assertEquals(20, criado.quantidadeMinima());
            assertEquals(0, criado.quantidade(), "material novo só ganha saldo por movimentação");
            assertTrue(criado.ativo());
            assertEquals(criado, a.materialServico().buscar(criado.id()));
        }

        @Test
        @DisplayName("Rejeita nome repetido, ignorando maiúsculas e acentos")
        void impedeNomeDuplicado() {
            Ambiente a = ambiente();
            criarMaterial(a, "Gaze estéril", null);

            assertStatus(409, () -> criarMaterial(a, "GAZE ESTERIL", null));
            assertEquals(1, a.materialServico().listar(null).size());
        }

        @Test
        @DisplayName("Rejeita código de barras repetido")
        void impedeCodigoDuplicado() {
            Ambiente a = ambiente();
            criarMaterial(a, "Gaze estéril", "GAZE-001");

            assertStatus(409, () -> criarMaterial(a, "Atadura", "GAZE-001"));
        }

        @Test
        @DisplayName("Nome e código de material inativado podem ser reaproveitados")
        void reaproveitaNomeAposInativar() {
            Ambiente a = ambiente();
            Material antigo = criarMaterial(a, "Gaze estéril", "GAZE-001");

            a.materialServico().inativar(antigo.id());

            Material novo = criarMaterial(a, "Gaze estéril", "GAZE-001");
            assertNotEquals(antigo.id(), novo.id());
            assertEquals(1, a.materialServico().listar(null).size());
        }

        @Test
        @DisplayName("Edição aceita o próprio nome, mas não o nome de outro material")
        void atualizarRespeitaDuplicidade() {
            Ambiente a = ambiente();
            Material gaze = criarMaterial(a, "Gaze estéril", null);
            criarMaterial(a, "Atadura", null);

            Material editado = a.materialServico().atualizar(gaze.id(), new DadosMaterial("Gaze estéril", null, "L9", null, 5));
            assertEquals("L9", editado.lote());
            assertEquals(5, editado.quantidadeMinima());

            assertStatus(409, () -> a.materialServico().atualizar(gaze.id(), new DadosMaterial("atadura", null, null, null, 5)));
        }

        @Test
        @DisplayName("Material inativo some da lista e não pode ser editado")
        void inativarEscondeMaterial() {
            Ambiente a = ambiente();
            Material gaze = criarMaterial(a, "Gaze estéril", null);

            a.materialServico().inativar(gaze.id());

            assertTrue(a.materialServico().listar(null).isEmpty());
            assertStatus(404, () -> a.materialServico().atualizar(gaze.id(), new DadosMaterial("Gaze", null, null, null, 1)));
        }

        @Test
        @DisplayName("Buscar id inexistente ou inválido retorna 404")
        void buscarInexistente() {
            Ambiente a = ambiente();

            assertStatus(404, () -> a.materialServico().buscar("000000000000000000000000"));
            assertStatus(404, () -> a.materialServico().buscar("id-invalido"));
        }

        @Test
        @DisplayName("Busca na lista ignora acentos e maiúsculas, por nome ou código")
        void buscaSemAcento() {
            Ambiente a = ambiente();
            criarMaterial(a, "Gaze Estéril", "GAZE-001");
            criarMaterial(a, "Seringa 10ml", "SER-010");

            assertEquals(1, a.materialServico().listar("gaze esteril").size());
            assertEquals("Seringa 10ml", a.materialServico().listar("ser-010").get(0).nome());
            assertEquals(2, a.materialServico().listar("").size());
            assertTrue(a.materialServico().listar("luva").isEmpty());
        }
    }

    // ========================================================= 2. MOVIMENTAÇÕES

    @Nested
    @DisplayName("Movimentações de estoque")
    class Movimentacoes {

        private final MovimentacaoServico semBanco = new MovimentacaoServico(null, null);

        // ---- sem banco: validações

        @ParameterizedTest(name = "tipo \"{0}\" é rejeitado")
        @ValueSource(strings = {"", "Transferência", "entrada"})
        void tipoInvalido(String tipo) {
            assertStatus(400, () -> semBanco.registrar(
                    new NovaMovimentacao("Gaze", tipo, "Manual", 1, LOCAL_A), ALMOX));
        }

        @Test
        @DisplayName("Origem e local inválidos são rejeitados")
        void origemELocalInvalidos() {
            assertStatus(400, () -> semBanco.registrar(
                    new NovaMovimentacao("Gaze", ENTRADA, "Telepatia", 1, LOCAL_A), ALMOX));
            assertStatus(400, () -> semBanco.registrar(
                    new NovaMovimentacao("Gaze", ENTRADA, "Manual", 1, "Garagem"), ALMOX));
        }

        @Test
        @DisplayName("Campos ausentes (tipo, origem, local) devem dar 400, e não erro interno")
        void camposAusentes() {
            // Se este teste falhar com NullPointerException, veja o item "Bug encontrado" do guia:
            // List.of(...).contains(null) lança NPE em MovimentacaoServico.registrar.
            assertAll(
                    () -> assertStatus(400, () -> semBanco.registrar(
                            new NovaMovimentacao("Gaze", null, "Manual", 1, LOCAL_A), ALMOX)),
                    () -> assertStatus(400, () -> semBanco.registrar(
                            new NovaMovimentacao("Gaze", ENTRADA, null, 1, LOCAL_A), ALMOX)),
                    () -> assertStatus(400, () -> semBanco.registrar(
                            new NovaMovimentacao("Gaze", ENTRADA, "Manual", 1, null), ALMOX))
            );
        }

        @ParameterizedTest(name = "material \"{0}\" é rejeitado")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void materialObrigatorio(String material) {
            assertStatus(400, () -> semBanco.registrar(mov(material, ENTRADA, 1, LOCAL_A), ALMOX));
        }

        @ParameterizedTest(name = "quantidade {0} é rejeitada")
        @NullSource
        @ValueSource(ints = {0, -5, 100_001})
        void quantidadeInvalida(Integer quantidade) {
            assertStatus(400, () -> semBanco.registrar(
                    new NovaMovimentacao("Gaze", ENTRADA, "Manual", quantidade, LOCAL_A), ALMOX));
        }

        @Test
        @DisplayName("Centro Cirúrgico não pode registrar entrada (403)")
        void centroCirurgicoNaoFazEntrada() {
            assertStatus(403, () -> semBanco.registrar(mov("Gaze", ENTRADA, 1, LOCAL_A), CENTRO));
        }

        // ---- com banco

        @Test
        @DisplayName("Entrada aumenta o saldo do local e devolve o registro")
        void entradaAumentaSaldo() {
            Ambiente a = ambiente();
            Material gaze = criarMaterial(a, "Gaze estéril", null);

            Movimentacao feita = a.movimentacaoServico().registrar(mov("Gaze estéril", ENTRADA, 50, LOCAL_A), ALMOX);

            assertEquals(50, saldo(a, gaze.id(), LOCAL_A));
            assertEquals(50, a.materialServico().buscar(gaze.id()).quantidade());
            assertEquals(ENTRADA, feita.tipo());
            assertEquals(50, feita.quantidade());
            assertEquals("Ana Souza", feita.usuario());
            assertEquals(ALMOX.id(), feita.usuarioId());
        }

        @Test
        @DisplayName("Saída diminui o saldo")
        void saidaDiminuiSaldo() {
            Ambiente a = ambiente();
            Material gaze = criarMaterial(a, "Gaze estéril", null);
            a.movimentacaoServico().registrar(mov(gaze.nome(), ENTRADA, 50, LOCAL_A), ALMOX);

            a.movimentacaoServico().registrar(mov(gaze.nome(), SAIDA, 20, LOCAL_A), ALMOX);

            assertEquals(30, saldo(a, gaze.id(), LOCAL_A));
        }

        @Test
        @DisplayName("Saída maior que o saldo é recusada e não altera nada")
        void saidaMaiorQueSaldo() {
            Ambiente a = ambiente();
            Material gaze = criarMaterial(a, "Gaze estéril", null);
            a.movimentacaoServico().registrar(mov(gaze.nome(), ENTRADA, 10, LOCAL_A), ALMOX);

            assertStatus(409, () -> a.movimentacaoServico().registrar(mov(gaze.nome(), SAIDA, 11, LOCAL_A), ALMOX));

            assertEquals(10, saldo(a, gaze.id(), LOCAL_A));
            assertEquals(1, a.movimentacaoServico().listar(null, gaze.id()).size(), "só a entrada deve estar no histórico");
        }

        @Test
        @DisplayName("Saída do saldo exato é permitida e zera o estoque")
        void saidaDoSaldoExato() {
            Ambiente a = ambiente();
            Material gaze = criarMaterial(a, "Gaze estéril", null);
            a.movimentacaoServico().registrar(mov(gaze.nome(), ENTRADA, 10, LOCAL_A), ALMOX);

            a.movimentacaoServico().registrar(mov(gaze.nome(), SAIDA, 10, LOCAL_A), ALMOX);

            assertEquals(0, saldo(a, gaze.id(), LOCAL_A));
        }

        @Test
        @DisplayName("Cada local tem saldo próprio")
        void saldoPorLocal() {
            Ambiente a = ambiente();
            Material gaze = criarMaterial(a, "Gaze estéril", null);
            a.movimentacaoServico().registrar(mov(gaze.nome(), ENTRADA, 10, LOCAL_A), ALMOX);

            assertStatus(409, () -> a.movimentacaoServico().registrar(mov(gaze.nome(), SAIDA, 1, LOCAL_B), ALMOX));

            a.movimentacaoServico().registrar(mov(gaze.nome(), ENTRADA, 5, LOCAL_B), ALMOX);
            Material atual = a.materialServico().buscar(gaze.id());
            assertEquals(10, atual.saldos().get(LOCAL_A));
            assertEquals(5, atual.saldos().get(LOCAL_B));
            assertEquals(15, atual.quantidade());
        }

        @Test
        @DisplayName("Material é encontrado por código de barras ou por nome; inexistente dá 404")
        void encontraMaterial() {
            Ambiente a = ambiente();
            Material gaze = criarMaterial(a, "Gaze estéril", "GAZE-001");

            a.movimentacaoServico().registrar(mov("GAZE-001", ENTRADA, 5, LOCAL_A), ALMOX);
            a.movimentacaoServico().registrar(mov("gaze esteril", ENTRADA, 5, LOCAL_A), ALMOX);

            assertEquals(10, saldo(a, gaze.id(), LOCAL_A));
            assertStatus(404, () -> a.movimentacaoServico().registrar(mov("Material fantasma", ENTRADA, 1, LOCAL_A), ALMOX));
        }

        @Test
        @DisplayName("Parte do nome ambígua não escolhe material por conta própria")
        void nomeAmbiguoNaoMovimenta() {
            Ambiente a = ambiente();
            criarMaterial(a, "Luva P", null);
            criarMaterial(a, "Luva M", null);

            assertStatus(404, () -> a.movimentacaoServico().registrar(mov("luva", ENTRADA, 1, LOCAL_A), ALMOX));
        }

        @Test
        @DisplayName("Material inativo não pode ser movimentado")
        void materialInativoNaoMovimenta() {
            Ambiente a = ambiente();
            Material gaze = criarMaterial(a, "Gaze estéril", null);
            a.materialServico().inativar(gaze.id());

            assertStatus(404, () -> a.movimentacaoServico().registrar(mov(gaze.nome(), ENTRADA, 1, LOCAL_A), ALMOX));
        }

        @Test
        @DisplayName("Centro Cirúrgico pode registrar saída")
        void centroCirurgicoFazSaida() {
            Ambiente a = ambiente();
            Material gaze = criarMaterial(a, "Gaze estéril", null);
            a.movimentacaoServico().registrar(mov(gaze.nome(), ENTRADA, 10, LOCAL_A), ALMOX);

            Movimentacao feita = a.movimentacaoServico().registrar(mov(gaze.nome(), SAIDA, 3, LOCAL_A), CENTRO);

            assertEquals("Carlos Lima", feita.usuario());
            assertEquals(7, saldo(a, gaze.id(), LOCAL_A));
        }

        @Test
        @DisplayName("Saídas simultâneas nunca deixam o saldo negativo")
        void saidasSimultaneas() throws Exception {
            Ambiente a = ambiente();
            Material seringa = criarMaterial(a, "Seringa 10ml", null);
            a.movimentacaoServico().registrar(mov(seringa.nome(), ENTRADA, 10, LOCAL_A), ALMOX);

            int tentativas = 8; // 8 x 3 un. pedidas, mas só há 10 un.: apenas 3 saídas podem dar certo
            ExecutorService pool = Executors.newFixedThreadPool(tentativas);
            CountDownLatch largada = new CountDownLatch(1);
            List<Future<Boolean>> resultados = new ArrayList<>();
            for (int i = 0; i < tentativas; i++) {
                resultados.add(pool.submit(() -> {
                    largada.await();
                    try {
                        a.movimentacaoServico().registrar(mov(seringa.nome(), SAIDA, 3, LOCAL_A), ALMOX);
                        return true;
                    } catch (ErroApi e) {
                        return false;
                    }
                }));
            }
            largada.countDown();
            int deramCerto = 0;
            for (Future<Boolean> resultado : resultados) {
                if (resultado.get(30, TimeUnit.SECONDS)) deramCerto++;
            }
            pool.shutdown();

            assertEquals(3, deramCerto);
            assertEquals(1, saldo(a, seringa.id(), LOCAL_A));
            assertEquals(4, a.movimentacaoServico().listar(null, seringa.id()).size(), "1 entrada + 3 saídas");
        }
    }

    // ========================================================= 3. USUÁRIOS

    @Nested
    @DisplayName("Usuários e login")
    class Usuarios {

        private final UsuarioServico semBanco = new UsuarioServico(null, new Sessoes());

        // ---- sem banco: validações do cadastro

        @ParameterizedTest(name = "nome \"{0}\" é rejeitado")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "Ana", "Ana S0uza", "Ana Sou_za"})
        void nomeInvalido(String nome) {
            assertStatus(400, () -> semBanco.cadastrar(new NovoUsuario(nome, "ana.souza", "GESTAO", "senha123")));
        }

        @ParameterizedTest(name = "matrícula \"{0}\" é rejeitada")
        @NullAndEmptySource
        @ValueSource(strings = {"ab", "ana souza", "ana@souza"})
        void matriculaInvalida(String matricula) {
            assertStatus(400, () -> semBanco.cadastrar(new NovoUsuario("Ana Souza", matricula, "GESTAO", "senha123")));
        }

        @ParameterizedTest(name = "senha \"{0}\" é rejeitada")
        @NullAndEmptySource
        @ValueSource(strings = {"abc123", "abcdefgh", "12345678"})
        void senhaInvalida(String senha) {
            // curta, sem número e sem letra
            assertStatus(400, () -> semBanco.cadastrar(new NovoUsuario("Ana Souza", "ana.souza", "GESTAO", senha)));
        }

        @Test
        @DisplayName("Senha com mais de 72 caracteres é rejeitada")
        void senhaMuitoLonga() {
            String senha = "a1".repeat(37);
            assertStatus(400, () -> semBanco.cadastrar(new NovoUsuario("Ana Souza", "ana.souza", "GESTAO", senha)));
        }

        @ParameterizedTest(name = "perfil \"{0}\" é rejeitado")
        @NullAndEmptySource
        @ValueSource(strings = {"ADMIN", "gestao"})
        void perfilInvalido(String perfil) {
            assertStatus(400, () -> semBanco.cadastrar(new NovoUsuario("Ana Souza", "ana.souza", perfil, "senha123")));
        }

        // ---- com banco

        @ParameterizedTest(name = "cadastra usuário com perfil {0}")
        @EnumSource(Perfil.class)
        void cadastraEmCadaPerfil(Perfil perfil) {
            Ambiente a = ambiente();

            Usuario criado = a.usuarioServico().cadastrar(
                    new NovoUsuario("  Ana   Souza ", "ana.souza", perfil.name(), "senha123"));

            assertNotNull(criado.id());
            assertEquals("Ana Souza", criado.nome());
            assertEquals(perfil, criado.perfil());
        }

        @Test
        @DisplayName("A senha é guardada como hash, nunca em texto")
        void senhaFicaComHash() {
            Ambiente a = ambiente();
            a.usuarioServico().cadastrar(new NovoUsuario("Ana Souza", "ana.souza", "GESTAO", "senha123"));

            String salvo = a.usuarios().buscarPorMatricula("ana.souza").orElseThrow().hashSenha();

            assertNotEquals("senha123", salvo);
            assertTrue(salvo.startsWith("pbkdf2$"));
        }

        @Test
        @DisplayName("Matrícula repetida é recusada, mesmo mudando maiúsculas")
        void matriculaDuplicada() {
            Ambiente a = ambiente();
            a.usuarioServico().cadastrar(new NovoUsuario("Ana Souza", "ana.souza", "GESTAO", "senha123"));

            assertStatus(409, () -> a.usuarioServico().cadastrar(
                    new NovoUsuario("Outra Pessoa", "ANA.SOUZA", "ALMOXARIFADO", "outra123")));
        }

        @Test
        @DisplayName("Login correto abre sessão válida")
        void loginCorreto() {
            Ambiente a = ambiente();
            a.usuarioServico().cadastrar(new NovoUsuario("Ana Souza", "ana.souza", "ALMOXARIFADO", "senha123"));

            UsuarioServico.LoginFeito feito = a.usuarioServico().entrar(new Login("ana.souza", "senha123"));

            assertFalse(feito.token().isBlank());
            assertEquals(Perfil.ALMOXARIFADO, feito.usuario().perfil());
            assertEquals(feito.usuario(), a.sessoes().usuarioDoToken(feito.token()).orElseThrow());
        }

        @Test
        @DisplayName("Login não diferencia maiúsculas na matrícula")
        void loginSemDiferenciarMaiusculas() {
            Ambiente a = ambiente();
            a.usuarioServico().cadastrar(new NovoUsuario("Ana Souza", "Ana.Souza", "GESTAO", "senha123"));

            assertEquals("Ana.Souza", a.usuarioServico().entrar(new Login("  ana.souza ", "senha123")).usuario().matricula());
        }

        @Test
        @DisplayName("Login incorreto retorna 401 sem revelar se a matrícula existe")
        void loginIncorreto() {
            Ambiente a = ambiente();
            a.usuarioServico().cadastrar(new NovoUsuario("Ana Souza", "ana.souza", "GESTAO", "senha123"));

            ErroApi senhaErrada = assertThrows(ErroApi.class,
                    () -> a.usuarioServico().entrar(new Login("ana.souza", "senha999")));
            ErroApi semUsuario = assertThrows(ErroApi.class,
                    () -> a.usuarioServico().entrar(new Login("ninguem", "senha123")));

            assertEquals(401, senhaErrada.status());
            assertEquals(401, semUsuario.status());
            assertEquals(senhaErrada.getMessage(), semUsuario.getMessage());
            assertStatus(401, () -> a.usuarioServico().entrar(new Login("ana.souza", null)));
            assertStatus(401, () -> a.usuarioServico().entrar(new Login(null, null)));
        }

        @Test
        @DisplayName("Sair invalida o token")
        void sairInvalidaToken() {
            Ambiente a = ambiente();
            a.usuarioServico().cadastrar(new NovoUsuario("Ana Souza", "ana.souza", "GESTAO", "senha123"));
            String token = a.usuarioServico().entrar(new Login("ana.souza", "senha123")).token();

            a.usuarioServico().sair(token);

            assertTrue(a.sessoes().usuarioDoToken(token).isEmpty());
        }

        // ---- gestão de usuários (feita pela Gestão)

        private static final Usuario GESTOR = new Usuario("aaaaaaaaaaaaaaaaaaaaaaaa", "Gil Gestor", "gil.gestor", Perfil.GESTAO);

        @Test
        @DisplayName("Alteração sem perfil nem ativo é rejeitada")
        void alteracaoVazia() {
            assertStatus(400, () -> semBanco.alterar("bbbbbbbbbbbbbbbbbbbbbbbb", new AlteracaoDeUsuario(null, null), GESTOR));
        }

        @ParameterizedTest(name = "novo perfil \"{0}\" é rejeitado")
        @ValueSource(strings = {"", "ADMIN", "gestao"})
        void alteracaoComPerfilInvalido(String perfil) {
            assertStatus(400, () -> semBanco.alterar("bbbbbbbbbbbbbbbbbbbbbbbb", new AlteracaoDeUsuario(perfil, null), GESTOR));
        }

        @Test
        @DisplayName("Ninguém altera a própria conta")
        void naoAlteraAPropriaConta() {
            assertStatus(400, () -> semBanco.alterar(GESTOR.id(), new AlteracaoDeUsuario("ALMOXARIFADO", null), GESTOR));
            assertStatus(400, () -> semBanco.alterar(GESTOR.id(), new AlteracaoDeUsuario(null, false), GESTOR));
        }

        @Test
        @DisplayName("Lista todos os usuários, ativos e inativos, em ordem de nome")
        void listaUsuarios() {
            Ambiente a = ambiente();
            Usuario bruno = a.usuarioServico().cadastrar(new NovoUsuario("Bruno Lobo", "bruno.lobo", "GESTAO", "senha123"));
            a.usuarioServico().cadastrar(new NovoUsuario("Ana Souza", "ana.souza", "ALMOXARIFADO", "senha123"));
            a.usuarioServico().alterar(bruno.id(), new AlteracaoDeUsuario(null, false), GESTOR);

            List<Usuario> lista = a.usuarioServico().listar();

            assertEquals(List.of("Ana Souza", "Bruno Lobo"), lista.stream().map(Usuario::nome).toList());
            assertFalse(lista.get(1).ativo());
        }

        @Test
        @DisplayName("Mudar o perfil derruba a sessão da pessoa, que entra de novo com o perfil novo")
        void mudarPerfil() {
            Ambiente a = ambiente();
            Usuario ana = a.usuarioServico().cadastrar(new NovoUsuario("Ana Souza", "ana.souza", "ALMOXARIFADO", "senha123"));
            String token = a.usuarioServico().entrar(new Login("ana.souza", "senha123")).token();

            Usuario alterada = a.usuarioServico().alterar(ana.id(), new AlteracaoDeUsuario("CENTRO_CIRURGICO", null), GESTOR);

            assertEquals(Perfil.CENTRO_CIRURGICO, alterada.perfil());
            assertTrue(a.sessoes().usuarioDoToken(token).isEmpty());
            assertEquals(Perfil.CENTRO_CIRURGICO, a.usuarioServico().entrar(new Login("ana.souza", "senha123")).usuario().perfil());
        }

        @Test
        @DisplayName("Conta inativa não entra (403) e volta a entrar depois de reativada")
        void inativarEReativar() {
            Ambiente a = ambiente();
            Usuario ana = a.usuarioServico().cadastrar(new NovoUsuario("Ana Souza", "ana.souza", "ALMOXARIFADO", "senha123"));
            String token = a.usuarioServico().entrar(new Login("ana.souza", "senha123")).token();

            assertFalse(a.usuarioServico().alterar(ana.id(), new AlteracaoDeUsuario(null, false), GESTOR).ativo());
            assertTrue(a.sessoes().usuarioDoToken(token).isEmpty());
            assertStatus(403, () -> a.usuarioServico().entrar(new Login("ana.souza", "senha123")));
            // Senha errada continua dando 401: não revela que a conta existe
            assertStatus(401, () -> a.usuarioServico().entrar(new Login("ana.souza", "senha999")));

            assertTrue(a.usuarioServico().alterar(ana.id(), new AlteracaoDeUsuario(null, true), GESTOR).ativo());
            assertTrue(a.usuarioServico().entrar(new Login("ana.souza", "senha123")).usuario().ativo());
        }

        @ParameterizedTest(name = "id \"{0}\" não encontrado (404)")
        @NullSource
        @ValueSource(strings = {"abc", "bbbbbbbbbbbbbbbbbbbbbbbb"})
        void alterarUsuarioInexistente(String id) {
            Ambiente a = ambiente();

            assertStatus(404, () -> a.usuarioServico().alterar(id, new AlteracaoDeUsuario("GESTAO", null), GESTOR));
        }
    }

    // ============================================ 4. PERMISSÕES POR PERFIL (HTTP)

    // Sobe o sistema como ele roda de verdade: API HTTP -> PonteSocket -> Servidor de sockets
    // (AceitadoraDeConexao/SupervisoraDeConexao) -> serviços. Quem confere login e perfil (401/403)
    // é a SupervisoraDeConexao. As respostas 400 abaixo vêm das validações, que acontecem antes de
    // gravar qualquer coisa. Precisa do MongoDB (banco de teste) só para criar os serviços.
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Permissões por perfil (API HTTP + servidor de sockets)")
    class Permissoes {

        private static final String MOV_INCOMPLETA =
                "{\"tipo\":\"Entrada\",\"origem\":\"Manual\",\"material\":\"\",\"quantidade\":1,\"local\":\"Almoxarifado A\"}";

        private HttpServer servidor;
        private HttpClient cliente;
        private Servicos servicos;
        private int porta;

        @BeforeAll
        void subirServidor() throws Exception {
            ambiente(); // abre a conexão com o banco de teste (ou ignora os testes sem MONGO_URI)
            servicos = new Servicos(conexao.banco());

            // Servidor de sockets numa porta livre (a thread não segura o fim dos testes)
            int portaSocket;
            try (ServerSocket livre = new ServerSocket(0)) {
                portaSocket = livre.getLocalPort();
            }
            AceitadoraDeConexao aceitadora = new AceitadoraDeConexao(String.valueOf(portaSocket), new ArrayList<Parceiro>(), servicos);
            aceitadora.setDaemon(true);
            aceitadora.start();

            // API HTTP ligada ao servidor de sockets pela ponte, como na Aplicacao
            PonteSocket ponte = new PonteSocket("127.0.0.1", portaSocket);
            Roteador roteador = new Roteador(new Gson());
            new UsuarioControlador(ponte).registrar(roteador);
            new MaterialControlador(ponte).registrar(roteador);
            new MovimentacaoControlador(ponte).registrar(roteador);

            servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            servidor.createContext("/api", roteador);
            servidor.start();
            porta = servidor.getAddress().getPort();
            cliente = HttpClient.newHttpClient();
        }

        @AfterAll
        void pararServidor() {
            if (servidor != null) servidor.stop(0);
        }

        // A sessão é aberta nas Sessoes do servidor de sockets, que é quem confere o token
        private String tokenDe(Perfil perfil) {
            return servicos.getSessoes().abrir(new Usuario("9", "Fulano de Tal", "fulano." + perfil.name().toLowerCase(), perfil));
        }

        private HttpResponse<String> chamar(String metodo, String caminho, String token, String corpo) throws Exception {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta + caminho))
                    .header("Content-Type", "application/json");
            if (token != null) b.header("Authorization", "Bearer " + token);
            b.method(metodo, corpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(corpo));
            return cliente.send(b.build(), HttpResponse.BodyHandlers.ofString());
        }

        @ParameterizedTest(name = "{0} {1} sem login retorna 401")
        @CsvSource({
                "GET,/api/eu",
                "GET,/api/usuarios",
                "PATCH,/api/usuarios/abc",
                "GET,/api/materiais",
                "POST,/api/materiais",
                "GET,/api/movimentacoes",
                "POST,/api/movimentacoes"
        })
        void semLoginNaoEntra(String metodo, String caminho) throws Exception {
            assertEquals(401, chamar(metodo, caminho, null, "{}").statusCode());
            assertEquals(401, chamar(metodo, caminho, "token-falso", "{}").statusCode());
        }

        @Test
        @DisplayName("Usuário logado consulta os próprios dados")
        void euDevolveUsuario() throws Exception {
            HttpResponse<String> resposta = chamar("GET", "/api/eu", tokenDe(Perfil.GESTAO), null);

            assertEquals(200, resposta.statusCode());
            assertTrue(resposta.body().contains("fulano.gestao"));
        }

        @Test
        @DisplayName("Logout invalida o token na API")
        void logoutInvalidaToken() throws Exception {
            String token = tokenDe(Perfil.ALMOXARIFADO);

            assertEquals(204, chamar("POST", "/api/logout", token, null).statusCode());
            assertEquals(401, chamar("GET", "/api/eu", token, null).statusCode());
        }

        @Test
        @DisplayName("Rota inexistente dá 404 e método errado dá 405")
        void rotaEMetodo() throws Exception {
            String token = tokenDe(Perfil.GESTAO);

            assertEquals(404, chamar("GET", "/api/nada", token, null).statusCode());
            assertEquals(405, chamar("DELETE", "/api/movimentacoes", token, null).statusCode());
        }

        @Test
        @DisplayName("Centro Cirúrgico não cadastra, edita nem inativa material (403)")
        void centroCirurgicoNaoMexeEmMateriais() throws Exception {
            String token = tokenDe(Perfil.CENTRO_CIRURGICO);

            assertEquals(403, chamar("POST", "/api/materiais", token, "{}").statusCode());
            assertEquals(403, chamar("PUT", "/api/materiais/abc", token, "{}").statusCode());
            assertEquals(403, chamar("DELETE", "/api/materiais/abc", token, null).statusCode());
        }

        @ParameterizedTest(name = "{0} pode cadastrar material (passa da permissão e cai na validação 400)")
        @EnumSource(value = Perfil.class, names = {"ALMOXARIFADO", "GESTAO"})
        void almoxarifadoEGestaoCadastramMaterial(Perfil perfil) throws Exception {
            assertEquals(400, chamar("POST", "/api/materiais", tokenDe(perfil), "{}").statusCode());
        }

        @ParameterizedTest(name = "{0} não lista nem altera usuários (403)")
        @EnumSource(value = Perfil.class, names = {"ALMOXARIFADO", "CENTRO_CIRURGICO"})
        void soGestaoMexeEmUsuarios(Perfil perfil) throws Exception {
            String token = tokenDe(perfil);

            assertEquals(403, chamar("GET", "/api/usuarios", token, null).statusCode());
            assertEquals(403, chamar("PATCH", "/api/usuarios/abc", token, "{\"ativo\":false}").statusCode());
        }

        @Test
        @DisplayName("Gestão lista usuários e pode alterar (passa da permissão e cai na validação 400)")
        void gestaoMexeEmUsuarios() throws Exception {
            String token = tokenDe(Perfil.GESTAO);

            assertEquals(200, chamar("GET", "/api/usuarios", token, null).statusCode());
            assertEquals(400, chamar("PATCH", "/api/usuarios/abc", token, "{}").statusCode());
        }

        @Test
        @DisplayName("Gestão não registra movimentação (403)")
        void gestaoNaoMovimenta() throws Exception {
            assertEquals(403, chamar("POST", "/api/movimentacoes", tokenDe(Perfil.GESTAO), MOV_INCOMPLETA).statusCode());
        }

        @ParameterizedTest(name = "{0} pode registrar movimentação (passa da permissão e cai na validação 400)")
        @EnumSource(value = Perfil.class, names = {"ALMOXARIFADO", "CENTRO_CIRURGICO"})
        void almoxarifadoECentroMovimentam(Perfil perfil) throws Exception {
            assertEquals(400, chamar("POST", "/api/movimentacoes", tokenDe(perfil), MOV_INCOMPLETA).statusCode());
        }

        @ParameterizedTest(name = "{0} pode consultar o histórico (400 por dias inválido, não 401/403)")
        @EnumSource(Perfil.class)
        void todosConsultamHistorico(Perfil perfil) throws Exception {
            assertEquals(400, chamar("GET", "/api/movimentacoes?dias=abc", tokenDe(perfil), null).statusCode());
        }
    }

    // ============================================================ 5. HISTÓRICO

    @Nested
    @DisplayName("Histórico de movimentações")
    class Historico {

        // ---- sem banco

        @ParameterizedTest(name = "dias=\"{0}\" é rejeitado")
        @ValueSource(strings = {"0", "-1", "366", "abc", "1.5"})
        void periodoInvalido(String dias) {
            MovimentacaoServico servico = new MovimentacaoServico(null, null);
            assertStatus(400, () -> servico.listar(dias, null));
        }

        // ---- com banco

        @Test
        @DisplayName("Entrada e saída aparecem no histórico, da mais recente para a mais antiga")
        void registraMovimentacoes() throws Exception {
            Ambiente a = ambiente();
            Material gaze = criarMaterial(a, "Gaze estéril", null);

            a.movimentacaoServico().registrar(mov(gaze.nome(), ENTRADA, 10, LOCAL_A), ALMOX);
            Thread.sleep(5);
            a.movimentacaoServico().registrar(new NovaMovimentacao(gaze.nome(), SAIDA, "Código de barras", 4, LOCAL_A), CENTRO);

            List<Movimentacao> historico = a.movimentacaoServico().listar(null, null);

            assertEquals(2, historico.size());
            Movimentacao ultima = historico.get(0);
            assertEquals(SAIDA, ultima.tipo());
            assertEquals(4, ultima.quantidade());
            assertEquals("Gaze estéril", ultima.material());
            assertEquals(gaze.id(), ultima.materialId());
            assertEquals(LOCAL_A, ultima.local());
            assertEquals("Código de barras", ultima.origem());
            assertEquals("Carlos Lima", ultima.usuario());
            // Lido de volta do banco: prova que o id foi gravado, não só devolvido
            assertEquals(CENTRO.id(), ultima.usuarioId());
            assertEquals(ALMOX.id(), historico.get(1).usuarioId());
            assertEquals(ENTRADA, historico.get(1).tipo());
        }

        @Test
        @DisplayName("Movimentação antiga sem usuarioId continua sendo lida (usuarioId = null)")
        void movimentacaoAntigaSemUsuarioId() {
            Ambiente a = ambiente();
            conexao.banco().getCollection("movimentacoes").insertOne(new org.bson.Document("data", new java.util.Date())
                    .append("materialId", "abc").append("material", "Gaze").append("tipo", ENTRADA)
                    .append("quantidade", 1).append("origem", "Manual").append("local", LOCAL_A)
                    .append("usuario", "Ana Souza"));

            List<Movimentacao> historico = a.movimentacaoServico().listar(null, null);

            assertEquals(1, historico.size());
            assertEquals("Ana Souza", historico.get(0).usuario());
            assertEquals(null, historico.get(0).usuarioId());
        }

        @Test
        @DisplayName("Filtro por material mostra só as movimentações dele")
        void filtroPorMaterial() {
            Ambiente a = ambiente();
            Material gaze = criarMaterial(a, "Gaze estéril", null);
            Material seringa = criarMaterial(a, "Seringa 10ml", null);
            a.movimentacaoServico().registrar(mov(gaze.nome(), ENTRADA, 10, LOCAL_A), ALMOX);
            a.movimentacaoServico().registrar(mov(seringa.nome(), ENTRADA, 20, LOCAL_A), ALMOX);
            a.movimentacaoServico().registrar(mov(seringa.nome(), SAIDA, 5, LOCAL_A), ALMOX);

            assertEquals(3, a.movimentacaoServico().listar(null, null).size());
            assertEquals(1, a.movimentacaoServico().listar(null, gaze.id()).size());
            assertEquals(2, a.movimentacaoServico().listar(null, seringa.id()).size());
            assertTrue(a.movimentacaoServico().listar(null, "000000000000000000000000").isEmpty());
        }

        @Test
        @DisplayName("Filtro por período: padrão de 30 dias e valores personalizados")
        void filtroPorPeriodo() {
            Ambiente a = ambiente();
            Material gaze = criarMaterial(a, "Gaze estéril", null);
            Instant agora = Instant.now();
            a.historico().inserir(agora.minus(Duration.ofDays(40)), gaze.id(), gaze.nome(), ENTRADA, 1, "Manual", LOCAL_A, "id-ana", "Ana Souza");
            a.historico().inserir(agora.minus(Duration.ofDays(10)), gaze.id(), gaze.nome(), ENTRADA, 2, "Manual", LOCAL_A, "id-ana", "Ana Souza");
            a.historico().inserir(agora, gaze.id(), gaze.nome(), ENTRADA, 3, "Manual", LOCAL_A, "id-ana", "Ana Souza");

            assertEquals(2, a.movimentacaoServico().listar(null, null).size(), "padrão = últimos 30 dias");
            assertEquals(2, a.movimentacaoServico().listar("", null).size());
            assertEquals(1, a.movimentacaoServico().listar("5", null).size());
            assertEquals(2, a.movimentacaoServico().listar("15", null).size());
            assertEquals(3, a.movimentacaoServico().listar("60", null).size());
            assertEquals(3, a.movimentacaoServico().listar("365", gaze.id()).size());
        }

        @Test
        @DisplayName("Período e material podem ser combinados")
        void periodoEMaterialJuntos() {
            Ambiente a = ambiente();
            Material gaze = criarMaterial(a, "Gaze estéril", null);
            Material seringa = criarMaterial(a, "Seringa 10ml", null);
            Instant agora = Instant.now();
            a.historico().inserir(agora.minus(Duration.ofDays(20)), gaze.id(), gaze.nome(), ENTRADA, 1, "Manual", LOCAL_A, "id-ana", "Ana Souza");
            a.historico().inserir(agora.minus(Duration.ofDays(2)), gaze.id(), gaze.nome(), ENTRADA, 2, "Manual", LOCAL_A, "id-ana", "Ana Souza");
            a.historico().inserir(agora.minus(Duration.ofDays(2)), seringa.id(), seringa.nome(), ENTRADA, 3, "Manual", LOCAL_A, "id-ana", "Ana Souza");

            List<Movimentacao> resultado = a.movimentacaoServico().listar("7", gaze.id());

            assertEquals(1, resultado.size());
            assertEquals(2, resultado.get(0).quantidade());
        }
    }
}
