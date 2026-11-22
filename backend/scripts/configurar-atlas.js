// Autor: Leonardo Fonseca RA: 25024579

// Palavras-chave: mongosh · cria coleções · validadores ($jsonSchema) · índices · moderate/warn
// Execute com mongosh conectado ao Atlas.
// Este script cria as coleções, validadores e índices usados pelo backend atual.
// Em coleções existentes, começa em moderate/warn para não interromper dados legados.
// Para testar sem mexer nos dados reais: mongosh "<uri>" --eval 'var nomeBanco = "outro"' --file scripts/configurar-atlas.js
const banco = db.getSiblingDB(typeof nomeBanco !== "undefined" ? nomeBanco : "estoquehosp");

// Mesmos locais de MovimentacaoServico.LOCAIS
const LOCAIS = ["Almoxarifado A", "Almoxarifado B", "Centro Cirúrgico"];
const saldoDoLocal = { bsonType: ["int", "long"], minimum: 0 };

const esquemas = {
  usuarios: {
    bsonType: "object",
    required: ["nome", "matricula", "matriculaChave", "perfil", "hashSenha"],
    properties: {
      nome: { bsonType: "string", minLength: 3, maxLength: 100 },
      matricula: { bsonType: "string", minLength: 3, maxLength: 30 },
      matriculaChave: { bsonType: "string", minLength: 3, maxLength: 30 },
      perfil: { bsonType: "string", enum: ["ALMOXARIFADO", "CENTRO_CIRURGICO", "GESTAO"] },
      hashSenha: { bsonType: "string", minLength: 1 },
      // Opcional: contas antigas não têm o campo e contam como ativas
      ativo: { bsonType: "bool" }
    }
  },
  materiais: {
    bsonType: "object",
    required: ["nome", "codigoBarras", "lote", "validade", "quantidadeMinima", "saldos", "ativo"],
    properties: {
      nome: { bsonType: "string", minLength: 2, maxLength: 100 },
      codigoBarras: {
        bsonType: ["string", "null"],
        pattern: "^[A-Za-z0-9-]{4,50}$"
      },
      lote: { bsonType: ["string", "null"], maxLength: 30 },
      validade: { bsonType: ["string", "null"], pattern: "^\\d{4}-\\d{2}-\\d{2}$" },
      quantidadeMinima: { bsonType: "int", minimum: 0, maximum: 100000 },
      // Só aceita os 3 locais conhecidos; qualquer outra chave é rejeitada
      saldos: {
        bsonType: "object",
        properties: Object.fromEntries(LOCAIS.map(local => [local, saldoDoLocal])),
        additionalProperties: false
      },
      ativo: { bsonType: "bool" }
    }
  },
  movimentacoes: {
    bsonType: "object",
    required: ["data", "materialId", "material", "tipo", "quantidade", "origem", "local", "usuarioId", "usuario"],
    properties: {
      data: { bsonType: "date" },
      materialId: { bsonType: "string", minLength: 1 },
      material: { bsonType: "string", minLength: 1, maxLength: 100 },
      tipo: { bsonType: "string", enum: ["Entrada", "Saída"] },
      quantidade: { bsonType: "int", minimum: 1, maximum: 100000 },
      origem: { bsonType: "string", enum: ["Código de barras", "Manual"] },
      local: { bsonType: "string", enum: LOCAIS },
      // Id do usuário (rastreabilidade); o nome fica em "usuario" como era na época
      usuarioId: { bsonType: "string", minLength: 1 },
      usuario: { bsonType: "string", minLength: 1, maxLength: 100 }
    }
  }
};

for (const [nome, esquema] of Object.entries(esquemas)) {
  const existe = banco.getCollectionInfos({ name: nome }).length > 0;
  const validator = { $jsonSchema: esquema };

  if (!existe) {
    banco.createCollection(nome, {
      validator,
      validationLevel: "strict",
      validationAction: "error"
    });
    print(`Coleção ${nome} criada com validação estrita.`);
  } else {
    banco.runCommand({
      collMod: nome,
      validator,
      validationLevel: "moderate",
      validationAction: "warn"
    });
    print(`Validador aplicado a ${nome} em modo moderate/warn para preservar compatibilidade.`);
  }
}

banco.usuarios.createIndex({ matriculaChave: 1 }, { unique: true });
banco.materiais.createIndex({ codigoBarras: 1 });
banco.movimentacoes.createIndex({ data: -1 });

print(`Índices criados/verificados. Banco preparado: ${banco.getName()}.`);
print("Após revisar documentos antigos e logs de validação, altere as coleções existentes para strict/error.");
