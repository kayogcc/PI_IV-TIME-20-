// Autor: Gabriel Figueira Albasini

// Palavras-chave: classe nova · reúne repositórios e serviços · criada uma vez e compartilhada pelas conexões
package br.puc.estoquehosp.socket.servidor;

import br.puc.estoquehosp.http.Sessoes;
import br.puc.estoquehosp.repositorio.MaterialRepositorio;
import br.puc.estoquehosp.repositorio.MovimentacaoRepositorio;
import br.puc.estoquehosp.repositorio.UsuarioRepositorio;
import br.puc.estoquehosp.servico.AlertaServico;
import br.puc.estoquehosp.servico.MaterialServico;
import br.puc.estoquehosp.servico.MovimentacaoServico;
import br.puc.estoquehosp.servico.UsuarioServico;
import com.mongodb.client.MongoDatabase;

// EstoqueHosp: classe nova (não existe no Fazedor de Continhas).
// Guarda as regras de negócio do estoque, criadas uma vez e usadas por todas as SupervisorasDeConexao.
// No Fazedor de Continhas a "regra" era só a conta feita dentro da SupervisoraDeConexao.
public class Servicos
{
    private Sessoes             sessoes;
    private UsuarioServico      usuarios;
    private MaterialServico     materiais;
    private MovimentacaoServico movimentacoes;
    private AlertaServico       alertas;

    public Servicos (MongoDatabase banco) throws Exception
    {
        if (banco==null)
            throw new Exception ("Banco ausente");

        MaterialRepositorio     repositorioDeMateriais     = new MaterialRepositorio     (banco);
        MovimentacaoRepositorio repositorioDeMovimentacoes = new MovimentacaoRepositorio (banco);

        this.sessoes       = new Sessoes ();
        this.usuarios      = new UsuarioServico      (new UsuarioRepositorio (banco), this.sessoes);
        this.materiais     = new MaterialServico     (repositorioDeMateriais);
        this.movimentacoes = new MovimentacaoServico (repositorioDeMateriais, repositorioDeMovimentacoes);
        this.alertas       = new AlertaServico       (repositorioDeMateriais, repositorioDeMovimentacoes);
    }

    public Sessoes getSessoes ()
    {
        return this.sessoes;
    }

    public UsuarioServico getUsuarios ()
    {
        return this.usuarios;
    }

    public MaterialServico getMateriais ()
    {
        return this.materiais;
    }

    public MovimentacaoServico getMovimentacoes ()
    {
        return this.movimentacoes;
    }

    public AlertaServico getAlertas ()
    {
        return this.alertas;
    }
}
