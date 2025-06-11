package br.uerj.graduacao;

import io.javalin.Javalin;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Tracker {
    private static final int TOTAL_BLOCKS = 100;
    private static final double INITIAL_BLOCK_PERCENTAGE = 0.2;
    private static final int PEER_SAMPLE_SIZE = 5;

    private final Map<String, PeerModel> peers;
    private Javalin app;

    public Tracker() {
        this.peers = new ConcurrentHashMap<>();
    }

    public void start(int port) {
        this.app = Javalin.create().start(port);
        System.out.println("Tracker executando na porta " + port);

        this.app.get("/register", ctx -> {
            String peerId = ctx.queryParam("peer_id");
            String portStr = ctx.queryParam("port");
            String ip = ctx.ip();

            if (peerId == null) {
                ctx.status(400).json(Map.of("error", "Faltando peer_id"));
                return;
            }
            if (portStr == null) {
                ctx.status(400).json(Map.of("error", "Faltando porta"));
                return;
            }

            int peerPort;
            try {
                peerPort = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                ctx.status(400).json(Map.of("error", "Porta invalida"));
                return;
            }

            peers.put(peerId, new PeerModel(ip, peerPort));
            System.out.println("Peer registrado: " + peerId);

            List<PeerModel> otherPeers = peers.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(peerId))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());

            if (otherPeers.size() >= PEER_SAMPLE_SIZE) {
                Collections.shuffle(otherPeers);
                otherPeers = otherPeers.subList(0, PEER_SAMPLE_SIZE);
            }

            List<Integer> allBlocks = IntStream.range(0, TOTAL_BLOCKS).boxed().collect(Collectors.toList());
            Collections.shuffle(allBlocks);
            int numInitialBlocks = (int) (TOTAL_BLOCKS * INITIAL_BLOCK_PERCENTAGE);
            List<Integer> initialBlocks = allBlocks.subList(0, numInitialBlocks);

            ctx.json(Map.of(
                    "peers", otherPeers,
                    "blocks", initialBlocks
            ));
        });
    }

    public void stop() {
        if (this.app != null) {
            this.app.stop();
        }
    }
}
