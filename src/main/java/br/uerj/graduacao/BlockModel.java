package br.uerj.graduacao;

public class BlockModel {
    private long BLOCK_INDEX;
    private long BLOCK_OFFSET;
    private byte[] data;

    public BlockModel (long index, byte[] data) {
        this.BLOCK_INDEX = index;
        this.BLOCK_OFFSET = index*4096;

        this.data = data;
    };

    public long Pointer () {
        return this.BLOCK_OFFSET;
    }

    public byte[] getData() {
        return this.data;
    }

    public String toString() {
        String name = "block_" + String.valueOf(this.BLOCK_INDEX);
        return name;
    }
}
