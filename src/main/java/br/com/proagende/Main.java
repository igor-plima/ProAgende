package br.com.proagende;

import br.com.proagende.config.ConexaoBanco;
import br.com.proagende.exception.PersistenciaException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * Executa o roteiro de testes do ProAgende.
 *
 * Uso:
 *   (sem argumentos)  testa contra o MySQL configurado em banco.properties
 *   --memoria         testa as regras de negócio com repositórios em memória (sem banco)
 */
public class Main {

    public static void main(String[] args) {
        boolean emMemoria = Arrays.asList(args).contains("--memoria");
        ProAgendeApp app;

        if (emMemoria) {
            System.out.println("Modo: repositórios em memória (sem banco de dados)");
            app = ProAgendeApp.emMemoria();
        } else {
            System.out.println("Modo: MySQL (JDBC)");
            ConexaoBanco conexao;
            try {
                conexao = ConexaoBanco.aPartirDoAmbiente();
                try (Connection teste = conexao.obterConexao()) {
                    System.out.println(teste.isValid(2) ? "Conexão com o banco estabelecida com sucesso." : "Conexão inválida.");
                }
            } catch (PersistenciaException | SQLException e) {
                System.out.println("Não foi possível conectar ao banco: " + e.getMessage());
                System.out.println("Verifique banco.properties, se o MySQL está ativo e se o script em /banco foi executado.");
                System.exit(2);
                return;
            }
            app = ProAgendeApp.comJdbc(conexao);
        }

        boolean sucesso = new RoteiroDeTestes(app).executar();
        System.exit(sucesso ? 0 : 1);
    }
}
