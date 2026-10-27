package br.puc.estoquehosp;

import br.puc.estoquehosp.banco.ConexaoMongo;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// Conecta no Atlas, grava um documento e lê de volta.
// Usa o banco estoquehosp_teste para não misturar com os dados reais.
class ConexaoTest {

    @Test
    void gravaELeNoAtlas() {
        String uri = System.getenv("MONGO_URI");
        assumeTrue(uri != null && !uri.isBlank(), "MONGO_URI não definida");

        try (ConexaoMongo conexao = new ConexaoMongo(uri, "estoquehosp_teste")) {
            conexao.testar();

            MongoCollection<Document> colecao = conexao.banco().getCollection("teste_conexao");
            colecao.drop();
            colecao.insertOne(new Document("mensagem", "conexão funcionando"));

            Document lido = colecao.find().first();
            assertEquals("conexão funcionando", lido.getString("mensagem"));
        }
    }
}