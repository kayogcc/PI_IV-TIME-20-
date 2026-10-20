
/*
 * Histórico de movimentações - EstoqueHosp
 * Dados de exemplo até a integração com o backend.
 */

const listaHistorico = document.getElementById("l-hist");
const filtros = document.getElementById("fm");

// Dados fictícios para testar a interface.
const movimentacoes = [
  {
    id: 1,
    tipo: "e",
    material: "Luvas cirúrgicas",
    descricao: "Reposição de estoque",
    quantidade: 50,
    unidade: "caixas",
    responsavel: "Carlos Eduardo",
    data: "2026-10-20T08:30:00"
  },
  {
    id: 2,
    tipo: "s",
    material: "Seringas descartáveis",
    descricao: "Utilização no centro cirúrgico",
    quantidade: 20,
    unidade: "unidades",
    responsavel: "Mariana Costa",
    data: "2026-10-19T14:15:00"
  },
  {
    id: 3,
    tipo: "e",
    material: "Máscaras cirúrgicas",
    descricao: "Entrada por reposição",
    quantidade: 100,
    unidade: "unidades",
    responsavel: "Carlos Eduardo",
    data: "2026-10-18T10:00:00"
  },
  {
    id: 4,
    tipo: "s",
    material: "Gazes esterilizadas",
    descricao: "Separação de materiais",
    quantidade: 30,
    unidade: "pacotes",
    responsavel: "Ana Souza",
    data: "2026-10-17T16:45:00"
  },
  {
    id: 5,
    tipo: "e",
    material: "Soro fisiológico",
    descricao: "Recebimento de materiais",
    quantidade: 40,
    unidade: "frascos",
    responsavel: "Mariana Costa",
    data: "2026-10-16T09:20:00"
  }
];

// Formata a data para o padrão brasileiro.
function formatarData(data) {
  return new Date(data).toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  });
}

// Cria um elemento HTML com texto seguro.
function criarElemento(tag, classe, texto) {
  const elemento = document.createElement(tag);

  if (classe) {
    elemento.className = classe;
  }

  if (texto !== undefined) {
    elemento.textContent = texto;
  }

  return elemento;
}

// Monta um item da lista de movimentações.
function criarItem(movimentacao) {
  const item = document.createElement("li");
  item.className = "movimentacao-item";

  const conteudo = criarElemento("div", "movimentacao-conteudo");

  const cabecalho = criarElemento("div", "movimentacao-cabecalho");

  const material = criarElemento(
    "h3",
    "movimentacao-material",
    movimentacao.material
  );

  const tipoTexto =
    movimentacao.tipo === "e" ? "Entrada" : "Saída";

  const tipo = criarElemento(
    "span",
    `movimentacao-tipo ${movimentacao.tipo === "e" ? "entrada" : "saida"}`,
    tipoTexto
  );

  cabecalho.append(material, tipo);

  const descricao = criarElemento(
    "p",
    "movimentacao-descricao",
    movimentacao.descricao
  );

  const detalhes = criarElemento("div", "movimentacao-detalhes");

  detalhes.append(
    criarElemento(
      "span",
      "movimentacao-quantidade",
      `${movimentacao.tipo === "e" ? "+" : "−"}${movimentacao.quantidade} ${movimentacao.unidade}`
    ),
    criarElemento(
      "span",
      "movimentacao-responsavel",
      `Responsável: ${movimentacao.responsavel}`
    ),
    criarElemento(
      "time",
      "movimentacao-data",
      formatarData(movimentacao.data)
    )
  );

  item.append(conteudo);
  conteudo.append(cabecalho, descricao, detalhes);

  return item;
}

// Exibe as movimentações de acordo com o filtro selecionado.
function renderizarHistorico(filtro = "") {
  listaHistorico.replaceChildren();

  const resultados = movimentacoes.filter((movimentacao) => {
    return filtro === "" || movimentacao.tipo === filtro;
  });

  if (resultados.length === 0) {
    const mensagem = criarElemento(
      "li",
      "historico-vazio",
      "Nenhuma movimentação encontrada."
    );

    listaHistorico.appendChild(mensagem);
    return;
  }

  resultados.forEach((movimentacao) => {
    listaHistorico.appendChild(criarItem(movimentacao));
  });
}

// Configura os botões Todas, Entradas e Saídas.
function configurarFiltros() {
  const botoes = filtros.querySelectorAll("button[data-f]");

  botoes.forEach((botao) => {
    botao.addEventListener("click", () => {
      botoes.forEach((outroBotao) => {
        outroBotao.classList.remove("on");
      });

      botao.classList.add("on");

      renderizarHistorico(botao.dataset.f);
    });
  });
}

// Inicializa a página.
function inicializarHistorico() {
  if (!listaHistorico || !filtros) {
    console.error(
      "Não foi possível inicializar o histórico: elementos HTML não encontrados."
    );
    return;
  }

  configurarFiltros();
  renderizarHistorico();
}

inicializarHistorico();
