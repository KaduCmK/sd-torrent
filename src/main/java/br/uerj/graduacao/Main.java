package br.uerj.graduacao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.uerj.graduacao.peer.Peer;
import br.uerj.graduacao.torrent.TorrentGenerator;
import br.uerj.graduacao.tracker.Tracker;
import br.uerj.graduacao.utils.Constants;
import br.uerj.graduacao.utils.ProgressDisplay;
import br.uerj.graduacao.torrent.Torrent;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // --- CONFIGURAÇÃO DA SIMULAÇÃO ---
        int numberOfPeers = 5;
        int trackerPort = 7000;
        int peerStartPort = 8001;
        int peerCooldownMs = Constants.PEER_INTERNAL_COOLDOWN_MS;
        String fileName = "example.zip";
        int refreshRate = Constants.PROGRESS_BAR_REFRESH_RATE_MS;
        // ----------------------------------

        // 0. PARSE DAS FLAGS
        Map<String, String> argsMap = new HashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            if (i + 1 < args.length) {
                argsMap.put(args[i], args[i + 1]);
            }
        }

        if (argsMap.containsKey("--peers")) {
            numberOfPeers = Integer.parseInt(argsMap.get("--peers"));
        }
        if (argsMap.containsKey("--tracker-port")) {
            trackerPort = Integer.parseInt(argsMap.get("--tracker-port"));
        }
        if (argsMap.containsKey("--peer-start-port")) {
            peerStartPort = Integer.parseInt(argsMap.get("--peer-start-port"));
        }
        if (argsMap.containsKey("--file")) {
            fileName = argsMap.get("--file");
        }
        if (argsMap.containsKey("--refresh-rate")) {
            refreshRate = Integer.parseInt(argsMap.get("--refresh-rate"));
        }
        if (argsMap.containsKey("--peer-cooldown")) {
            peerCooldownMs = Integer.parseInt(argsMap.get("--peer-cooldown"));
        }

        final String TRACKER_ADDRESS = "http://localhost:" + trackerPort;

        // 1. Gera os metadados do torrent a partir do arquivo original
        Torrent torrent = TorrentGenerator.generateTorrent(TRACKER_ADDRESS, fileName);
        if (torrent.getSize() == 0) {
            System.err.println("Arquivo " + fileName + " nao encontrado ou vazio. Encerrando.");
            return;
        }

        // 2. Inicia o Tracker
        Tracker tracker = new Tracker(fileName);
        tracker.start(trackerPort);
        System.out.println("Tracker iniciado em " + TRACKER_ADDRESS);
        System.out.println("Iniciando simulação com " + numberOfPeers + " peers...");
        Thread.sleep(1000); // Dá um tempo pro tracker subir

        // 3. Cria os Peers e armazena numa lista
        List<Peer> peers = new ArrayList<>();
        for (int i = 0; i < numberOfPeers; i++) {
            int peerPort = peerStartPort + i;
            Peer peer = new Peer(peerPort, torrent, peerCooldownMs);
            peers.add(peer);
        }

        // 4. Inicia o painel de progresso
        ProgressDisplay display = new ProgressDisplay(peers, torrent.getNumBlocks(), refreshRate);
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