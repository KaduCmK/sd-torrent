package br.uerj.graduacao;

public class BlockModel {
    private long BLOCK_INDEX;
    private long BLOCK_OFFSET;

    public BlockModel (long index) {
        this.BLOCK_INDEX = index;
        this.BLOCK_OFFSET = index*4096;
    };

    public long Pointer () {
        return this.BLOCK_OFFSET;
    }

    public String toString() {
        String name = "block_" + String.valueOf(this.BLOCK_INDEX);
        return name;
    }
}
