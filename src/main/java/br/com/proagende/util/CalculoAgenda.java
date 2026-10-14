package br.com.proagende.util;

import java.time.LocalDateTime;

/**
 * Cálculos de agenda sem dependência de banco ou interface.
 * Extraídos de AgendamentoService (Extract Method / Extract Class) para poderem ser
 * testados isoladamente com JUnit e reaproveitados pela futura versão web.
 */
public final class CalculoAgenda {

    /** Um serviço sem duração cadastrada ocupa ao menos 1 minuto, para que dois no mesmo instante conflitem. */
    public static final int DURACAO_MINIMA_MINUTOS = 1;

    private CalculoAgenda() {
    }

    /** Duração considerada no cálculo: a cadastrada, ou o mínimo quando for zero, negativa ou ausente. */
    public static int duracaoEfetiva(int duracaoMinutos) {
        return duracaoMinutos <= 0 ? DURACAO_MINIMA_MINUTOS : duracaoMinutos;
    }

    /** Horário em que o atendimento termina: início + duração efetiva. */
    public static LocalDateTime calcularTermino(LocalDateTime inicio, int duracaoMinutos) {
        return inicio.plusMinutes(duracaoEfetiva(duracaoMinutos));
    }

    /**
     * Indica se dois intervalos [início, fim) se sobrepõem.
     * Intervalos que apenas se tocam (fim de um = início do outro) NÃO se sobrepõem.
     */
    public static boolean haSobreposicao(LocalDateTime inicioA, LocalDateTime fimA,
                                         LocalDateTime inicioB, LocalDateTime fimB) {
        return inicioA.isBefore(fimB) && inicioB.isBefore(fimA);
    }
}
