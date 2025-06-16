package br.uerj.graduacao.utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileManager {
    private static final Logger LOGGER = Logger.getLogger(FileManager.class.getName());

    public String name;
    public String filePath;
    public long numberOfBlocks;
    public long sizeOfFile;

    private RandomAccessFile manager;

    public FileManager(String path, long size, long numBlocks) {
        this.filePath = path;
        this.sizeOfFile = size;
        this.numberOfBlocks = numBlocks;

        try {
            File file = new File(this.filePath);

            if (!file.exists()) {
                LOGGER.info("Arquivo nao existe, criando: " + this.filePath);
                file.createNewFile();
            }

            this.manager = new RandomAccessFile(file, "rwd");

            if (this.manager.length() != this.sizeOfFile) {
                this.manager.setLength(this.sizeOfFile);
            }
        } catch (NullPointerException error) {
            LOGGER.log(Level.SEVERE, "Path do arquivo nulo.", error);
        } catch (IOException error) {
            String msg = String.format("Erro de I/O ao criar ou ajustar arquivo: %s", this.filePath);
            LOGGER.log(Level.SEVERE, msg, error);
        }
    }

    public void writeBlock(BlockModel block) {
        try {
            this.manager.seek(block.pointer());
            this.manager.write(block.getData());
        } catch (IOException error) {
            String msg = String.format("Erro de I/O ao escrever bloco (ponteiro: %d) no arquivo %s", block.pointer(),
                    this.filePath);
            LOGGER.log(Level.SEVERE, msg, error);
        }
    }

    public BlockModel readBlock(long index) {
        BlockModel block = new BlockModel(index);

        try {
            long offset = block.pointer();
            long bytesToRead = Math.min(Constants.BLOCK_SIZE_BYTES, this.sizeOfFile - offset);

            if (bytesToRead <= 0) {
                block.setData(new byte[0]);
                return block;
            }

            byte[] data = new byte[(int) bytesToRead];
            manager.seek(offset);
            manager.readFully(data);

            block.setData(data);
        } catch (IOException error) {
            String msg = String.format("Erro de I/O ao ler bloco %d do arquivo %s", index, this.filePath);
            LOGGER.log(Level.WARNING, msg, error);
        }

        return block;
    }
}