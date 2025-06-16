package br.uerj.graduacao;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.*;

public class Peer {
    private static final Logger LOGGER = Logger.getLogger(Peer.class.getName());

    public String ip;
    public int port;
    public String id;
    private final FileManager fileManager;

    public List<Peer> neighbors = new ArrayList<>();
    public List<BlockModel> ownedBlocks = new ArrayList<>();

    public Peer(String ip, int port, String id, FileManager fileManager) {
        this.ip = ip;
        this.port = port;
        this.id = id;
        this.fileManager = fileManager;
    }

    public Peer(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.id = null;
        this.fileManager = null;
    }

    public void connectToTracker(String trackerAddress) {
        try {
            URL url = new URL(trackerAddress + "/register?peer_id=" + this.id + "&port=" + this.port);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                Gson gson = new Gson();
                InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                JsonObject response = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray peers = response.getAsJsonArray("peers");

                for (JsonElement element : peers) {
                    JsonObject peer = element.getAsJsonObject();
                    String peerIp = peer.get("ip").getAsString();
                    int peerPort = peer.get("port").getAsInt();

                    if (peerPort == this.port && peerIp.equals(this.ip))
                        continue;

                    Peer p = new Peer(peerIp, peerPort);
                    this.neighbors.add(p);
                }

                JsonArray blocks = response.getAsJsonArray("blocks");
                for (JsonElement element : blocks) {
                    BlockModel block = gson.fromJson(element, BlockModel.class);
                    this.ownedBlocks.add(block);
                }

                System.out.println("Peer [" + this.id + "] conectado ao tracker com sucesso!");
                System.out.println("Peers vizinhos: " + neighbors.toString());
                System.out.println("Blocos iniciais recebidos: " + ownedBlocks.toString());
            } else {
                System.out.println(
                        "Erro na comunicação com o tracker para o peer [" + this.id + "]. Código " + responseCode);
                InputStreamReader errorReader = new InputStreamReader(connection.getErrorStream());
                JsonObject errorResponse = JsonParser.parseReader(errorReader).getAsJsonObject();
                System.out.println("Mensagem: " + errorResponse.get("error").getAsString());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Starta o server pelo lado do peer
    public void startServer() {
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(this.port)) {
                System.out.println(">> Servidor do Peer [" + this.id + "] escutando na porta " + this.port);
                while (true) {
                    Socket socket = server.accept();
                    new Thread(() -> handleClient(socket)).start();
                }
            } catch (IOException e) {
                // Silencia o erro de "Address already in use" que pode acontecer ao parar
            }
        }).start();
    }

    /**
     * Lida com conexões de entrada de outros peers.
     * 
     * @param socket A conexão com o outro peer.
     */
    public void handleClient(Socket socket) {
        System.out.println(
                ">> Peer [" + this.id + "] recebeu uma conexão de " + socket.getInetAddress() + ":" + socket.getPort());

        try {
            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());

            String request = (String) input.readObject();

            LOGGER.info("Peer [" + this.id + "] recebeu uma mensagem: " + request);

            if (request.equals("GET_BLOCKS_LIST")) {
                List<Long> myIndexes = new ArrayList<>();
                for (BlockModel b : this.ownedBlocks) {
                    myIndexes.add(b.getBlockIndex());
                }
                output.writeObject(myIndexes);
            } else if (request.startsWith("GET_BLOCK_DATA:")) {
                long blockIndex = Long.parseLong(request.split(":")[1]);
                // TODO: Verificar se o peer tem o bloco
                BlockModel blockToSend = this.fileManager.readBlock(blockIndex);
                output.writeObject(blockToSend);
            }
            output.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Por enquanto, apenas fechamos a conexão.
        try {
            socket.close();
        } catch (IOException e) {
            /* */}
    }

    /**
     * LADO CLIENTE: Pede a um vizinho a lista de blocos que ele possui.
     * 
     * @return Uma lista de índices de blocos.
     */
    public List<Long> requestBlockListFrom(Peer neighbor) {
        LOGGER.info("Peer [" + this.id + "] pedindo lista de blocos para " + neighbor.ip + ":" + neighbor.port);
        try (Socket socket = new Socket(neighbor.ip, neighbor.port);
                ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());) {

            output.flush();
            try (ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
                output.writeObject("GET_BLOCKS_LIST");
                output.flush();
                return (List<Long>) input.readObject();
            }

        } catch (Exception e) {
            LOGGER.warning("Não foi possível obter lista de blocos de " + neighbor.ip + ":" + neighbor.port);
            return Collections.emptyList(); // Retorna lista vazia em caso de falha
        }
    }

    /**
     * LADO CLIENTE: Pede a um vizinho os dados de um bloco específico.
     */
    public void requestAndSaveBlockData(Peer neighbor, long blockIndex) {
        LOGGER.info(
                "Peer [" + this.id + "] pedindo o bloco " + blockIndex + " para " + neighbor.ip + ":" + neighbor.port);
        try (Socket socket = new Socket(neighbor.ip, neighbor.port);
                ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());) {

            output.flush();

            try (ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
            output.writeObject("GET_BLOCK_DATA:" + blockIndex);
            output.flush();

            BlockModel receivedBlock = (BlockModel) input.readObject();
            
            if (receivedBlock != null && receivedBlock.getData() != null) {
                this.fileManager.writeBlock(receivedBlock);
                this.ownedBlocks.add(receivedBlock);
                LOGGER.info("<<< Bloco " + blockIndex + " recebido e salvo com sucesso!");
            } else {
                LOGGER.warning("Recebido bloco nulo ou sem dados do vizinho.");
            }
        }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Falha ao obter dados do bloco " + blockIndex, e);
        }
    }
}
