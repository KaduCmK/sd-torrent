package br.uerj.graduacao;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // --- CONFIGURAÇÃO DA SIMULAÇÃO ---
        final int NUMBER_OF_PEERS = 5;
        final int TRACKER_PORT = 7000;
        final int PEER_START_PORT = 8001; // Porta inicial para o primeiro peer
        final String TRACKER_ADDRESS = "http://localhost:" + TRACKER_PORT;
        // ----------------------------------

        TorrentGenerator torrent = TorrentGenerator.getInstance("luffy.jpeg");

        Tracker tracker = new Tracker("luffy.jpeg");
        tracker.start(TRACKER_PORT);

        System.out.println("Tracker iniciado em " + TRACKER_ADDRESS);
        System.out.println("Iniciando simulação com " + NUMBER_OF_PEERS + " peers...");
        Thread.sleep(1000);

        // Lista para guardar os peers criados, caso precise usar depois
        List<Peer> createdPeers = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_PEERS; i++) {
            System.out.println("----------------------------------------");

            // Calcula a porta e o ID para o peer atual
            int currentPort = PEER_START_PORT + i;
            String peerId = "peer" + (i + 1);

            System.out.println("Iniciando " + peerId + " na porta " + currentPort + "...");

            FileManager fileManager = new FileManager("luffy.jpeg", torrent.getSize(), torrent.getNumBlocks());

            Peer peer = new Peer("127.0.0.1", currentPort, peerId, fileManager);
            createdPeers.add(peer);

            peer.startServer();
            peer.connectToTracker(TRACKER_ADDRESS);

            // Pausa entre as conexões pra dar tempo de ver os logs
            Thread.sleep(750);
        }

        System.out.println("----------------------------------------");
        System.out.println("INICIANDO TESTE DE TROCA DE BLOCOS");
        System.out.println("----------------------------------------");
        Thread.sleep(1000);

        Peer peer1 = createdPeers.get(0);
        Peer peer2 = createdPeers.get(1);

        if (!peer2.ownedBlocks.isEmpty()) {
            // 1. Peer 1 pede a lista de blocos do Peer 2
            List<Long> peer2BlockList = peer1.requestBlockListFrom(new Peer(peer2.id, peer2.port, peer2.ip, null));

            // 2. Peer 1 encontra um bloco que ele não tem, mas o Peer 2 tem
            Set<Long> peer1OwnedIndexes = new HashSet<>();
            peer1.ownedBlocks.forEach(b -> peer1OwnedIndexes.add(b.getBlockIndex()));
            
            Optional<Long> firstMissingBlock = peer2BlockList.stream()
                .filter(blockIndex -> !peer1OwnedIndexes.contains(blockIndex))
                .findFirst();

            // 3. Se encontrou um bloco, Peer 1 pede os dados desse bloco para o Peer 2
            if (firstMissingBlock.isPresent()) {
                long blockToRequest = firstMissingBlock.get();
                peer1.requestAndSaveBlockData(new Peer(peer2.id, peer2.port, peer2.ip, null), blockToRequest);
            } else {
                System.out.println("Peer 1 já tem todos os blocos que o Peer 2 ofereceu.");
            }
        } else {
            System.out.println("Peer 2 não tem blocos para compartilhar no momento.");
        }

        tracker.stop();
        System.out.println("Tracker parado.");

        // Força o encerramento do programa
        System.exit(0);
    }
}