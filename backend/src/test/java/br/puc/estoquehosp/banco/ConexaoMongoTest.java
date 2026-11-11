package br.puc.estoquehosp.banco;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConexaoMongoTest {

    // CT-01: Verifica se URI nula é rejeitada
    @Test
    void deveRejeitarUriNula() {
        IllegalStateException erro = assertThrows(
                IllegalStateException.class,
                () -> new ConexaoMongo(null, "estoquehosp")
        );

        assertTrue(erro.getMessage().contains("MONGO_URI"));
    }

    // CT-02: Verifica se URI vazia é rejeitada
    @Test
    void deveRejeitarUriVazia() {
        assertThrows(
                IllegalStateException.class,
                () -> new ConexaoMongo("", "estoquehosp")
        );
    }

    // CT-03: Verifica se URI contendo apenas espaços é rejeitada
    @Test
    void deveRejeitarUriComEspacos() {
        assertThrows(
                IllegalStateException.class,
                () -> new ConexaoMongo("   ", "estoquehosp")
        );
    }

    // CT-04: Verifica se uma URI malformada é rejeitada
    @Test
    void deveRejeitarUriMalformada() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ConexaoMongo("isso-nao-e-uma-uri", "estoquehosp")
        );
    }

    // CT-05: Verifica se uma URI válida permite criar a conexão
    @Test
    void deveAceitarUriValida() {
        try (ConexaoMongo conexao = new ConexaoMongo(
                "mongodb://localhost:27017",
                "estoquehosp_teste"
        )) {
            assertNotNull(conexao);
            assertNotNull(conexao.banco());
        }
    }

    // CT-06: Verifica se a conexão identifica servidor indisponível
    @Test
    void deveIdentificarServidorIndisponivel() {
        try (ConexaoMongo conexao = new ConexaoMongo(
                "mongodb://localhost:27099/?serverSelectionTimeoutMS=1000",
                "estoquehosp_teste"
        )) {
            assertThrows(
                    Exception.class,
                    conexao::testar
            );
        }
    }
}