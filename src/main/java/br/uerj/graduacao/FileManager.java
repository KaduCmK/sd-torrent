package br.uerj.graduacao;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
// import java.io.FileNotFoundException;


public class FileManager {
    public long BLOCK_SIZE = 4096;
    public long NUMBER_OF_BLOCKS;
    
    public String name;
    public String file_path;
    public long size_of_file;

    private RandomAccessFile manager;

    public FileManager (String path, long size, long num_blocks) {
        // this.name = name;
        this.file_path = path;
        this.size_of_file = size;
        this.NUMBER_OF_BLOCKS = num_blocks;

        try{
            File file = new File(this.file_path);

            if (!file.exists()) {
                System.out.println(file + "doesnt exit");
                file.createNewFile();    
            }
            
            this.manager = new RandomAccessFile(file, "rwd");
            
            if (this.manager.length() != this.size_of_file) {manager.setLength(this.size_of_file);}
            // checa o tamanho do arquivo           
        }

        catch(NullPointerException error) {
            // file_path is Null, return exception to the User
        }
        
        catch (IOException error) {
            // error while ajusting file size
        }
    }

    public void writeBlock(BlockModel block) {
        try {
            this.manager.seek(block.pointer());
            this.manager.write(block.getData());
        } 
        
        catch (IOException error) {
            System.out.println("An error was found while writing in the file");
            // position not found in file
            // or error when handling file
        }
    }

    public BlockModel readBlock(long index) {
        BlockModel block = new BlockModel(index);

        try {
            byte[] data = new byte[4096];
            manager.seek(block.pointer());
            manager.read(data);

            block.setData(data);
        }
        
        catch (IOException error) {
            // handle
            System.out.println("An error was found while reading the file");
        }

        return block;
    }
}
