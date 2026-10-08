# ProAgende – Sistema de Agendamento de Serviços

Camadas de negócio e persistência do sistema de agendamento **ProAgende**, extraídas do projeto desktop (Java Swing) e reorganizadas segundo os princípios SOLID, para serem reaproveitadas em uma aplicação web. **Este projeto não depende de Swing.**

## Requisitos

- Java 17 ou superior
- Maven (ou NetBeans, que já o inclui)
- MySQL 8 com o banco criado pelo script `banco/ProAgende_estrutura_e_dados.sql`

## Configuração do banco

1. Execute `banco/ProAgende_estrutura_e_dados.sql` no MySQL (cria o banco `ProAgende`, as tabelas e os dados iniciais).
2. Copie `src/main/resources/banco.properties.example` para `src/main/resources/banco.properties` e ajuste usuário e senha.
   Alternativa: defina as variáveis de ambiente `PROAGENDE_DB_URL`, `PROAGENDE_DB_USER` e `PROAGENDE_DB_PASSWORD` (têm prioridade sobre o arquivo).

> `banco.properties` está no `.gitignore`: a senha não vai para o GitHub.

## Executando os testes

Os testes ficam no `main()` (classe `br.com.proagende.Main`), que executa o `RoteiroDeTestes` (33 verificações de CRUD e regras de negócio). O roteiro cria os próprios dados e os remove ao final.

- **Com MySQL:** execute `Main` sem argumentos (no NetBeans: *Run*).
- **Sem banco (memória):** execute `Main` com o argumento `--memoria` (no NetBeans: *Project Properties → Run → Arguments*).

Saída esperada ao final: `Resultado: 33 aprovados, 0 reprovados.`

## Estrutura

```
br.com.proagende
 ├─ model/        entidades do UML + StatusAgendamento (enum)
 ├─ exception/    RegraNegocioException, PersistenciaException
 ├─ config/       FabricaConexao (interface) e ConexaoBanco
 ├─ util/         Validador, DataHoraUtil
 ├─ repository/   interfaces (Repository<T>) e implementações jdbc/ e memoria/
 ├─ service/      regras de negócio (um service por entidade)
 ├─ ProAgendeApp  montagem das dependências
 ├─ RoteiroDeTestes
 └─ Main
```

## Uso a partir de outra interface (ex.: web)

```java
ProAgendeApp app = ProAgendeApp.comJdbc();
app.agendamentos().agendar(idServico, idCliente, "15/01/2030", "10:00", StatusAgendamento.PENDENTE);
```

Violações de regra lançam `RegraNegocioException` (mensagem pronta para exibir ao usuário); falhas de banco lançam `PersistenciaException`.
