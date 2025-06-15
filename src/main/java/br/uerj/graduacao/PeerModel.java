package br.uerj.graduacao;

import java.io.*;
import java.net.*;
import java.util.*;
import com.google.gson.*;

public class PeerModel {
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

        // AQUI entraria a lógica de troca de mensagens:
        // - O outro peer poderia pedir uma lista de blocos que eu tenho.
        // - O outro peer poderia pedir um bloco específico.

        // Por enquanto, apenas fechamos a conexão.
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // faz o gerenciamento do client
    /*
     * public void handleClient(Socket socket) {
     * try (ObjectInputStream input = new
     * ObjectInputStream(socket.getInputStream());
     * ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())
     * ) {
     * output.writeObject(response);
     * output.flush();
     * 
     * // Outros tipos...
     * } catch (IOException | ClassNotFoundException e) {
     * e.printStackTrace();
     * }
     * }
     */
}