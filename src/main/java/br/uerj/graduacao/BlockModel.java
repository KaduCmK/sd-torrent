package br.uerj.graduacao;

public class BlockModel {
    private int BLOCK_INDEX;
    // private Byte block_data;

    public BlockModel (int index, Byte data) {
        this.BLOCK_INDEX = index;
        // this.block_data = data;
    };

    public String toString() {
        String name = "block_" + String.valueOf(this.BLOCK_INDEX);
        return name;
    }
}
