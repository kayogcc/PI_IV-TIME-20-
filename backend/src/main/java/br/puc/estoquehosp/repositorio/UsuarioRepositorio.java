// Autores: Gabriel Albasini RA: 25019916, Bruno Lobo de Jesus RA: 25019830 (listagem e alteração pela Gestão)
// Palavras-chave: coleção "usuarios" · índice único na matrícula · matriculaChave (minúsculas) · DUPLICATE_KEY · ativo
package br.puc.estoquehosp.repositorio;

import br.puc.estoquehosp.modelo.Perfil;
import br.puc.estoquehosp.modelo.Usuario;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationStrength;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

// Coleção "usuarios". matriculaChave é a matrícula em minúsculas, para "Ana.Souza" e "ana.souza" serem a mesma conta.
// Contas antigas, sem o campo "ativo", contam como ativas.
public class UsuarioRepositorio {

    public record Credencial(Usuario usuario, String hashSenha) {
        // O hash da senha nunca aparece no terminal
        @Override
        public String toString() {
            return "Credencial[usuario=" + usuario + ", hashSenha=(oculto)]";
        }
    }

    // Ordena por nome sem diferenciar maiúsculas e acentos
    private static final Collation SEM_ACENTO = Collation.builder().locale("pt").collationStrength(CollationStrength.PRIMARY).build();

    private final MongoCollection<Document> colecao;

    public UsuarioRepositorio(MongoDatabase banco) {
        this.colecao = banco.getCollection("usuarios");
        colecao.createIndex(Indexes.ascending("matriculaChave"), new IndexOptions().unique(true));
    }

    // Vazio se a matrícula já estiver em uso
    public Optional<Usuario> inserir(String nome, String matricula, Perfil perfil, String hashSenha) {
        Document doc = new Document("nome", nome)
                .append("matricula", matricula)
                .append("matriculaChave", chave(matricula))
                .append("perfil", perfil.name())
                .append("hashSenha", hashSenha)
                .append("ativo", true);
        try {
            colecao.insertOne(doc);
        } catch (MongoWriteException e) {
            if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) return Optional.empty();
            throw e;
        }
        return Optional.of(paraUsuario(doc));
    }

    public Optional<Credencial> buscarPorMatricula(String matricula) {
        Document doc = colecao.find(Filters.eq("matriculaChave", chave(matricula))).first();
        if (doc == null) return Optional.empty();
        return Optional.of(new Credencial(paraUsuario(doc), doc.getString("hashSenha")));
    }

    // Todos os usuários, ativos e inativos, em ordem de nome
    public List<Usuario> listar() {
        List<Usuario> usuarios = new ArrayList<>();
        colecao.find().collation(SEM_ACENTO).sort(Sorts.ascending("nome")).forEach(doc -> usuarios.add(paraUsuario(doc)));
        return usuarios;
    }

    // Muda só o que não for null. Vazio se o id não existir.
    public Optional<Usuario> alterar(String id, Perfil perfil, Boolean ativo) {
        Optional<ObjectId> objectId = idValido(id);
        if (objectId.isEmpty()) return Optional.empty();

        List<Bson> mudancas = new ArrayList<>();
        if (perfil != null) mudancas.add(Updates.set("perfil", perfil.name()));
        if (ativo != null) mudancas.add(Updates.set("ativo", ativo));

        Bson filtro = Filters.eq("_id", objectId.get());
        Document doc = mudancas.isEmpty()
                ? colecao.find(filtro).first()
                : colecao.findOneAndUpdate(filtro, Updates.combine(mudancas),
                        new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
        return Optional.ofNullable(doc).map(UsuarioRepositorio::paraUsuario);
    }

    private static String chave(String matricula) {
        return matricula.toLowerCase(Locale.ROOT);
    }

    private static Usuario paraUsuario(Document doc) {
        return new Usuario(
                doc.getObjectId("_id").toHexString(),
                doc.getString("nome"),
                doc.getString("matricula"),
                Perfil.valueOf(doc.getString("perfil")),
                doc.getBoolean("ativo", true)
        );
    }

    static Optional<ObjectId> idValido(String id) {
        return id != null && ObjectId.isValid(id) ? Optional.of(new ObjectId(id)) : Optional.empty();
    }
}
