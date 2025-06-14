package br.uerj.graduacao;

import java.io.File;
// import java.io.FileInputStream;

public class TorrentGenerator {
    public long BLOCK_SIZE = 4096;
    public long NUMBER_OF_BLOCKS;
    public byte[] block_array;

    public String name;
    public String file_path;

    public TorrentGenerator (String file_path) {
        this.file_path = file_path;

        File file = new File(file_path);

        if (file.exists() && !file.isDirectory()) {
            long size = file.length();
            this.NUMBER_OF_BLOCKS = (size / this.BLOCK_SIZE);
            // Map or array with the indexes of the block (block_0, block_1, ...)
        }

        else {
            // If possible -> treat
            // If not -> Raise Exception
        }
    }
}
