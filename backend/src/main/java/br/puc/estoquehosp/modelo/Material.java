// Autores: Bruno Lobo de Jesus RA: 25019830 (modelo de dados), Gabriel Albasini RA: 25019916 (adaptação para o servidor de sockets)

// Palavras-chave: record · Serializable · saldos por local · quantidade total · ativo (inativação lógica)
package br.puc.estoquehosp.modelo;

import java.io.Serializable;
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
) implements Serializable {}
