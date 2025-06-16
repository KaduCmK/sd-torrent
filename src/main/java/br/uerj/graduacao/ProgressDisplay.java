package br.uerj.graduacao;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ProgressDisplay implements Runnable {
    private final List<Peer> peers;
    private final long totalBlocks;
    private final int progressBarWidth = 200;
    private final double blocksPerSlot;
    private volatile boolean allPeersComplete = false;

    public ProgressDisplay(List<Peer> peers, long totalBlocks) {
        this.peers = peers;
        this.totalBlocks = totalBlocks;
        this.blocksPerSlot = (double) totalBlocks / progressBarWidth;
    }

    @Override
    public void run() {
        try {
            while (!allPeersComplete) {
                clearConsole();
                boolean allDoneCurrentCycle = true;

                for (Peer peer : peers) {
                    drawPeerStatus(peer);
                    if (!peer.isComplete()) {
                        allDoneCurrentCycle = false;
                    }
                }

                allPeersComplete = allDoneCurrentCycle;
                Thread.sleep(250); // Refresh rate
            }
            // Desenha uma última vez para mostrar 100% em tudo
            clearConsole();
            for (Peer peer : peers) {
                drawPeerStatus(peer);
            }
            System.out.println("Painel de progresso encerrado.");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Painel de progresso interrompido.");
        }
    }

    private void drawPeerStatus(Peer peer) {
        String peerId = peer.getId();
        Set<PeerInfo> unchoked = peer.getUnchokedPeers();
        String neighbors = unchoked.stream()
                .limit(5)
                .map(p -> "peer-" + p.port)
                .collect(Collectors.joining(", "));

        System.out.printf("Peer: %-15s | Vizinhos (Unchoked): %s\n", peerId, neighbors);

        String progressBar = buildProgressBar(peer.getMyBlocks());
        double percentage = (double) peer.getMyBlocks().size() / this.totalBlocks * 100;
        System.out.printf("%s %.2f%%\n\n", progressBar, percentage);
    }

    private String buildProgressBar(Set<Long> myBlocks) {
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < progressBarWidth; i++) {
            long startBlock = (long) (i * blocksPerSlot);
            long endBlock = (long) ((i + 1) * blocksPerSlot);

            boolean slotComplete = true;
            for (long j = startBlock; j < endBlock; j++) {
                if (!myBlocks.contains(j)) {
                    slotComplete = false;
                    break;
                }
            }
            bar.append(slotComplete ? "█" : "-");
        }
        bar.append("]");
        return bar.toString();
    }

    private void clearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}
