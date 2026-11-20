// Gabriel Figueira Albasini
// Palavras-chave: record · Serializable · histórico · Entrada/Saída · origem (código de barras | manual) · data ISO-8601
package br.puc.estoquehosp.modelo;

import java.io.Serializable;

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
) implements Serializable {}
