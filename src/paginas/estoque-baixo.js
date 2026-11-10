import { montarLayout } from '../componentes/estrutura.js'
import { api } from '../servicos/api.js'
import { esc } from '../utilitarios/textos.js'

async function carregarEstoqueBaixo() {
    const tbody = document.getElementById("tabela-estoque-baixo");
    const mensagem = (texto) => `<tr><td colspan="4" style="text-align: center;">${esc(texto)}</td></tr>`;

    let materiais;
    try {
        materiais = await api('GET', '/materiais');
    } catch (erro) {
        tbody.innerHTML = mensagem(erro.message);
        return;
    }

    // Mesma regra da API (AlertaServico.java): quantidade menor ou igual ao mínimo
    const estoqueBaixo = materiais.filter(p => p.quantidadeMinima > 0 && p.quantidade <= p.quantidadeMinima);

    if (estoqueBaixo.length === 0) {
        tbody.innerHTML = mensagem("Nenhum produto com estoque baixo.");
        return;
    }

    tbody.innerHTML = ""; // Limpa a tabela antes de preencher

    // Insere as linhas dinamicamente no HTML
    estoqueBaixo.forEach(produto => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${esc(produto.nome)}</td>
            <td>${esc(produto.quantidade)}</td>
            <td>${esc(produto.quantidadeMinima)}</td>
            <td><span class="selo-alerta">Crítico</span></td>
        `;
        tbody.appendChild(tr);
    });
}

// Módulos rodam depois do HTML carregado; sem login, montarLayout redireciona
if (montarLayout()) carregarEstoqueBaixo();
