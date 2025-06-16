package br.uerj.graduacao;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ProgressDisplay implements Runnable {
    private final List<Peer> peers;
    private final long totalBlocks;
    private final double blocksPerSlot;
    private volatile boolean allPeersComplete = false;

    public ProgressDisplay(List<Peer> peers, long totalBlocks) {
        this.peers = peers;
        this.totalBlocks = totalBlocks;
        this.blocksPerSlot = (double) totalBlocks / Constants.PROGRESS_BAR_WIDTH;
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
        String status = peer.getStatus().toString();
        Set<PeerInfo> unchoked = peer.getUnchokedPeers();
        String neighbors = unchoked.stream()
                .limit(5)
                .map(p -> "peer-" + p.port)
                .collect(Collectors.joining(", "));

        System.out.printf("Peer: %-12s | Status: %-15s | Vizinhos (Unchoked): %s\n", peerId, status, neighbors);

        // --- AQUI A GENTE PASSA O PEER INTEIRO ---
        String progressBar = buildProgressBar(peer);
        double percentage = (double) peer.getMyBlocks().size() / this.totalBlocks * 100;
        System.out.printf("%s %.2f%%\n\n", progressBar, percentage);
    }

    private String buildProgressBar(Peer peer) {
        PeerStatus status = peer.getStatus();
        Set<Long> myBlocks = peer.getMyBlocks();
        StringBuilder bar = new StringBuilder("[");

        // Decide a cor dos blocos completos com base no status geral do peer
        String completedBlockColor;
        if (status == PeerStatus.SEMEANDO) {
            completedBlockColor = Constants.ANSI_GREEN;
        } else {
            // Qualquer outro status (Baixando, Verificando, etc.) usa amarelo
            completedBlockColor = Constants.ANSI_YELLOW;
        }

        for (int i = 0; i < Constants.PROGRESS_BAR_WIDTH; i++) {
            long startBlock = (long) (i * blocksPerSlot);
            long endBlock = (long) ((i + 1) * blocksPerSlot);

            boolean slotComplete = true;
            for (long j = startBlock; j < endBlock; j++) {
                if (!myBlocks.contains(j)) {
                    slotComplete = false;
                    break;
                }
            }

            if (slotComplete) {
                // Pinta o bloco completo com verde
                bar.append(completedBlockColor + "█" + Constants.ANSI_RESET);
            } else {
                // O bloco incompleto fica na cor padrão (branco/preto)
                bar.append("-");
            }
        }
        bar.append("]");
        return bar.toString();
    }

    private void clearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}
