package br.uerj.graduacao;

public class Main {
    public static void main(String[] args) {
        TorrentGenerator torrent = TorrentGenerator.getInstance("destiny.mp4");

        FileManager peer1 = new FileManager("destiny.mp4", torrent.getSize(), torrent.getNumBlocks());
        FileManager peer2 = new FileManager("destiny2.mp4", torrent.getSize(), torrent.getNumBlocks());

        for (long i = 0; i < torrent.getNumBlocks(); i++) {
            BlockModel block = new BlockModel(i);
            block = peer1.readBlock(i);
            peer2.writeBlock(block, i, torrent.getNumBlocks());
        }
    }
}
