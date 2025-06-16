package br.uerj.graduacao;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TorrentGenerator {
    private static final Logger LOGGER = Logger.getLogger(TorrentGenerator.class.getName());
    private static TorrentGenerator instance;
    
    // public String name;
    private long numberOfBlocks;
    private String filePath;
    private long sizeOfFile;

    private TorrentGenerator(String filePath) {
        this.filePath = filePath;
        File file = new File(filePath);

        if (file.exists()) {
            this.sizeOfFile = file.length();
            this.numberOfBlocks = (long) Math.ceil((double) this.sizeOfFile / Constants.BLOCK_SIZE_BYTES);
            LOGGER.log(Level.INFO, "Generator criado para: {0}", this.filePath);
            LOGGER.log(Level.INFO, "Tamanho: {0} bytes, Blocos: {1}", new Object[]{this.sizeOfFile, this.numberOfBlocks});
        } else {
            LOGGER.log(Level.SEVERE, "Arquivo nao encontrado: {0}", filePath);
            // If possible -> treat
            // If not -> Raise Exception
        }
    }

    public static TorrentGenerator getInstance(String filePath) {
        if (instance == null) {
            instance = new TorrentGenerator(filePath);
        }
        return instance;
    }

    public long getSize() {
        return this.sizeOfFile;
    }

    public long getNumBlocks() {
        return this.numberOfBlocks;
    }
}