import { montarLayout } from '../componentes/estrutura.js'
import { esc } from '../utilitarios/textos.js'

// Dados fictícios simulando o banco de dados
const produtosFicticios = [
    { id: 1, nome: "Cateter venoso 20G", quantidade: 4, minimo: 30 },
    { id: 2, nome: "Seringa 10 ml", quantidade: 85, minimo: 150 },
    { id: 3, nome: "Máscara cirúrgica", quantidade: 420, minimo: 200 }, // Este não deve aparecer (estoque normal)
    { id: 4, nome: "Gaze estéril", quantidade: 60, minimo: 120 }
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
            <td>${esc(produto.nome)}</td>
            <td>${esc(produto.quantidade)}</td>
            <td>${esc(produto.minimo)}</td>
            <td><span class="selo-alerta">Crítico</span></td>
        `;
        tbody.appendChild(tr);
    });
}

// Módulos rodam depois do HTML carregado; sem login, montarLayout redireciona
if (montarLayout()) carregarEstoqueBaixo();