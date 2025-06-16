package br.uerj.graduacao.peer;

import br.uerj.graduacao.torrent.Torrent;
import br.uerj.graduacao.torrent.TorrentGenerator;

public class PeerMain {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: java -jar peer.jar <tracker-address> <file-name>");
            System.err.println("Exemplo: java -jar peer.jar http://localhost:7000 semic.docx");
            return;
        }

        final String trackerAddress = args[0];
        final String fileName = args[1];
        int peerPort = 8001;
        if (args.length > 2) {
            try {
                peerPort = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                System.err.println("Porta inválida, usando 8001.");
            }
        } else {
            peerPort = 8001 + (int) (Math.random() * 1000); // Se não passar, usa aleatória
        }

        Torrent torrent = TorrentGenerator.generateTorrent(trackerAddress, fileName);
        if (torrent == null || torrent.getSize() == 0) {
            System.err.println("Arquivo " + fileName + " nao encontrado ou vazio. Encerrando.");
            return;
        }

        Peer peer = new Peer(peerPort, torrent);
        
        // Inicia o display de progresso para este peer específico
        PeerProgressDisplay display = new PeerProgressDisplay(peer, torrent.getNumBlocks());
        Thread displayThread = new Thread(display);
        displayThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            peer.stop();
            display.stop(); // Para o display
            try {
                displayThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("Peer " + peer.getId() + " encerrado.");
        }));

        peer.start();
    }
}