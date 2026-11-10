package br.puc.estoquehosp.repositorio;

import br.puc.estoquehosp.modelo.Perfil;
import br.puc.estoquehosp.modelo.Usuario;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Locale;
import java.util.Optional;

// Coleção "usuarios". matriculaChave é a matrícula em minúsculas, para "Ana.Souza" e "ana.souza" serem a mesma conta.
public class UsuarioRepositorio {

    public record Credencial(Usuario usuario, String hashSenha) {}

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
                .append("hashSenha", hashSenha);
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

    private static String chave(String matricula) {
        return matricula.toLowerCase(Locale.ROOT);
    }

    private static Usuario paraUsuario(Document doc) {
        return new Usuario(
                doc.getObjectId("_id").toHexString(),
                doc.getString("nome"),
                doc.getString("matricula"),
                Perfil.valueOf(doc.getString("perfil"))
        );
    }

    static Optional<ObjectId> idValido(String id) {
        return id != null && ObjectId.isValid(id) ? Optional.of(new ObjectId(id)) : Optional.empty();
    }
}
