/**
 * Constantes globais do projeto.
 * 
 * <p>
 * Aqui devem ser adicionadas constantes globais do projeto, como tamanhos de
 * blocos, tempos de espera, etc.
 * </p>
 * 
 */
package br.uerj.graduacao.utils;

public final class Constants {
    private Constants() {
    }

    /**
     * Tamanho dos blocos do arquivo, em bytes.
     */
    public static final int BLOCK_SIZE_BYTES = 8192;

    /**
     * Intervalo de tempo em que os peers buscarão novos peers no tracker, em
     * segundos.
     */
    public static final int PEER_DISCOVERY_INTERVAL_SECONDS = 15;

    /**
     * Intervalo de tempo em que os peers atualizarão sua lista de vizinhos
     * unchoked, em segundos
     */
    public static final int PEER_UNCHOKE_INTERVAL_SECONDS = 10;

    /**
     * Tempo de espera entre as requisições de um peer, em milissegundos.
     * Valores maiores reduzem a velocidade de transferência.
     * Valores menos aumentam a velocidade, mas pode gerar problemas de desempenho.
     */
    public static final int PEER_INTERNAL_COOLDOWN_MS = 5;

    /**
     * Largura do painel de progresso em caracteres.
     * Para um caractere da barra estar preenchida, todos os blocos equivalentes ao
     * caractere devem estar baixados.
     * 
     * EX: Se o arquivo tem 400 blocos e a barra tem 200 blocos, então 2 blocos
     * adjacentes representam um caractere.
     */
    public static final int PROGRESS_BAR_WIDTH = 220;

    /**
     * Intervalo de atualização do painel de progresso em milissegundos.
     */
    public static final int PROGRESS_BAR_REFRESH_RATE_MS = 1000;

    /**
     * Codigo ANSI para resetar a cor do texto.
     */
    public static final String ANSI_RESET = "\u001B[0m";

    /**
     * Codigo ANSI para texto verde.
     */
    public static final String ANSI_GREEN = "\u001B[32m";

    /**
     * Codigo ANSI para texto amarelo.
     */
    public static final String ANSI_YELLOW = "\u001B[33m";

    /**
     * Codigo ANSI para texto vermelho.
     */
    public static final String ANSI_RED = "\u001B[31m";
}
