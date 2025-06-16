package br.uerj.graduacao.torrent;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import br.uerj.graduacao.utils.Constants;

public class TorrentGenerator {
    private static final Logger LOGGER = Logger.getLogger(TorrentGenerator.class.getName());

    public static Torrent generateTorrent(String serverAddress, String filePath) {
        File file = new File(filePath);

        if (file.exists()) {
            try (InputStream is = Files.newInputStream(Paths.get(filePath))) {
                long fileSizeBytes = file.length();
                long totalBlocks = (long) Math.ceil((double) fileSizeBytes / Constants.BLOCK_SIZE_BYTES);

                // checksum
                String checksum = DigestUtils.md5Hex(is);

                LOGGER.log(Level.INFO, "Generator criado para: {0}", filePath);
                LOGGER.log(Level.INFO, "Tamanho: {0} bytes, Blocos: {1}",
                        new Object[] { fileSizeBytes, totalBlocks });

                return new Torrent(serverAddress, file.getName(), totalBlocks, filePath, fileSizeBytes,
                        checksum);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erro ao calcular checksum", e);
                return null;
            }

        } else {
            LOGGER.log(Level.SEVERE, "Arquivo nao encontrado: {0}", filePath);
            return null;
            // If possible -> treat
            // If not -> Raise Exception
        }
    }
}