// Autor: Leonardo Fonseca RA: 25024579

// Palavras-chave: mongosh · teste dos validadores · banco descartável · strict/error · não toca no "estoquehosp"
// Execute na pasta backend:  mongosh "<uri>" --quiet --file scripts/testar-atlas.js
// Cria o banco "estoquehosp_validacao", aplica o configurar-atlas.js (coleções novas = strict/error),
// tenta gravar documentos válidos e inválidos no mesmo formato que o Java grava e apaga o banco no final.
globalThis.nomeBanco = "estoquehosp_validacao";
const bdTeste = db.getSiblingDB(nomeBanco);
bdTeste.dropDatabase();

const pastaScripts = typeof __dirname !== "undefined" ? __dirname : "scripts";
load(pastaScripts + "/configurar-atlas.js");

let falhas = 0;
function verificar(descricao, deveAceitar, acao) {
  let aceitou = true;
  let motivo = "";
  try {
    const r = acao();
    // updateOne que não casa nenhum documento não é erro, mas também não gravou
    if (r && r.matchedCount === 0) { aceitou = false; motivo = "nenhum documento alterado"; }
  } catch (e) {
    aceitou = false;
    motivo = e.code === 121 ? "rejeitado pelo validador" : e.message;
  }
  const ok = aceitou === deveAceitar;
  if (!ok) falhas++;
  print(`${ok ? "OK   " : "FALHA"} ${descricao} -> ${aceitou ? "aceito" : "recusado (" + motivo + ")"}`);
}

// ---- materiais (mesmo formato do MaterialRepositorio.inserir)
const material = () => ({
  nome: "Gaze estéril", codigoBarras: "GZ-0001", lote: "L1", validade: "2027-01-31",
  quantidadeMinima: NumberInt(10), saldos: {}, ativo: true
});

verificar("material novo com saldos vazio", true, () => bdTeste.materiais.insertOne(material()));
verificar("saldos com os 3 locais", true, () => bdTeste.materiais.insertOne({
  ...material(), saldos: { "Almoxarifado A": NumberInt(5), "Almoxarifado B": NumberInt(0), "Centro Cirúrgico": NumberLong("3") }
}));
verificar("saldos com local desconhecido", false, () => bdTeste.materiais.insertOne({
  ...material(), saldos: { "Almoxarifado C": NumberInt(5) }
}));
verificar("saldos com local escrito errado (sem acento)", false, () => bdTeste.materiais.insertOne({
  ...material(), saldos: { "Centro Cirurgico": NumberInt(5) }
}));
verificar("saldo negativo", false, () => bdTeste.materiais.insertOne({
  ...material(), saldos: { "Almoxarifado A": NumberInt(-1) }
}));

// ---- ajuste de saldo (mesmo $inc do MaterialRepositorio.ajustarSaldo)
const id = bdTeste.materiais.findOne({ saldos: {} })._id;
verificar("$inc em local válido", true, () => bdTeste.materiais.updateOne({ _id: id }, { $inc: { "saldos.Almoxarifado A": NumberInt(20) } }));
verificar("$inc no Centro Cirúrgico", true, () => bdTeste.materiais.updateOne({ _id: id }, { $inc: { "saldos.Centro Cirúrgico": NumberInt(4) } }));
verificar("$inc em local inexistente", false, () => bdTeste.materiais.updateOne({ _id: id }, { $inc: { "saldos.Depósito": NumberInt(1) } }));

// ---- movimentacoes (mesmo formato do MovimentacaoRepositorio.inserir)
const mov = () => ({
  data: new Date(), materialId: id.toHexString(), material: "Gaze estéril", tipo: "Entrada",
  quantidade: NumberInt(20), origem: "Manual", local: "Almoxarifado A",
  usuarioId: "6650f0c2a1b2c3d4e5f60718", usuario: "Ana Souza"
});

verificar("movimentação com usuarioId e usuario", true, () => bdTeste.movimentacoes.insertOne(mov()));
verificar("movimentação sem usuarioId", false, () => {
  const m = mov(); delete m.usuarioId; return bdTeste.movimentacoes.insertOne(m);
});
verificar("movimentação com usuarioId vazio", false, () => bdTeste.movimentacoes.insertOne({ ...mov(), usuarioId: "" }));
verificar("movimentação com usuarioId null", false, () => bdTeste.movimentacoes.insertOne({ ...mov(), usuarioId: null }));
verificar("movimentação em local desconhecido", false, () => bdTeste.movimentacoes.insertOne({ ...mov(), local: "Almoxarifado C" }));

bdTeste.dropDatabase();
print("");
print(falhas === 0 ? "Todos os testes passaram. Banco de teste apagado." : `${falhas} teste(s) falharam. Banco de teste apagado.`);
if (falhas > 0) quit(1);
