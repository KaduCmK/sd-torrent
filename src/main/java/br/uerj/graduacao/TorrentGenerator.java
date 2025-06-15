package br.uerj.graduacao;

import java.io.File;
// import java.io.FileInputStream;
// import java.io.RandomAccessFile;

// import br.uerj.graduacao.BlockModel;


public class TorrentGenerator {
    public long BLOCK_SIZE = 4096;
    public long NUMBER_OF_BLOCKS;
    
    // public String name;
    public String file_path;
    public long size_of_file;

    public TorrentGenerator (String file_path) {
        this.file_path = file_path;

        File file = new File(file_path);

        if (file.exists()) {
            this.size_of_file = file.length();
            this.NUMBER_OF_BLOCKS = (this.size_of_file / this.BLOCK_SIZE);
            
        }

        else {
            System.out.println("File Not Found!");
            // If possible -> treat
            // If not -> Raise Exception
        }
    }

    public long size() {
        return this.size_of_file;
    }

    public long numBlocks() {
        return this.NUMBER_OF_BLOCKS;
    }
}
