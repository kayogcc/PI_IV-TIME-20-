package br.puc.estoquehosp.modelo;

// data em ISO-8601 (UTC). tipo: Entrada | Saída — origem: Código de barras | Manual
public record Movimentacao(
        String id,
        String data,
        String materialId,
        String material,
        String tipo,
        int quantidade,
        String origem,
        String local,
        String usuario
) {}
