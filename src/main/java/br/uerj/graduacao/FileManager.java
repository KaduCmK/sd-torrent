package br.uerj.graduacao;

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

    public void writeBlock(BlockModel block, long currentBlock, long totalBlocks) {
        try {
            this.manager.seek(block.pointer());
            this.manager.write(block.getData());
            updateProgressBar(currentBlock, totalBlocks);
        } catch (IOException error) {
            String msg = String.format("Erro de I/O ao escrever bloco (ponteiro: %d) no arquivo %s", block.pointer(),
                    this.filePath);
            LOGGER.log(Level.SEVERE, msg, error);
        }
    }

    private void updateProgressBar(long current, long total) {
        int percent = (int) ((current * 100) / total);
        StringBuilder bar = new StringBuilder("[");

        int progressChars = (int) (percent / 2.0);
        for (int i = 0; i < 50; i++) {
            if (i < progressChars) {
                bar.append("=");
            } else {
                bar.append(" ");
            }
        }

        bar.append("] " + percent + "%");
        System.out.print("\r" + bar.toString());

        if (current == total) {
            System.out.println();
            LOGGER.log(Level.INFO, "Escrita de {0} blocos concluida em {1}", new Object[] { total, this.filePath });
        }
    }

    public BlockModel readBlock(long index) {
        BlockModel block = new BlockModel(index);

        try {
            byte[] data = new byte[Constants.BLOCK_SIZE_BYTES];
            manager.seek(block.pointer());
            manager.read(data);

            block.setData(data);
        } catch (IOException error) {
            String msg = String.format("Erro de I/O ao ler bloco %d do arquivo %s", index, this.filePath);
            LOGGER.log(Level.WARNING, msg, error);
        }

        return block;
    }
}