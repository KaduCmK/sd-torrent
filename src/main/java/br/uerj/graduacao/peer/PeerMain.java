package br.uerj.graduacao.peer;

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

        TorrentGenerator torrent = TorrentGenerator.getInstance(fileName);
        if (torrent.getSize() == 0) {
            System.err.println("Arquivo " + fileName + " nao encontrado ou vazio. Encerrando.");
            return;
        }

        Peer peer = new Peer(
                peerPort,
                trackerAddress,
                fileName,
                torrent.getNumBlocks(),
                torrent.getSize(),
                torrent.getChecksum());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            peer.stop();
            System.out.println("Peer " + peer.getId() + " encerrado.");
        }));

        peer.start();
    }
}