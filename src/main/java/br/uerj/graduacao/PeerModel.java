package br.uerj.graduacao;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Logger;

import com.google.gson.*;

public class PeerModel {
    private static final Logger LOGGER = Logger.getLogger(PeerModel.class.getName());

    public String ip;
    public int port;
    public String id;
    public List<PeerModel> neighbors = new ArrayList<>();
    public List<BlockModel> ownedBlocks = new ArrayList<>();

    public PeerModel(String ip, int port, String id) {
        this.ip = ip;
        this.port = port;
        this.id = id;
    }

    public PeerModel(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.id = null;
    }

    public void connectToTracker(String trackerAddress) {
        try {
            URL url = new URL(trackerAddress + "/register?peer_id=" + this.id + "&port=" + this.port);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                JsonObject response = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray peers = response.getAsJsonArray("peers");

                for (JsonElement element : peers) {
                    JsonObject peer = element.getAsJsonObject();
                    String peerIp = peer.get("ip").getAsString();
                    int peerPort = peer.get("port").getAsInt();

                    if (peerPort == this.port && peerIp.equals(this.ip))
                        continue;

                    PeerModel p = new PeerModel(peerIp, peerPort);
                    this.neighbors.add(p);
                }
                JsonArray blocks = response.getAsJsonArray("blocks");

                for (JsonElement element : blocks) {
                    BlockModel b = new BlockModel(element.getAsLong());
                    this.ownedBlocks.add(b);
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

            Object request = input.readObject();

            if (request != null) {
                System.out.println("Peer [" + this.id + "] recebeu uma mensagem: " + request);
                output.writeObject(this.ownedBlocks);
                output.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Por enquanto, apenas fechamos a conexão.
        try {
            socket.close();
        } catch (IOException e) {
            /* */}
    }

    public void shareAndRequest(PeerModel neighbor) {
        LOGGER.info("Peer [" + this.id + "] compartilhando e solicitando blocos com " + neighbor.id);
        try {
            Socket socket = new Socket(neighbor.ip, neighbor.port);
            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());

            output.writeObject("GET_BLOCKS");
            output.flush();

            @SuppressWarnings("unchecked")
            List<BlockModel> receivedList = (List<BlockModel>) input.readObject();
            LOGGER.info("<<< Recebidos " + receivedList.size() + " blocos de " + neighbor.id);

            Set<Long> myBlockIndexes = new HashSet<>();
            for (BlockModel b : this.ownedBlocks) {
                myBlockIndexes.add(b.getBlockIndex());
            }

            for (BlockModel receivedBlock : receivedList) {
                if (!myBlockIndexes.contains(receivedBlock.getBlockIndex())) {
                    this.ownedBlocks.add(receivedBlock);
                    LOGGER.info("<<< Bloco novo adquirido: " + receivedBlock.getBlockIndex());
                }
            }
            socket.close();
        } catch (Exception e) {
            LOGGER.severe("Erro ao compartilhar e solicitar blocos com " + neighbor.id);
        }
    }
}
