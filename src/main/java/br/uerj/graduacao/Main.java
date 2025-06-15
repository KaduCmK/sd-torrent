package br.uerj.graduacao;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // --- CONFIGURAÇÃO DA SIMULAÇÃO ---
        final int NUMBER_OF_PEERS = 5;
        final int TRACKER_PORT = 7000;
        final int PEER_START_PORT = 8001; // Porta inicial para o primeiro peer
        final String TRACKER_ADDRESS = "http://localhost:" + TRACKER_PORT;
        // ----------------------------------

        Tracker tracker = new Tracker("destiny.mp4");
        tracker.start(TRACKER_PORT);

        System.out.println("Tracker iniciado em " + TRACKER_ADDRESS);
        System.out.println("Iniciando simulação com " + NUMBER_OF_PEERS + " peers...");
        Thread.sleep(1000);

        // Lista para guardar os peers criados, caso precise usar depois
        List<PeerModel> createdPeers = new ArrayList<>();

        // Loop para criar e conectar os peers
        for (int i = 0; i < NUMBER_OF_PEERS; i++) {
            System.out.println("----------------------------------------");
            
            // Calcula a porta e o ID para o peer atual
            int currentPort = PEER_START_PORT + i;
            String peerId = "peer" + (i + 1);

            System.out.println("Iniciando " + peerId + " na porta " + currentPort + "...");
            
            PeerModel peer = new PeerModel("127.0.0.1", currentPort, peerId);
            createdPeers.add(peer);

            peer.startServer();
            peer.connectToTracker(TRACKER_ADDRESS);

            // Pausa entre as conexões pra dar tempo de ver os logs
            Thread.sleep(1000); 
        }

        System.out.println("----------------------------------------");
        System.out.println("Simulação concluída. Todos os " + createdPeers.size() + " peers foram iniciados.");
        
        tracker.stop();
        System.out.println("Tracker parado.");
        
        // Força o encerramento do programa
        System.exit(0);
    }
}