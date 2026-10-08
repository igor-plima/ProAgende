package br.com.proagende;

import br.com.proagende.exception.RegraNegocioException;
import br.com.proagende.model.Agendamento;
import br.com.proagende.model.Cliente;
import br.com.proagende.model.Profissional;
import br.com.proagende.model.Servico;
import br.com.proagende.model.StatusAgendamento;
import br.com.proagende.service.AgendamentoService;
import br.com.proagende.service.ClienteService;
import br.com.proagende.service.ProfissionalService;
import br.com.proagende.service.ServicoService;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Roteiro de testes executado pelo main(). Percorre o CRUD e as regras de negócio
 * criando seus próprios dados (e-mails únicos) e removendo tudo ao final,
 * de modo que os dados iniciais do banco não são alterados.
 */
public class RoteiroDeTestes {

    private final ProfissionalService profissionais;
    private final ClienteService clientes;
    private final ServicoService servicos;
    private final AgendamentoService agendamentos;

    private int aprovados;
    private int reprovados;

    public RoteiroDeTestes(ProAgendeApp app) {
        this.profissionais = app.profissionais();
        this.clientes = app.clientes();
        this.servicos = app.servicos();
        this.agendamentos = app.agendamentos();
    }

    /** @return true se todos os testes passaram */
    public boolean executar() {
        String sufixo = String.valueOf(System.currentTimeMillis());
        Deque<Runnable> limpeza = new ArrayDeque<>();

        try {
            // ------------------------------------------------ Profissional
            secao("Profissional");
            esperarRegra("rejeita e-mail inválido", () -> profissionais.cadastrar("Prof Teste", "email-invalido", "11999990000"));
            esperarRegra("rejeita nome vazio", () -> profissionais.cadastrar("  ", "a@teste.com", "11999990000"));

            final Profissional prof = profissionais.cadastrar("Prof Teste", "prof" + sufixo + "@teste.com", "11999990000");
            limpeza.push(() -> profissionais.excluir(prof.getIdProfissional()));
            verificar("cadastro gera id", prof.getIdProfissional() > 0);

            profissionais.atualizar(prof.getIdProfissional(), "Prof Teste Atualizado", prof.getEmail(), "11988887777");
            verificar("atualização é persistida", "11988887777".equals(profissionais.buscar(prof.getIdProfissional()).getTelefone()));
            verificar("listagem contém o profissional",
                    profissionais.listar().stream().anyMatch(p -> p.getIdProfissional() == prof.getIdProfissional()));

            // ------------------------------------------------ Cliente
            secao("Cliente");
            esperarRegra("rejeita telefone com letras", () -> clientes.cadastrar("Cli Teste", "c@teste.com", "11abc"));

            final Cliente cli = clientes.cadastrar("Cli Teste", "cli" + sufixo + "@teste.com", "11911112222");
            limpeza.push(() -> clientes.excluir(cli.getIdCliente()));
            verificar("cadastro gera id", cli.getIdCliente() > 0);

            clientes.atualizar(cli.getIdCliente(), cli.getNome(), "novo" + sufixo + "@teste.com", cli.getTelefone());
            verificar("e-mail atualizado", clientes.buscar(cli.getIdCliente()).getEmail().startsWith("novo"));

            // ------------------------------------------------ Serviço
            secao("Serviço");
            esperarRegra("rejeita profissional inexistente (bug do if/else do desktop)",
                    () -> servicos.cadastrar(999_999_999, "Serviço X", "Descrição", 50.0, 30));
            esperarRegra("rejeita preço zero", () -> servicos.cadastrar(prof.getIdProfissional(), "Serviço X", "Descrição", 0, 30));

            final Servico serv60 = servicos.cadastrar(prof.getIdProfissional(), "Serviço 60 min", "Descrição", 80.0, 60);
            limpeza.push(() -> servicos.excluir(serv60.getIdServico()));
            final Servico serv30 = servicos.cadastrar(prof.getIdProfissional(), "Serviço 30 min", "Descrição", 40.0, 30);
            limpeza.push(() -> servicos.excluir(serv30.getIdServico()));
            verificar("cadastro gera id", serv60.getIdServico() > 0 && serv30.getIdServico() > 0);

            servicos.atualizar(serv60.getIdServico(), prof.getIdProfissional(), serv60.getNome(), "Descrição", 75.0, 60);
            verificar("preço atualizado", servicos.buscar(serv60.getIdServico()).getPreco() == 75.0);
            verificar("listagem por profissional retorna 2 serviços", servicos.listarPorProfissional(prof.getIdProfissional()).size() == 2);

            // ------------------------------------------------ Agendamento
            secao("Agendamento");
            final LocalDateTime base = LocalDateTime.of(2030, 1, 15, 10, 0);

            final Agendamento ag1 = agendamentos.agendar(serv60.getIdServico(), cli.getIdCliente(), base, StatusAgendamento.CONFIRMADO);
            limpeza.push(() -> agendamentos.excluir(ag1.getIdAgendamento()));
            verificar("agendamento gera id e guarda status", ag1.getIdAgendamento() > 0 && ag1.getStatus() == StatusAgendamento.CONFIRMADO);

            esperarRegra("rejeita conflito no mesmo horário",
                    () -> agendamentos.agendar(serv60.getIdServico(), cli.getIdCliente(), base, StatusAgendamento.PENDENTE));
            esperarRegra("rejeita conflito por sobreposição (10:30 dentro do atendimento 10:00-11:00)",
                    () -> agendamentos.agendar(serv30.getIdServico(), cli.getIdCliente(), base.plusMinutes(30), null));

            final Agendamento ag2 = agendamentos.agendar(serv30.getIdServico(), cli.getIdCliente(), base.plusMinutes(60), null);
            limpeza.push(() -> agendamentos.excluir(ag2.getIdAgendamento()));
            verificar("aceita horário logo após o término (11:00) e usa status padrão pendente",
                    ag2.getStatus() == StatusAgendamento.PENDENTE);

            esperarRegra("rejeita data inexistente (31/02/2030)",
                    () -> agendamentos.agendar(serv60.getIdServico(), cli.getIdCliente(), "31/02/2030", "10:00", null));
            esperarRegra("rejeita hora inválida (25:00)",
                    () -> agendamentos.agendar(serv60.getIdServico(), cli.getIdCliente(), "16/01/2030", "25:00", null));

            final Agendamento ag3 = agendamentos.agendar(serv60.getIdServico(), cli.getIdCliente(), "16/01/2030", "09:30", null);
            limpeza.push(() -> agendamentos.excluir(ag3.getIdAgendamento()));
            verificar("data e hora em texto convertidas corretamente",
                    LocalDateTime.of(2030, 1, 16, 9, 30).equals(agendamentos.buscar(ag3.getIdAgendamento()).getDataHora()));

            verificar("clientes por serviço retorna o cliente uma única vez",
                    agendamentos.listarClientesPorServico(serv60.getIdServico()).size() == 1);
            verificar("consulta por serviço e cliente retorna 2 agendamentos",
                    agendamentos.listarPorServicoECliente(serv60.getIdServico(), cli.getIdCliente()).size() == 2);

            agendamentos.alterarStatus(ag3.getIdAgendamento(), StatusAgendamento.CONFIRMADO);
            verificar("status alterado para confirmado",
                    agendamentos.buscar(ag3.getIdAgendamento()).getStatus() == StatusAgendamento.CONFIRMADO);

            agendamentos.cancelar(ag1.getIdAgendamento());
            verificar("cancelamento persistido",
                    agendamentos.buscar(ag1.getIdAgendamento()).getStatus() == StatusAgendamento.CANCELADO);

            final Agendamento ag4 = agendamentos.agendar(serv60.getIdServico(), cli.getIdCliente(), base, null);
            limpeza.push(() -> agendamentos.excluir(ag4.getIdAgendamento()));
            verificar("horário de agendamento cancelado volta a ficar livre", ag4.getIdAgendamento() > 0);

            esperarRegra("não reativa o cancelado se o horário foi ocupado",
                    () -> agendamentos.alterarStatus(ag1.getIdAgendamento(), StatusAgendamento.CONFIRMADO));

            // ------------------------------------------------ Exclusões
            secao("Exclusão e integridade");
            esperarRegra("bloqueia excluir cliente com agendamentos", () -> clientes.excluir(cli.getIdCliente()));
            esperarRegra("bloqueia excluir serviço com agendamentos", () -> servicos.excluir(serv60.getIdServico()));
            esperarRegra("bloqueia excluir profissional com serviços", () -> profissionais.excluir(prof.getIdProfissional()));

            agendamentos.excluir(ag1.getIdAgendamento());
            agendamentos.excluir(ag2.getIdAgendamento());
            agendamentos.excluir(ag3.getIdAgendamento());
            agendamentos.excluir(ag4.getIdAgendamento());
            esperarRegra("agendamento excluído deixa de existir", () -> agendamentos.buscar(ag1.getIdAgendamento()));

            servicos.excluir(serv60.getIdServico());
            servicos.excluir(serv30.getIdServico());
            clientes.excluir(cli.getIdCliente());
            profissionais.excluir(prof.getIdProfissional());
            esperarRegra("serviço excluído deixa de existir", () -> servicos.buscar(serv60.getIdServico()));
            esperarRegra("cliente excluído deixa de existir", () -> clientes.buscar(cli.getIdCliente()));
            esperarRegra("profissional excluído deixa de existir", () -> profissionais.buscar(prof.getIdProfissional()));

        } catch (RuntimeException e) {
            reprovados++;
            System.out.println("  [FALHOU] erro inesperado: " + e);
            e.printStackTrace(System.out);
        } finally {
            // Remove o que sobrou em caso de falha no meio do roteiro (ordem inversa à criação).
            while (!limpeza.isEmpty()) {
                try {
                    limpeza.pop().run();
                } catch (RuntimeException ignorado) {
                    // já removido pelo próprio roteiro, ou bloqueado por dependência: nada a fazer
                }
            }
        }

        System.out.println();
        System.out.println("Resultado: " + aprovados + " aprovados, " + reprovados + " reprovados.");
        return reprovados == 0;
    }

    // ------------------------------------------------------------ apoio

    private void secao(String titulo) {
        System.out.println();
        System.out.println("--- " + titulo + " ---");
    }

    private void verificar(String descricao, boolean condicao) {
        if (condicao) {
            aprovados++;
            System.out.println("  [OK]     " + descricao);
        } else {
            reprovados++;
            System.out.println("  [FALHOU] " + descricao);
        }
    }

    /** O teste passa se a ação for rejeitada com RegraNegocioException. */
    private void esperarRegra(String descricao, Runnable acao) {
        try {
            acao.run();
            reprovados++;
            System.out.println("  [FALHOU] " + descricao + " (nenhuma regra foi aplicada)");
        } catch (RegraNegocioException e) {
            aprovados++;
            System.out.println("  [OK]     " + descricao + "  -> \"" + e.getMessage() + "\"");
        }
    }
}
