// Autores: Bruno Lobo(modelo de dados), Gabriel Figueira Albasini (adaptação para o servidor de sockets)

// Palavras-chave: record · Serializable · dados públicos (sem senha) · perfil
package br.puc.estoquehosp.modelo;

import java.io.Serializable;

// Dados públicos do usuário (a senha nunca sai do repositório)
public record Usuario(String id, String nome, String matricula, Perfil perfil) implements Serializable {}
