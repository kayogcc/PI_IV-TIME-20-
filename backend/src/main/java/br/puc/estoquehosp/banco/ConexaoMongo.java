package br.puc.estoquehosp.banco;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.concurrent.TimeUnit;

// Abre a conexão com o MongoDB. A URI vem da variável de ambiente MONGO_URI.
public class ConexaoMongo implements AutoCloseable {

    private final MongoClient cliente;
    private final MongoDatabase banco;

    public ConexaoMongo(String uri, String nomeBanco) {
        if (uri == null || uri.isBlank()) {
            throw new IllegalStateException("Defina a variável de ambiente MONGO_URI antes de iniciar.");
        }
        MongoClientSettings config = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .applyToClusterSettings(c -> c.serverSelectionTimeout(10, TimeUnit.SECONDS))
                .build();
        this.cliente = MongoClients.create(config);
        this.banco = cliente.getDatabase(nomeBanco);
    }

    public static ConexaoMongo doAmbiente() {
        return new ConexaoMongo(System.getenv("MONGO_URI"), "estoquehosp");
    }

    public MongoDatabase banco() {
        return banco;
    }

    // Lança exceção se o servidor não responder
    public void testar() {
        banco.runCommand(new Document("ping", 1));
    }

    @Override
    public void close() {
        cliente.close();
    }
}