package br.uerj.graduacao;

public final class Constants {
    private Constants() {}

    // adicionar constantes globais aki, dessa forma
    public static final int BLOCK_SIZE_BYTES = 8192;

    public static final int PROGRESS_BAR_WIDTH = 220;
    public static final int PROGRESS_BAR_REFRESH_RATE_MS = 1000;

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_RED = "\u001B[31m";
}
