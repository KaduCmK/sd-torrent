package br.uerj.graduacao;

import io.javalin.Javalin;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Tracker {
    private static final Logger LOGGER = Logger.getLogger(Tracker.class.getName());

    private static final int TOTAL_BLOCKS = 100;
    private static final double INITIAL_BLOCK_PERCENTAGE = 0.2;
    private static final int PEER_SAMPLE_SIZE = 5;

    private final Set<PeerModel> peers;
    private Javalin app;

    public Tracker() {
        this.peers = Collections.synchronizedSet(new HashSet<>());
    }

    public void start(int trackerPort) {
        this.app = Javalin.create().start(trackerPort);
        LOGGER.info("Tracker executando na porta " + trackerPort);

        this.app.get("/register", ctx -> {
            String peerId = ctx.queryParam("peer_id");
            String peerPortStr = ctx.queryParam("port");
            String ip = ctx.ip();

            // checagem de validade dos parametros da requisicao
            if (peerId == null) {
                ctx.status(400).json(Map.of("error", "Faltando peer_id"));
                return;
            }
            if (peerPortStr == null) {
                ctx.status(400).json(Map.of("error", "Faltando porta"));
                return;
            }

            // checagem de porta em uso
            int peerPort;
            try {
                peerPort = Integer.parseInt(peerPortStr);
            } catch (NumberFormatException e) {
                ctx.status(400).json(Map.of("error", "Porta invalida"));
                return;
            }

            if (this.peers.stream().anyMatch(p -> p.port == peerPort)) {
                ctx.status(400).json(Map.of("error", "Porta ja em uso"));
                return;
            }

            // adicionando novo peer
            PeerModel newPeer = new PeerModel(ip, peerPort);
            peers.add(newPeer);

            // coletando peers aleatorios para retornar
            List<PeerModel> otherPeers = peers.stream()
                    .filter(peer -> !peer.equals(newPeer))
                    .collect(Collectors.toList());

            if (otherPeers.size() >= PEER_SAMPLE_SIZE) {
                Collections.shuffle(otherPeers);
                otherPeers = otherPeers.subList(0, PEER_SAMPLE_SIZE);
            }

            // coletando blocos iniciais para retornar
            List<Integer> allBlocks = IntStream.range(0, TOTAL_BLOCKS).boxed().collect(Collectors.toList());
            Collections.shuffle(allBlocks);
            int numInitialBlocks = (int) (TOTAL_BLOCKS * INITIAL_BLOCK_PERCENTAGE);
            List<Integer> initialBlocks = allBlocks.subList(0, numInitialBlocks);

            ctx.json(Map.of(
                    "peers", otherPeers,
                    "blocks", initialBlocks));
        });
    }

    public void stop() {
        if (this.app != null) {
            LOGGER.info("Parando tracker...");
            this.app.stop();
        }
    }
}
