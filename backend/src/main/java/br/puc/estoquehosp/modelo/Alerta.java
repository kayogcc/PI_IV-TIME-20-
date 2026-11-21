// Autores: Bruno Lobo de Jesus RA: 25019830 (modelo de dados), Gabriel Albasini RA: 25019916 (adaptação para o servidor de sockets)

// Palavras-chave: record · Serializable · tipo (estoque | validade) · prioridade (Alta | Média | Baixa)
package br.puc.estoquehosp.modelo;

import java.io.Serializable;

// tipoAlerta: estoque | validade — prioridade: Alta | Média | Baixa
public record Alerta(String tipoAlerta, String prioridade, String materialId, String titulo, String detalhe) implements Serializable {}
