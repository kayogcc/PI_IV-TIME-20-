package br.puc.estoquehosp.repositorio;

import br.puc.estoquehosp.modelo.Entradas.DadosMaterial;
import br.puc.estoquehosp.modelo.Material;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationStrength;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

// Coleção "materiais". O saldo fica em "saldos": { "Almoxarifado A": 120, "Centro Cirúrgico": 30 }.
public class MaterialRepositorio {

    // Compara ignorando maiúsculas e acentos ("gaze esteril" encontra "Gaze estéril")
    private static final Collation SEM_ACENTO = Collation.builder().locale("pt").collationStrength(CollationStrength.PRIMARY).build();

    private final MongoCollection<Document> colecao;

    public MaterialRepositorio(MongoDatabase banco) {
        this.colecao = banco.getCollection("materiais");
        colecao.createIndex(Indexes.ascending("codigoBarras"));
    }

    public List<Material> listarAtivos(String busca) {
        Bson filtro = Filters.eq("ativo", true);
        if (busca != null && !busca.isBlank()) {
            Pattern contem = Pattern.compile(Pattern.quote(busca.trim()), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            filtro = Filters.and(filtro, Filters.or(Filters.regex("nome", contem), Filters.eq("codigoBarras", busca.trim())));
        }
        List<Material> materiais = new ArrayList<>();
        colecao.find(filtro).collation(SEM_ACENTO).sort(Sorts.ascending("nome")).forEach(d -> materiais.add(paraMaterial(d)));
        return materiais;
    }

    public Optional<Material> buscarPorId(String id) {
        return UsuarioRepositorio.idValido(id).map(oid -> colecao.find(Filters.eq("_id", oid)).first()).map(MaterialRepositorio::paraMaterial);
    }

    // Procura primeiro pelo código de barras e, se não achar, pelo nome exato (sem diferenciar maiúsculas/acentos)
    public Optional<Material> buscarAtivoPorCodigoOuNome(String texto) {
        Document doc = colecao.find(Filters.and(Filters.eq("ativo", true), Filters.eq("codigoBarras", texto))).first();
        if (doc == null) {
            doc = colecao.find(Filters.and(Filters.eq("ativo", true), Filters.eq("nome", texto))).collation(SEM_ACENTO).first();
        }
        return Optional.ofNullable(doc).map(MaterialRepositorio::paraMaterial);
    }

    public boolean existeAtivoComCodigo(String codigoBarras, String ignorarId) {
        Bson filtro = Filters.and(Filters.eq("ativo", true), Filters.eq("codigoBarras", codigoBarras));
        if (ignorarId != null) filtro = Filters.and(filtro, Filters.ne("_id", new ObjectId(ignorarId)));
        return colecao.countDocuments(filtro) > 0;
    }

    public boolean existeAtivoComNome(String nome, String ignorarId) {
        Bson filtro = Filters.and(Filters.eq("ativo", true), Filters.eq("nome", nome));
        if (ignorarId != null) filtro = Filters.and(filtro, Filters.ne("_id", new ObjectId(ignorarId)));
        return colecao.find(filtro).collation(SEM_ACENTO).first() != null;
    }

    public Material inserir(DadosMaterial dados) {
        Document doc = new Document()
                .append("nome", dados.nome())
                .append("codigoBarras", dados.codigoBarras())
                .append("lote", dados.lote())
                .append("validade", dados.validade())
                .append("quantidadeMinima", dados.quantidadeMinima())
                .append("saldos", new Document())
                .append("ativo", true);
        colecao.insertOne(doc);
        return paraMaterial(doc);
    }

    public Optional<Material> atualizar(String id, DadosMaterial dados) {
        ObjectId oid = new ObjectId(id);
        colecao.updateOne(Filters.eq("_id", oid), Updates.combine(
                Updates.set("nome", dados.nome()),
                Updates.set("codigoBarras", dados.codigoBarras()),
                Updates.set("lote", dados.lote()),
                Updates.set("validade", dados.validade()),
                Updates.set("quantidadeMinima", dados.quantidadeMinima())
        ));
        return buscarPorId(id);
    }

    public void inativar(String id) {
        colecao.updateOne(Filters.eq("_id", new ObjectId(id)), Updates.set("ativo", false));
    }

    // Soma "delta" ao saldo do local. Para saída (delta negativo) só atualiza se houver saldo suficiente,
    // e a conferência acontece no próprio MongoDB, então duas saídas simultâneas não deixam o saldo negativo.
    public boolean ajustarSaldo(String id, String local, int delta) {
        String campo = "saldos." + local;
        Bson filtro = Filters.and(Filters.eq("_id", new ObjectId(id)), Filters.eq("ativo", true));
        if (delta < 0) filtro = Filters.and(filtro, Filters.gte(campo, -delta));
        return colecao.updateOne(filtro, Updates.inc(campo, delta)).getModifiedCount() == 1;
    }

    private static Material paraMaterial(Document doc) {
        Map<String, Integer> saldos = new LinkedHashMap<>();
        Document docSaldos = doc.get("saldos", Document.class);
        if (docSaldos != null) docSaldos.forEach((local, qtd) -> saldos.put(local, ((Number) qtd).intValue()));
        int total = saldos.values().stream().mapToInt(Integer::intValue).sum();

        return new Material(
                doc.getObjectId("_id").toHexString(),
                doc.getString("nome"),
                doc.getString("codigoBarras"),
                doc.getString("lote"),
                doc.getString("validade"),
                doc.getInteger("quantidadeMinima", 0),
                saldos,
                total,
                doc.getBoolean("ativo", true)
        );
    }
}
