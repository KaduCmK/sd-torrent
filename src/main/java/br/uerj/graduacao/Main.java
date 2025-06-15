package br.uerj.graduacao;

public class Main {
    public static void main(String[] args) {
        TorrentGenerator torrent = new TorrentGenerator("arq1");

        FileManager peer1 = new FileManager("arq1", torrent.size(), torrent.numBlocks());
        
        FileManager peer2 = new FileManager("arq2", torrent.size(), torrent.numBlocks());

        for (long i=0; i<torrent.numBlocks(); i++) {
            BlockModel block = new BlockModel(i);

            block = peer1.readBlock(i);

            peer2.writeBlock(block);
        }
    }
}
