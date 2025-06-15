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

    public void ConnectToTracker() {
        try {
            URL url = new URL("http://localhost" + this.port);
            HttpURLConnection connect = (HttpURLConnection) url.openConnection();
            connect.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 200) {
                //recebe lista de peers vizinhos e preenche ela
                inputStreamReader reader = new InputStreamReader(connection.getInputStream());
                JsonObject response = JsonParser.parseReader(isr).getAsJsonObject();
                JsonArray peers = response.getAsJsonArray("peers");

               for (JsonElement element : peers) {
                JsonObject peer = element.getAsJsonObject();
                String peerIp = peer.get("ip").getAsString();
                int peerPort = peer.get("port").getAsInt();
                
                if (peerPort == this.port && peerIp.equals(this.ip)) continue;

                PeerModel p = new PeerModel(id, peerIp, peerPort);
                p.startServer();
                this.neighbors.add(p);
            }
            // Listar blocos
            JsonArray blocks = response.getAsJsonArray("blocks");

            for (JsonElement element : blocks) {
                BlockModel b = new BlockModel(element.getAsLong());
                this.ownedBlocks.add(b); 
            }
            System.out.println("Conectado ao tracker!");
            System.out.println("Peers conhecidos: " + neighbors.size()); 
            System.out.println("Blocos iniciais: " + ownedBlocks.size()); 
            }
            else {
                System.out.println("Erro na comunicação com o tracker. Código " + responseCode);
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startServer(){
    new Thread(() -> {
        try (ServerSocket server = new ServerSocket(this.port)) {
            while (true) {
                Socket socket = server.accept();
                new Thread(() -> handleClient(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }).start();
}
    
}
