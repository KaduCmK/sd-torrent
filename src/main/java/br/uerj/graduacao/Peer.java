package br.uerj.graduacao;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.json.JsonMapper;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Peer extends PeerInfo {
    private static final Logger LOGGER = Logger.getLogger(Peer.class.getName());

    private final String id;
    private final String trackerAddress;
    private final FileManager fileManager;
    private final long totalBlocks;

    private final Set<Long> myBlocks = Collections.synchronizedSet(new HashSet<>());
    private final Set<PeerInfo> knownPeers = Collections.synchronizedSet(new HashSet<>());
    private final Set<PeerInfo> unchokedPeers = Collections.synchronizedSet(new HashSet<>());

    private Javalin server;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public Peer(int port, String trackerAddress, String originalFileName, long totalBlocks, long fileSize) {
        super("localhost", port);
        this.id = "peer-" + port;
        this.trackerAddress = trackerAddress;
        this.totalBlocks = totalBlocks;
        String peerFileName = originalFileName.replace(".", "-" + port + ".");
        this.fileManager = new FileManager("./" + peerFileName, fileSize, totalBlocks);
    }

    public String getId() {
        return this.id;
    }

    public Set<Long> getMyBlocks() {
        return Collections.unmodifiableSet(myBlocks);
    }

    public Set<PeerInfo> getUnchokedPeers() {
        return Collections.unmodifiableSet(unchokedPeers);
    }

    public boolean isComplete() {
        return myBlocks.size() >= totalBlocks;
    }

    public void start() {
        LOGGER.info("[" + id + "] Iniciando...");
        setupHttpServer();
        registerWithTracker();
        startTitForTatScheduler();
        runLifecycle();
        stop();
    }

    private void setupHttpServer() {
        // Lógica mantida conforme solicitado
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
        server = Javalin.create(config -> {
            config.jsonMapper(gsonMapper);
        }).start(this.port);

        server.get("/hello", ctx -> {
            synchronized (myBlocks) {
                ctx.json(myBlocks);
            }
        });

        server.get("/request_block/{index}", ctx -> {
            String requesterPortStr = ctx.queryParam("requester_port");
            if (requesterPortStr == null) {
                ctx.status(HttpStatus.BAD_REQUEST).result("Faltando o parametro: requester_port");
                return;
            }

            int requesterPort = Integer.parseInt(requesterPortStr);
            PeerInfo requester = new PeerInfo(ctx.ip(), requesterPort);

            if (!isUnchoked(requester)) {
                ctx.status(HttpStatus.FORBIDDEN).result("You are choked.");
                return;
            }

            long blockIndex = Long.parseLong(ctx.pathParam("index"));
            if (myBlocks.contains(blockIndex)) {
                BlockModel block = fileManager.readBlock(blockIndex);
                ctx.json(block);
            } else {
                ctx.status(HttpStatus.NOT_FOUND).result("Block not found.");
            }
        });
        LOGGER.info("[" + id + "] Servidor HTTP iniciado na porta " + this.port);
    }

    private void registerWithTracker() {
        try {
            String url = trackerAddress + "/register?peer_id=" + id + "&port=" + this.port;
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            Type responseType = new TypeToken<Map<String, Object>>() {
            }.getType();
            Map<String, Object> responseMap = gson.fromJson(response.body(), responseType);

            Type peerListType = new TypeToken<List<PeerInfo>>() {
            }.getType();
            List<PeerInfo> peers = gson.fromJson(gson.toJson(responseMap.get("peers")), peerListType);

            Type blockListType = new TypeToken<Set<BlockModel>>() {
            }.getType();
            Set<BlockModel> initialBlocks = gson.fromJson(gson.toJson(responseMap.get("blocks")), blockListType);

            knownPeers.addAll(peers);

            LOGGER.info("[" + id + "] Registrado no tracker. Recebeu " + peers.size() + " peers e "
                    + initialBlocks.size() + " blocos.");

            for (BlockModel block : initialBlocks) {
                fileManager.writeBlock(block);
                myBlocks.add(block.getBlockIndex());
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "[" + id + "] Erro ao registrar no tracker.", e);
        }
    }

    private void startTitForTatScheduler() {
        scheduler.scheduleAtFixedRate(this::updateUnchokedPeers, 5, 10, TimeUnit.SECONDS);
    }

    private void updateUnchokedPeers() {
        if (knownPeers.isEmpty())
            return;

        // --- LÓGICA DE UNCHOKE CORRIGIDA E OTIMIZADA ---
        Map<Long, Integer> blockFrequencies = getBlockFrequencies();
        if (blockFrequencies.isEmpty()) return;

        // 1. Define "blocos raros" como os 30% menos comuns para focar a pontuação.
        int rareThreshold = (int) (blockFrequencies.size() * 0.3);
        Set<Long> rareBlockSet = blockFrequencies.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(Math.max(1, rareThreshold)) // Garante que sempre haja pelo menos 1 bloco raro
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet()); // Usa um Set para verificação O(1) (muito rápido)

        // 2. Pontua os peers baseado em quantos blocos raros eles possuem.
        Map<PeerInfo, Integer> peerScores = new HashMap<>();
        for (PeerInfo peer : new ArrayList<>(knownPeers)) {
            Set<Long> peerBlocks = getBlocksFromPeer(peer);
            int score = 0;
            for (Long block : peerBlocks) {
                if (rareBlockSet.contains(block)) { // Verificação agora é instantânea
                    score++;
                }
            }
            peerScores.put(peer, score);
        }

        // 3. Seleciona os 4 peers com maior pontuação.
        List<PeerInfo> sortedPeers = peerScores.entrySet().stream()
                .sorted(Map.Entry.<PeerInfo, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        unchokedPeers.clear();
        sortedPeers.stream().limit(4).forEach(unchokedPeers::add);

        // 4. Adiciona 1 peer otimista aleatório para dar chance a todos.
        List<PeerInfo> chokedPeers = new ArrayList<>(knownPeers);
        chokedPeers.removeAll(unchokedPeers);
        if (!chokedPeers.isEmpty()) {
            unchokedPeers.add(chokedPeers.get(new Random().nextInt(chokedPeers.size())));
        }
    }

    private void runLifecycle() {
        boolean selfComplete = false;

        while (true) {
            try {
                if (myBlocks.size() < totalBlocks) {
                    findAndDownloadRarestBlock();
                }
                else {
                    if (!selfComplete) {
                        selfComplete = true;
                        LOGGER.info("[" + id + "] \uD83C\uDF89 Download completo! Aguardando vizinhos...");
                    }
                    if (areAllNeighborsComplete()) {
                        LOGGER.info("[" + id + "] Todos os vizinhos completaram o download. Desligando.");
                        break;
                    }
                }
                Thread.sleep(250);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.warning("[" + id + "] Loop de vida interrompido.");
                break;
            }
        }
    }

    private boolean areAllNeighborsComplete() {
        if (knownPeers.isEmpty()) {
            return true;
        }
        for (PeerInfo peer : new ArrayList<>(knownPeers)) {
            Set<Long> neighborBlocks = getBlocksFromPeer(peer);
            if (neighborBlocks.size() < totalBlocks) {
                return false;
            }
        }
        return true;
    }

    private void findAndDownloadRarestBlock() {
        Map<Long, Integer> blockFrequencies = getBlockFrequencies();
        if (blockFrequencies.isEmpty()) {
            return;
        }

        List<Map.Entry<Long, Integer>> sortedFrequencies = blockFrequencies.entrySet().stream()
                .filter(entry -> !myBlocks.contains(entry.getKey()))
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toList());

        for (Map.Entry<Long, Integer> entry : sortedFrequencies) {
            long rarestBlockIndex = entry.getKey();
            List<PeerInfo> candidates = findPeersWithBlock(rarestBlockIndex);

            Collections.shuffle(candidates);

            for (PeerInfo peer : candidates) {
                if (downloadBlockFromPeer(peer, rarestBlockIndex)) {
                    return;
                }
            }
        }
    }

    private boolean downloadBlockFromPeer(PeerInfo peer, long blockIndex) {
        try {
            String url = "http://" + peer.ip + ":" + peer.port + "/request_block/" + blockIndex + "?requester_port="
                    + this.port;
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                BlockModel block = gson.fromJson(response.body(), BlockModel.class);
                fileManager.writeBlock(block);
                myBlocks.add(block.getBlockIndex());
                return true;
            }
        } catch (Exception e) {
            LOGGER.warning(
                    "[" + id + "] Falha ao baixar bloco " + blockIndex + " de " + peer + ". Motivo: " + e.getMessage());
            knownPeers.remove(peer);
        }
        return false;
    }

    private Map<Long, Integer> getBlockFrequencies() {
        Map<Long, Integer> frequencies = new HashMap<>();
        for (PeerInfo peer : new ArrayList<>(knownPeers)) {
            Set<Long> peerBlocks = getBlocksFromPeer(peer);
            for (Long blockIndex : peerBlocks) {
                frequencies.put(blockIndex, frequencies.getOrDefault(blockIndex, 0) + 1);
            }
        }
        return frequencies;
    }

    private Set<Long> getBlocksFromPeer(PeerInfo peer) {
        try {
            String url = "http://" + peer.ip + ":" + peer.port + "/hello";
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Type type = new TypeToken<Set<Long>>() {
                }.getType();
                return gson.fromJson(response.body(), type);
            }
        } catch (Exception e) {
            LOGGER.warning("[" + id + "] Peer " + peer + " não respondeu. Removendo da lista.");
            knownPeers.remove(peer);
        }
        return Collections.emptySet();
    }

    private List<PeerInfo> findPeersWithBlock(long blockIndex) {
        List<PeerInfo> peersWithBlock = new ArrayList<>();
        for (PeerInfo peer : new ArrayList<>(knownPeers)) {
            if (getBlocksFromPeer(peer).contains(blockIndex)) {
                peersWithBlock.add(peer);
            }
        }
        return peersWithBlock;
    }

    private boolean isUnchoked(PeerInfo peer) {
        return unchokedPeers.stream().anyMatch(p -> p.port == peer.port);
    }

    public void stop() {
        LOGGER.info("[" + id + "] Encerrando...");
        scheduler.shutdownNow();
        if (server != null) {
            server.stop();
        }
        LOGGER.info("[" + id + "] Desligado.");
    }
}