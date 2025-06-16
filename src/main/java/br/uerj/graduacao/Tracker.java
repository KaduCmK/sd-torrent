package br.uerj.graduacao;

import io.javalin.Javalin;
import io.javalin.json.JsonMapper;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import com.google.gson.Gson;

public class Tracker {
    private static final Logger LOGGER = Logger.getLogger(Tracker.class.getName());
    private FileManager fileManager;

    private static final int MINIMUM_NUMBER_OF_PEERS = 5;
    private static final int PEER_SAMPLE_SIZE = 5;

    private final Set<PeerModel> peers;
    private final long totalBlocks;
    private final List<Integer> remainingIndexesPool;

    private Javalin app;

    public Tracker(String torrentFilePath) {
        this.peers = Collections.synchronizedSet(new HashSet<>());

        TorrentGenerator torrent = TorrentGenerator.getInstance(torrentFilePath);
        this.totalBlocks = torrent.getNumBlocks();

        this.fileManager = new FileManager(torrentFilePath, torrent.getSize(), torrent.getNumBlocks());

        LOGGER.info("Tracker iniciado. Total de blocos: " + this.totalBlocks);

        this.remainingIndexesPool = Collections.synchronizedList(new ArrayList<>());
        LongStream.range(0, this.totalBlocks).forEach(i -> remainingIndexesPool.add((int) i));
        Collections.shuffle(remainingIndexesPool);
        LOGGER.info("Lista de " + remainingIndexesPool.size() + " blocos foi criada e embaralhada para distribuição.");
    }

    public void start(int trackerPort) {
        Gson gson = new Gson();

        JsonMapper gsonMapper = new JsonMapper() {
            @Override
            public String toJsonString(Object obj, Type type) {
                return gson.toJson(obj, type);
            }

            @Override
            public <T> T fromJsonString(String json, Type targetType) {
                return gson.fromJson(json, targetType);
            }
        };

        this.app = Javalin.create(config -> {
            config.jsonMapper(gsonMapper);
        }).start(trackerPort);
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
            PeerModel newPeer = new PeerModel(ip, peerPort, peerId);
            peers.add(newPeer);
            LOGGER.info("Peer " + peerId + " registrado em " + ip + ":" + peerPort);

            // coletando peers aleatorios para retornar
            List<PeerModel> otherPeers = peers.stream()
                    .filter(peer -> !peer.equals(newPeer))
                    .collect(Collectors.toList());
            if (otherPeers.size() >= PEER_SAMPLE_SIZE) {
                Collections.shuffle(otherPeers);
                otherPeers = otherPeers.subList(0, PEER_SAMPLE_SIZE);
            }

            // coletando blocos iniciais para retornar
            Set<BlockModel> initialBlocks = Collections.synchronizedSet(new HashSet<>());
            if (!remainingIndexesPool.isEmpty()) {
                int bloocksPerPeer = (int) Math.ceil((double) this.totalBlocks / MINIMUM_NUMBER_OF_PEERS);
                int blocksToGive = Math.min(bloocksPerPeer, remainingIndexesPool.size());

                for (int i = 0; i < blocksToGive; i++) {
                    BlockModel newBlock = fileManager.readBlock(i);
                    initialBlocks.add(newBlock);
                }
                remainingIndexesPool.subList(0, blocksToGive).clear();

                LOGGER.info("Distribuindo " + initialBlocks.size() + " blocos para " + peerId + ". Restam "
                        + remainingIndexesPool.size() + " na pool.");
            }

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
