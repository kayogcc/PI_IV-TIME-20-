package br.puc.estoquehosp.modelo;

// Dados públicos do usuário (a senha nunca sai do repositório)
public record Usuario(String id, String nome, String matricula, Perfil perfil) {}
