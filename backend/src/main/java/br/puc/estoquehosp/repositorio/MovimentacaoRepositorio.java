package br.puc.estoquehosp.repositorio;

import br.puc.estoquehosp.modelo.Movimentacao;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// Coleção "movimentacoes". Cada documento é um registro imutável do histórico.
public class MovimentacaoRepositorio {

    private final MongoCollection<Document> colecao;

    public MovimentacaoRepositorio(MongoDatabase banco) {
        this.colecao = banco.getCollection("movimentacoes");
        colecao.createIndex(Indexes.descending("data"));
    }

    public Movimentacao inserir(Instant data, String materialId, String material, String tipo, int quantidade,
                                String origem, String local, String usuario) {
        Document doc = new Document("data", Date.from(data))
                .append("materialId", materialId)
                .append("material", material)
                .append("tipo", tipo)
                .append("quantidade", quantidade)
                .append("origem", origem)
                .append("local", local)
                .append("usuario", usuario);
        colecao.insertOne(doc);
        return paraMovimentacao(doc);
    }

    // Mais recentes primeiro. materialId pode ser null (todas).
    public List<Movimentacao> listar(Instant desde, String materialId, int limite) {
        Bson filtro = Filters.gte("data", Date.from(desde));
        if (materialId != null) filtro = Filters.and(filtro, Filters.eq("materialId", materialId));
        List<Movimentacao> lista = new ArrayList<>();
        colecao.find(filtro).sort(Sorts.descending("data")).limit(limite).forEach(d -> lista.add(paraMovimentacao(d)));
        return lista;
    }

    private static Movimentacao paraMovimentacao(Document doc) {
        return new Movimentacao(
                doc.getObjectId("_id").toHexString(),
                doc.getDate("data").toInstant().toString(),
                doc.getString("materialId"),
                doc.getString("material"),
                doc.getString("tipo"),
                doc.getInteger("quantidade"),
                doc.getString("origem"),
                doc.getString("local"),
                doc.getString("usuario")
        );
    }
}
