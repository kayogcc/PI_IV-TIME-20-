// Autores: Gabriel Albasini RA: 25019916, Bruno Lobo de Jesus RA: 25019830 (gestão de usuários: listar, mudar perfil, inativar)

// Palavras-chave: cadastro e login · validação de nome/matrícula/senha · senha com letra e número · 401 · token de sessão · conta inativa
package br.puc.estoquehosp.servico;

import br.puc.estoquehosp.http.ErroApi;
import br.puc.estoquehosp.http.Sessoes;
import br.puc.estoquehosp.modelo.Entradas.AlteracaoDeUsuario;
import br.puc.estoquehosp.modelo.Entradas.Login;
import br.puc.estoquehosp.modelo.Entradas.NovoUsuario;
import br.puc.estoquehosp.modelo.Perfil;
import br.puc.estoquehosp.modelo.Usuario;
import br.puc.estoquehosp.repositorio.UsuarioRepositorio;

import java.io.Serializable;
import java.util.List;
import java.util.regex.Pattern;

// Cadastro, login e gestão de usuários. As regras são as mesmas de criar-conta.js, porque o front-end pode ser contornado.
public class UsuarioServico {

    public record LoginFeito(String token, Usuario usuario) implements Serializable {
        // O toString automático do record mostraria o token; aqui ele fica oculto
        @Override
        public String toString() {
            return "LoginFeito[token=(oculto), usuario=" + usuario + "]";
        }
    }

    private static final Pattern NOME = Pattern.compile("^[\\p{L}' -]+$");
    private static final Pattern MATRICULA = Pattern.compile("^[\\p{L}\\d._-]{3,30}$");
    private static final Pattern TEM_LETRA = Pattern.compile("\\p{L}");
    private static final Pattern TEM_NUMERO = Pattern.compile("\\d");
    private static final int TAMANHO_MAXIMO_NOME = 100;
    private static final int TAMANHO_MINIMO_SENHA = 8;
    private static final int TAMANHO_MAXIMO_SENHA = 72;

    private final UsuarioRepositorio repositorio;
    private final Sessoes sessoes;

    public UsuarioServico(UsuarioRepositorio repositorio, Sessoes sessoes) {
        this.repositorio = repositorio;
        this.sessoes = sessoes;
    }

    public Usuario cadastrar(NovoUsuario dados) {
        String nome = dados.nome() == null ? "" : dados.nome().trim().replaceAll("\\s+", " ");
        String matricula = dados.matricula() == null ? "" : dados.matricula().trim();
        String senha = dados.senha() == null ? "" : dados.senha();

        if (nome.split(" ").length < 2) throw ErroApi.requisicaoInvalida("Informe nome e sobrenome.");
        if (!NOME.matcher(nome).matches()) throw ErroApi.requisicaoInvalida("O nome deve conter apenas letras.");
        if (nome.length() > TAMANHO_MAXIMO_NOME) {
            throw ErroApi.requisicaoInvalida("O nome pode ter no máximo " + TAMANHO_MAXIMO_NOME + " caracteres.");
        }
        if (!MATRICULA.matcher(matricula).matches()) {
            throw ErroApi.requisicaoInvalida("Usuário ou matrícula inválido: use de 3 a 30 letras, números, ponto, hífen ou sublinhado, sem espaços.");
        }
        if (senha.length() < TAMANHO_MINIMO_SENHA || senha.length() > TAMANHO_MAXIMO_SENHA) {
            throw ErroApi.requisicaoInvalida("A senha deve ter de " + TAMANHO_MINIMO_SENHA + " a " + TAMANHO_MAXIMO_SENHA + " caracteres.");
        }
        if (!TEM_LETRA.matcher(senha).find() || !TEM_NUMERO.matcher(senha).find()) {
            throw ErroApi.requisicaoInvalida("A senha precisa ter pelo menos uma letra e um número.");
        }

        return repositorio.inserir(nome, matricula, lerPerfil(dados.perfil()), Senhas.gerarHash(senha))
                .orElseThrow(() -> ErroApi.conflito("Já existe uma conta com esse usuário ou matrícula."));
    }

    public LoginFeito entrar(Login dados) {
        String matricula = dados.matricula() == null ? "" : dados.matricula().trim();
        UsuarioRepositorio.Credencial credencial = repositorio.buscarPorMatricula(matricula)
                .filter(c -> Senhas.confere(dados.senha(), c.hashSenha()))
                .orElseThrow(() -> new ErroApi(401, "Usuário ou senha incorretos."));
        // Só avisa da conta inativa depois de a senha conferir, para não revelar quais contas existem
        if (!credencial.usuario().ativo()) {
            throw new ErroApi(403, "Esta conta está inativa. Procure a Gestão para reativá-la.");
        }
        return new LoginFeito(sessoes.abrir(credencial.usuario()), credencial.usuario());
    }

    public void sair(String token) {
        sessoes.fechar(token);
    }

    public List<Usuario> listar() {
        return repositorio.listar();
    }

    // Muda o perfil e/ou ativa ou inativa a conta. A permissão (só Gestão) é conferida no Servidor.
    // Ninguém mexe na própria conta: assim a Gestão não perde o acesso e sempre sobra pelo menos uma Gestão ativa.
    public Usuario alterar(String id, AlteracaoDeUsuario dados, Usuario quemPediu) {
        if (dados.perfil() == null && dados.ativo() == null) {
            throw ErroApi.requisicaoInvalida("Informe o novo perfil ou se a conta fica ativa.");
        }
        Perfil perfil = dados.perfil() == null ? null : lerPerfil(dados.perfil());
        if (quemPediu.id().equals(id)) {
            throw ErroApi.requisicaoInvalida("Você não pode alterar o perfil nem inativar a sua própria conta.");
        }

        Usuario alterado = repositorio.alterar(id, perfil, dados.ativo())
                .orElseThrow(() -> ErroApi.naoEncontrado("Usuário não encontrado."));
        // A pessoa precisa entrar de novo para valer o perfil novo (ou fica de fora, se foi inativada)
        sessoes.fecharDoUsuario(alterado.id());
        return alterado;
    }

    private static Perfil lerPerfil(String perfil) {
        try {
            return Perfil.valueOf(perfil);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw ErroApi.requisicaoInvalida("Perfil inválido. Use ALMOXARIFADO, CENTRO_CIRURGICO ou GESTAO.");
        }
    }
}
