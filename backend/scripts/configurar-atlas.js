// Execute com mongosh conectado ao Atlas.
// Este script cria as coleções, validadores e índices usados pelo backend atual.
// Em coleções existentes, começa em moderate/warn para não interromper dados legados.
const banco = db.getSiblingDB("estoquehosp");

const esquemas = {
  usuarios: {
    bsonType: "object",
    required: ["nome", "matricula", "matriculaChave", "perfil", "hashSenha"],
    properties: {
      nome: { bsonType: "string", minLength: 3, maxLength: 100 },
      matricula: { bsonType: "string", minLength: 3, maxLength: 30 },
      matriculaChave: { bsonType: "string", minLength: 3, maxLength: 30 },
      perfil: { bsonType: "string", enum: ["ALMOXARIFADO", "CENTRO_CIRURGICO", "GESTAO"] },
      hashSenha: { bsonType: "string", minLength: 1 }
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
      saldos: {
        bsonType: "object",
        additionalProperties: { bsonType: ["int", "long"], minimum: 0 }
      },
      ativo: { bsonType: "bool" }
    }
  },
  movimentacoes: {
    bsonType: "object",
    required: ["data", "materialId", "material", "tipo", "quantidade", "origem", "local", "usuario"],
    properties: {
      data: { bsonType: "date" },
      materialId: { bsonType: "string", minLength: 1 },
      material: { bsonType: "string", minLength: 1, maxLength: 100 },
      tipo: { bsonType: "string", enum: ["Entrada", "Saída"] },
      quantidade: { bsonType: "int", minimum: 1, maximum: 100000 },
      origem: { bsonType: "string", enum: ["Código de barras", "Manual"] },
      local: {
        bsonType: "string",
        enum: ["Almoxarifado A", "Almoxarifado B", "Centro Cirúrgico"]
      },
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

print("Índices criados/verificados. Banco preparado: estoquehosp.");
print("Após revisar documentos antigos e logs de validação, altere as coleções existentes para strict/error.");
