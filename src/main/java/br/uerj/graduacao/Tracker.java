package br.uerj.graduacao;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.json.JsonMapper;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public class Tracker {
    private static final Logger LOGGER = Logger.getLogger(Tracker.class.getName());

    private static final int MINIMUM_NUMBER_OF_PEERS = 5;
    private static final int PEER_SAMPLE_SIZE = 5;

    private FileManager fileManager;

    private final Set<PeerInfo> peers;
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

            if (peerId == null) {
                ctx.status(400).json(Map.of("error", "Faltando peer_id"));
                return;
            }
            if (peerPortStr == null) {
                ctx.status(400).json(Map.of("error", "Faltando porta"));
                return;
            }

            int peerPort;
            try {
                peerPort = Integer.parseInt(peerPortStr);
            } catch (NumberFormatException e) {
                ctx.status(400).json(Map.of("error", "Porta invalida"));
                return;
            }

            synchronized (this.peers) {
                if (this.peers.stream().anyMatch(p -> p.port == peerPort)) {
                    ctx.status(400).json(Map.of("error", "Porta ja em uso"));
                    return;
                }
                PeerInfo newPeer = new PeerInfo(ip, peerPort);
                peers.add(newPeer);
            }

            LOGGER.info("Peer " + peerId + " registrado em " + ip + ":" + peerPort);

            List<PeerInfo> otherPeers;
            // Sincroniza a leitura da lista de peers para enviar ao peer que se registrou
            synchronized (this.peers) {
                 otherPeers = peers.stream()
                        .filter(peer -> peer.port != peerPort) // Filtra o próprio peer que fez a requisição
                        .collect(Collectors.toList());
            }

            if (otherPeers.size() >= PEER_SAMPLE_SIZE) {
                Collections.shuffle(otherPeers);
                otherPeers = otherPeers.subList(0, PEER_SAMPLE_SIZE);
            }

            Set<BlockModel> initialBlocks = Collections.synchronizedSet(new HashSet<>());
            synchronized (remainingIndexesPool) {
                if (!remainingIndexesPool.isEmpty()) {
                    int blocksPerPeer = (int) Math.ceil((double) this.totalBlocks / MINIMUM_NUMBER_OF_PEERS);
                    int blocksToGive = Math.min(blocksPerPeer, remainingIndexesPool.size());

                    List<Integer> indexesToGive = new ArrayList<>(remainingIndexesPool.subList(0, blocksToGive));

                    for (Integer i : indexesToGive) {
                        BlockModel newBlock = fileManager.readBlock(i);
                        initialBlocks.add(newBlock);
                    }
                    remainingIndexesPool.subList(0, blocksToGive).clear();

                    LOGGER.info("Distribuindo " + initialBlocks.size() + " blocos para " + peerId + ". Restam "
                            + remainingIndexesPool.size() + " na pool.");
                } else {
                    LOGGER.info("Pool de blocos iniciais vazia. Distribuindo blocos aleatórios...");

                    int blocksToGive = (int) Math.ceil((double) this.totalBlocks / MINIMUM_NUMBER_OF_PEERS);

                    List<Long> allBlockIndices = LongStream.range(0, this.totalBlocks)
                            .boxed()
                            .collect(Collectors.toList());

                    Collections.shuffle(allBlockIndices);
                    List<Long> indexesToGive = allBlockIndices.subList(0, blocksToGive);

                    for (Long i : indexesToGive) {
                        BlockModel newBlock = fileManager.readBlock(i);
                        initialBlocks.add(newBlock);
                    }
                    LOGGER.info("Distribuindo " + initialBlocks.size() + " blocos aleatórios para " + peerId);
                }
            }

            ctx.json(Map.of(
                    "peers", otherPeers,
                    "blocks", initialBlocks));
        });

        this.app.get("/peers", ctx -> {
        String peerPortStr = ctx.queryParam("port");
        if (peerPortStr == null) {
            ctx.status(400).json(Map.of("error", "Faltando porta do requisitante"));
            return;
        }
        int requesterPort = Integer.parseInt(peerPortStr);

        List<PeerInfo> peerSample;
        synchronized (this.peers) {
            peerSample = peers.stream()
                    .filter(p -> p.port != requesterPort) // Exclui o próprio peer
                    .collect(Collectors.toList());
        }

        Collections.shuffle(peerSample);

        if (peerSample.size() > PEER_SAMPLE_SIZE) {
            peerSample = peerSample.subList(0, PEER_SAMPLE_SIZE);
        }

        ctx.json(Map.of("peers", peerSample));
    });
    }

    public void stop() {
        if (this.app != null) {
            LOGGER.info("Parando tracker...");
            this.app.stop();
        }
    }
}