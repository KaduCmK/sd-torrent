package br.uerj.graduacao.utils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import br.uerj.graduacao.peer.Peer;
import br.uerj.graduacao.peer.PeerInfo;
import br.uerj.graduacao.peer.PeerStatus;

public class ProgressDisplay implements Runnable {
    private final List<Peer> peers;
    private final long totalBlocks;
    private final double blocksPerSlot;
    private final int refreshRateMs;

    public ProgressDisplay(List<Peer> peers, long totalBlocks, int refreshRateMs) {
        this.peers = peers;
        this.totalBlocks = totalBlocks;
        this.blocksPerSlot = (double) totalBlocks / Constants.PROGRESS_BAR_WIDTH;
        this.refreshRateMs = refreshRateMs;
    }

    @Override
    public void run() {
        try {
            while (true) {
                clearConsole();

                for (Peer peer : peers) {
                    drawPeerStatus(peer);
                }

                Thread.sleep(refreshRateMs);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Painel de progresso interrompido.");
        }
    }

    private void drawPeerStatus(Peer peer) {
        String peerId = peer.getId();
        PeerStatus peerStatusEnum = peer.getStatus();
        String statusString = peerStatusEnum.toString();

        String statusColor;
        switch (peerStatusEnum) {
            case BAIXANDO:
            case VERIFICANDO:
                statusColor = Constants.ANSI_YELLOW;
                break;
            case SEMEANDO:
                statusColor = Constants.ANSI_GREEN;
                break;
            case CORROMPIDO:
                statusColor = Constants.ANSI_RED;
                break;
            default:
                statusColor = Constants.ANSI_RESET;
                break;
        }

        String coloredStatus = statusColor + String.format("%-15s", statusString) + Constants.ANSI_RESET;

        Set<PeerInfo> unchoked = peer.getUnchokedPeers();
        String neighbors = unchoked.stream()
                .limit(5)
                .map(p -> "peer-" + p.port)
                .collect(Collectors.joining(", "));

        System.out.printf("Peer: %-12s | Status: %s | Vizinhos (Unchoked): %s\n", peerId, coloredStatus, neighbors);

        String progressBar = buildProgressBar(peer);
        double percentage = (double) peer.getMyBlocks().size() / this.totalBlocks * 100;
        System.out.printf("%s %.2f%%\n\n", progressBar, percentage);
    }

    private String buildProgressBar(Peer peer) {
        PeerStatus status = peer.getStatus();
        Set<Long> myBlocks = peer.getMyBlocks();
        StringBuilder bar = new StringBuilder("[");

        String completedBlockColor;
        switch (status) {
            case SEMEANDO:
                completedBlockColor = Constants.ANSI_GREEN;
                break;
            case CORROMPIDO:
                completedBlockColor = Constants.ANSI_RED;
                break;
            default: // BAIXANDO, VERIFICANDO, CONECTANDO
                completedBlockColor = Constants.ANSI_YELLOW;
                break;
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
                bar.append(completedBlockColor).append("█").append(Constants.ANSI_RESET);
            } else {
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
