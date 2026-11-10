package br.puc.estoquehosp.modelo;

// tipoAlerta: estoque | validade — prioridade: Alta | Média | Baixa
public record Alerta(String tipoAlerta, String prioridade, String materialId, String titulo, String detalhe) {}
