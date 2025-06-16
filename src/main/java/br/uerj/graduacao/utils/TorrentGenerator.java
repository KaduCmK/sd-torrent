package br.uerj.graduacao.utils;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import br.uerj.graduacao.Constants;

public class TorrentGenerator {
    private static final Logger LOGGER = Logger.getLogger(TorrentGenerator.class.getName());
    private static TorrentGenerator instance;
    
    // public String name;
    private long numberOfBlocks;
    private String filePath;
    private long fileSizeBytes;
    private String checksum;

    private TorrentGenerator(String filePath) {
        this.filePath = filePath;
        File file = new File(filePath);

        if (file.exists()) {
            this.fileSizeBytes = file.length();
            this.numberOfBlocks = (long) Math.ceil((double) this.fileSizeBytes / Constants.BLOCK_SIZE_BYTES);

            // checksum
            try(InputStream is = Files.newInputStream(Paths.get(filePath))) {
                this.checksum = DigestUtils.md5Hex(is);
            }
            catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erro ao calcular checksum", e);
            }

            LOGGER.log(Level.INFO, "Generator criado para: {0}", this.filePath);
            LOGGER.log(Level.INFO, "Tamanho: {0} bytes, Blocos: {1}", new Object[]{this.fileSizeBytes, this.numberOfBlocks});
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
        return this.fileSizeBytes;
    }

    public long getNumBlocks() {
        return this.numberOfBlocks;
    }

    public String getChecksum() {
        return this.checksum;
    }
}