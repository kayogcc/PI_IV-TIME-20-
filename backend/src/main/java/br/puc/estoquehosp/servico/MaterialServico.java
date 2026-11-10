package br.puc.estoquehosp.servico;

import br.puc.estoquehosp.http.ErroApi;
import br.puc.estoquehosp.modelo.Entradas.DadosMaterial;
import br.puc.estoquehosp.modelo.Material;
import br.puc.estoquehosp.repositorio.MaterialRepositorio;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;

// Cadastro, edição, busca e inativação de materiais. O saldo só muda por movimentação.
public class MaterialServico {

    private static final Pattern CODIGO_BARRAS = Pattern.compile("^[A-Za-z0-9-]{4,50}$");
    private static final int TAMANHO_MAXIMO_NOME = 100;
    private static final int TAMANHO_MAXIMO_LOTE = 30;
    private static final int QUANTIDADE_MAXIMA = 100_000;

    private final MaterialRepositorio repositorio;

    public MaterialServico(MaterialRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    public List<Material> listar(String busca) {
        return repositorio.listarAtivos(busca);
    }

    public Material buscar(String id) {
        return repositorio.buscarPorId(id).orElseThrow(() -> ErroApi.naoEncontrado("Material não encontrado."));
    }

    public Material criar(DadosMaterial dados) {
        DadosMaterial limpo = validar(dados, null);
        return repositorio.inserir(limpo);
    }

    public Material atualizar(String id, DadosMaterial dados) {
        if (!buscar(id).ativo()) throw ErroApi.naoEncontrado("Material não encontrado.");
        DadosMaterial limpo = validar(dados, id);
        return repositorio.atualizar(id, limpo).orElseThrow();
    }

    public void inativar(String id) {
        buscar(id);
        repositorio.inativar(id);
    }

    // Confere os campos e devolve uma cópia com espaços removidos e opcionais vazios como null
    private DadosMaterial validar(DadosMaterial dados, String idAtual) {
        String nome = vazioParaNull(dados.nome());
        String codigo = vazioParaNull(dados.codigoBarras());
        String lote = vazioParaNull(dados.lote());
        String validade = vazioParaNull(dados.validade());
        Integer minimo = dados.quantidadeMinima();

        if (nome == null || nome.length() < 2) throw ErroApi.requisicaoInvalida("Informe o nome do material.");
        nome = nome.replaceAll("\\s+", " ");
        if (nome.length() > TAMANHO_MAXIMO_NOME) {
            throw ErroApi.requisicaoInvalida("O nome pode ter no máximo " + TAMANHO_MAXIMO_NOME + " caracteres.");
        }
        if (codigo != null && !CODIGO_BARRAS.matcher(codigo).matches()) {
            throw ErroApi.requisicaoInvalida("Código de barras inválido: use de 4 a 50 letras, números ou hífen.");
        }
        if (lote != null && lote.length() > TAMANHO_MAXIMO_LOTE) {
            throw ErroApi.requisicaoInvalida("O lote pode ter no máximo " + TAMANHO_MAXIMO_LOTE + " caracteres.");
        }
        if (validade != null) {
            try {
                LocalDate.parse(validade);
            } catch (DateTimeParseException e) {
                throw ErroApi.requisicaoInvalida("Validade inválida. Use o formato AAAA-MM-DD.");
            }
        }
        if (minimo == null || minimo < 0 || minimo > QUANTIDADE_MAXIMA) {
            throw ErroApi.requisicaoInvalida("Informe a quantidade mínima (de 0 a " + QUANTIDADE_MAXIMA + ").");
        }
        if (codigo != null && repositorio.existeAtivoComCodigo(codigo, idAtual)) {
            throw ErroApi.conflito("Já existe um material com esse código de barras.");
        }
        if (repositorio.existeAtivoComNome(nome, idAtual)) {
            throw ErroApi.conflito("Já existe um material com esse nome.");
        }
        return new DadosMaterial(nome, codigo, lote, validade, minimo);
    }

    private static String vazioParaNull(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
