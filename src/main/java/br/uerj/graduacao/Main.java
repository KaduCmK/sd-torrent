package br.uerj.graduacao;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // --- CONFIGURAÇÃO DA SIMULAÇÃO ---
        final int NUMBER_OF_PEERS = 10;
        final int TRACKER_PORT = 7000;
        final int PEER_START_PORT = 8001;
        final String TRACKER_ADDRESS = "http://localhost:" + TRACKER_PORT;
        final String FILE_NAME = "poster.pdf";
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

        // 3. Cria os Peers e armazena numa lista
        List<Peer> peers = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_PEERS; i++) {
            int peerPort = PEER_START_PORT + i;
            Peer peer = new Peer(peerPort, TRACKER_ADDRESS, FILE_NAME, torrent.getNumBlocks(), torrent.getSize());
            peers.add(peer);
        }

        // 4. Inicia o painel de progresso
        ProgressDisplay display = new ProgressDisplay(peers, torrent.getNumBlocks());
        Thread displayThread = new Thread(display);
        displayThread.start();

        // 5. Inicia as threads dos Peers
        List<Thread> peerThreads = new ArrayList<>();
        for (Peer peer : peers) {
            Thread peerThread = new Thread(peer::start);
            peerThreads.add(peerThread);
            peerThread.start();
        }

        // 6. Espera todos os peers terminarem
        for (Thread thread : peerThreads) {
            thread.join();
        }
        displayThread.join();

        System.out.println("\nTodos os peers completaram o download.");

        // 7. Para o Tracker e encerra
        tracker.stop();
        System.out.println("Tracker parado.");

        System.exit(0);
    }
}