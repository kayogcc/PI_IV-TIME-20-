// Autores: Bruno Lobo de Jesus RA: 25019830 (modelo de dados e campo ativo), Gabriel Albasini RA: 25019916 (adaptação para o servidor de sockets)

// Palavras-chave: record · Serializable · dados públicos (sem senha) · perfil · ativo
package br.puc.estoquehosp.modelo;

import java.io.Serializable;

// Dados públicos do usuário (a senha nunca sai do repositório). Conta inativa não consegue entrar.
public record Usuario(String id, String nome, String matricula, Perfil perfil, boolean ativo) implements Serializable {

    // Conta ativa: é o caso de quem acabou de ser cadastrado ou está logado
    public Usuario(String id, String nome, String matricula, Perfil perfil) {
        this(id, nome, matricula, perfil, true);
    }
}
