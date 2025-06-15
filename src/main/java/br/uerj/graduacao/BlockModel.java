package br.uerj.graduacao;

public class BlockModel {
    private long BLOCK_INDEX;
    private long BLOCK_OFFSET;
    private byte[] data;

    public BlockModel (long index) {
        this.BLOCK_INDEX = index;
        this.BLOCK_OFFSET = index*4096;
    };
    
    public long pointer () {
        return this.BLOCK_OFFSET;
    }
    
    public void setData(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        // check if is not empty
        return this.data;
    }

    public String toString() {
        String name = "block_" + String.valueOf(this.BLOCK_INDEX);
        return name;
    }
}
