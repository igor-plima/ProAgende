# ProAgendeTestes (Projeto Integrador, Etapa 7)

Projeto Maven de testes JUnit 5 do **ProAgende** (`ProAgende`). Não usa banco de dados: os testes de serviço usam `ProAgendeApp.emMemoria()`.

## Como executar

1. Abra o `ProAgende` no NetBeans e faça **Clean and Build** (instala o projeto no repositório Maven local).
2. Abra este projeto e use **Run > Test Project** (ou `mvn test` no terminal).

## O que é testado

| Classe de teste | Alvo |
| --- | --- |
| `util.CalculoAgendaTest` | término do atendimento, duração efetiva, sobreposição de horários |
| `util.ValidadorTest` | texto obrigatório, e-mail, telefone, valores positivos |
| `util.DataHoraUtilTest` | conversão de data e hora digitadas |
| `model.StatusAgendamentoTest` | conversão do status para o ENUM do banco |
| `service.AgendamentoServiceTest` | status padrão, conflito de horário, cancelamento e reativação |

Os casos estão descritos no Plano de Testes (CT-01 a CT-16).
