// Autores: Bruno Lobo de Jesus RA: 25019830 (acesso aos materiais no banco), Leonardo Fonseca RA: 25024579 (busca sem acento)

// Palavras-chave: coleção "materiais" · busca sem acento (Collation + Normalizer) · saldo atômico com $inc · não deixa saldo negativo
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

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

// Coleção "materiais". O saldo fica em "saldos": { "Almoxarifado A": 120, "Centro Cirúrgico": 30 }.
public class MaterialRepositorio {

    // Compara ignorando maiúsculas e acentos ("gaze esteril" encontra "Gaze estéril")
    private static final Collation SEM_ACENTO = Collation.builder().locale("pt").collationStrength(CollationStrength.PRIMARY).build();

    private final MongoCollection<Document> colecao;

    public MaterialRepositorio(MongoDatabase banco) {
        this.colecao = banco.getCollection("materiais");
        colecao.createIndex(Indexes.ascending("codigoBarras"));
    }

    // Busca por parte do nome ou do código de barras, sem diferenciar maiúsculas nem acentos
    // ("gaze esteril" encontra "Gaze Estéril"). O filtro é feito aqui no Java porque o regex
    // do MongoDB não ignora acentos.
    public List<Material> listarAtivos(String busca) {
        String termo = busca == null ? "" : normalizar(busca);
        List<Material> materiais = new ArrayList<>();
        colecao.find(Filters.eq("ativo", true)).collation(SEM_ACENTO).sort(Sorts.ascending("nome"))
                .forEach(d -> {
                    Material m = paraMaterial(d);
                    if (termo.isEmpty() || corresponde(m, termo)) materiais.add(m);
                });
        return materiais;
    }

    // true se o termo (já normalizado) aparece no nome ou no código de barras do material
    static boolean corresponde(Material m, String termoNormalizado) {
        return normalizar(m.nome()).contains(termoNormalizado)
                || normalizar(m.codigoBarras()).contains(termoNormalizado);
    }

    // "  Gaze  Estéril " -> "gaze esteril"
    public static String normalizar(String texto) {
        if (texto == null) return "";
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return semAcento.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public Optional<Material> buscarPorId(String id) {
        return UsuarioRepositorio.idValido(id).map(oid -> colecao.find(Filters.eq("_id", oid)).first()).map(MaterialRepositorio::paraMaterial);
    }

    // Procura pelo código de barras e, se não achar, pelo nome exato (sem diferenciar maiúsculas/acentos).
    // Se ainda não achar, aceita parte do nome desde que só um material corresponda (ex.: "luva" -> "Luva de procedimento").
    public Optional<Material> buscarAtivoPorCodigoOuNome(String texto) {
        String limpo = texto == null ? "" : texto.trim();
        Document doc = colecao.find(Filters.and(Filters.eq("ativo", true), Filters.eq("codigoBarras", limpo))).first();
        if (doc == null) {
            doc = colecao.find(Filters.and(Filters.eq("ativo", true), Filters.eq("nome", limpo))).collation(SEM_ACENTO).first();
        }
        if (doc != null) return Optional.of(paraMaterial(doc));

        String termo = normalizar(limpo);
        List<Material> parecidos = listarAtivos(limpo);
        Optional<Material> exato = parecidos.stream()
                .filter(m -> normalizar(m.nome()).equals(termo) || normalizar(m.codigoBarras()).equals(termo))
                .findFirst();
        if (exato.isPresent()) return exato;
        return parecidos.size() == 1 ? Optional.of(parecidos.get(0)) : Optional.empty();
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
