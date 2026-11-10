package br.puc.estoquehosp.modelo;

import java.util.Map;

// validade no formato yyyy-MM-dd. saldos: quantidade por local; quantidade: soma de todos os locais.
public record Material(
        String id,
        String nome,
        String codigoBarras,
        String lote,
        String validade,
        int quantidadeMinima,
        Map<String, Integer> saldos,
        int quantidade,
        boolean ativo
) {}
