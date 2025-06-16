package br.uerj.graduacao;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // --- CONFIGURAÇÃO DA SIMULAÇÃO ---
        final int NUMBER_OF_PEERS = 5;
        final int TRACKER_PORT = 7000;
        final int PEER_START_PORT = 8001;
        final String TRACKER_ADDRESS = "http://localhost:" + TRACKER_PORT;
        final String FILE_NAME = "luffy.jpeg";
        // ----------------------------------

        // 1. Gera os metadados do torrent a partir do arquivo original
        TorrentGenerator torrent = TorrentGenerator.getInstance(FILE_NAME);
        if (torrent.getSize() == 0) {
            System.err.println("Arquivo " + FILE_NAME + " nao encontrado ou vazio. Encerrando.");
            return;
        }

        // 2. Inicia o Tracker
        Tracker tracker = new Tracker(FILE_NAME);
        tracker.start(TRACKER_PORT);
        System.out.println("Tracker iniciado em " + TRACKER_ADDRESS);
        System.out.println("Iniciando simulação com " + NUMBER_OF_PEERS + " peers...");
        Thread.sleep(1000); // Dá um tempo pro tracker subir

        // 3. Inicia os Peers em threads separadas
        List<Thread> peerThreads = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_PEERS; i++) {
            int peerPort = PEER_START_PORT + i;
            Peer peer = new Peer(peerPort, TRACKER_ADDRESS, FILE_NAME, torrent.getNumBlocks(), torrent.getSize());
            Thread peerThread = new Thread(peer::start);
            peerThreads.add(peerThread);
            peerThread.start();
        }

        // 4. Espera todos os peers terminarem
        for (Thread thread : peerThreads) {
            thread.join();
        }

        System.out.println("\nTodos os peers completaram o download.");

        // 5. Para o Tracker e encerra
        tracker.stop();
        System.out.println("Tracker parado.");

        System.exit(0);
    }
}