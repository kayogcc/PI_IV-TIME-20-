// Autores: Bruno Lobo de Jesus RA: 25019830 (modelo de dados), Gabriel Albasini RA: 25019916 (adaptação para o servidor de sockets), Leonardo Fonseca RA: 25024579 (campo usuarioId)

// Palavras-chave: record · Serializable · histórico · Entrada/Saída · origem (código de barras | manual) · data ISO-8601 · usuarioId (rastreabilidade)
package br.puc.estoquehosp.modelo;

import java.io.Serializable;

// data em ISO-8601 (UTC). tipo: Entrada | Saída — origem: Código de barras | Manual
// usuarioId identifica quem fez a movimentação mesmo que o nome mude; usuario guarda o nome da época
public record Movimentacao(
        String id,
        String data,
        String materialId,
        String material,
        String tipo,
        int quantidade,
        String origem,
        String local,
        String usuarioId,
        String usuario
) implements Serializable {}
