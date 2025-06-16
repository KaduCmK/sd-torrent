package br.uerj.graduacao.utils;

import java.io.Serializable;

public class BlockModel implements Serializable {
    private long BLOCK_INDEX;
    private long BLOCK_OFFSET;
    private byte[] data;

    public BlockModel (long index) {
        this.BLOCK_INDEX = index;
        this.BLOCK_OFFSET = index*Constants.BLOCK_SIZE_BYTES;
    };
    
    public void setData(byte[] data) {
        this.data = data;
    }
    
    public long pointer () {
        return this.BLOCK_OFFSET;
    }

    public long getBlockIndex() {
        return this.BLOCK_INDEX;
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
