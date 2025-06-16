package br.uerj.graduacao;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // --- CONFIGURAÇÃO DA SIMULAÇÃO ---
        final int NUMBER_OF_PEERS = 5;
        final int TRACKER_PORT = 7000;
        final int PEER_START_PORT = 8001;
        final String TRACKER_ADDRESS = "http://localhost:" + TRACKER_PORT;
        final String FILE_NAME = "luffy.jpeg"; // Arquivo original na pasta resources
        // ----------------------------------

        TorrentGenerator torrent = TorrentGenerator.getInstance(FILE_NAME);

        Tracker tracker = new Tracker(FILE_NAME);
        tracker.start(TRACKER_PORT);

        System.out.println("Tracker iniciado em " + TRACKER_ADDRESS);
        System.out.println("Iniciando simulação com " + NUMBER_OF_PEERS + " peers...");
        Thread.sleep(1000);

        // logica de simulacao dos peers

        tracker.stop();
        System.out.println("Tracker parado.");

        System.exit(0);
    }
}