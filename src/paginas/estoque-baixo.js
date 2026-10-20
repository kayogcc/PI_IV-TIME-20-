// Dados fictícios simulando o banco de dados
const produtosFicticios = [
    { id: 1, nome: "Arroz 5kg", quantidade: 2, minimo: 5 },
    { id: 2, nome: "Feijão 1kg", quantidade: 1, minimo: 4 },
    { id: 3, nome: "Óleo de Soja", quantidade: 8, minimo: 3 }, // Este não deve aparecer (estoque normal)
    { id: 4, nome: "Açúcar 1kg", quantidade: 3, minimo: 10 }
];

function carregarEstoqueBaixo() {
    const tbody = document.getElementById("tabela-estoque-baixo");
    tbody.innerHTML = ""; // Limpa a tabela antes de preencher

    // Filtra apenas os produtos onde a quantidade é menor ou igual ao mínimo
    const estoqueBaixo = produtosFicticios.filter(p => p.quantidade <= p.minimo);

    if (estoqueBaixo.length === 0) {
        tbody.innerHTML = `<tr><td colspan="4" style="text-align: center;">Nenhum produto com estoque baixo.</td></tr>`;
        return;
    }

    // Insere as linhas dinamicamente no HTML
    estoqueBaixo.forEach(produto => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${produto.nome}</td>
            <td>${produto.quantidade}</td>
            <td>${produto.minimo}</td>
            <td><span class="selo-alerta">Crítico</span></td>
        `;
        tbody.appendChild(tr);
    });
}

// Executa a função assim que o DOM estiver carregado
document.addEventListener("DOMContentLoaded", carregarEstoqueBaixo);