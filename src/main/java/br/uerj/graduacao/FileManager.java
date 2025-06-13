package br.uerj.graduacao;

import java.io.File;
import java.io.FileInputStream;

public class FileManager {
    public int NUMBER_OF_BLOCKS;
    public int BLOCK_SIZE;
    public byte[] block_array;

    public String name;
    public String file_path;

    public FileManager (String file_path) {
        this.file_path = file_path;


    }
}
