package br.uerj.graduacao.torrent;

public class Torrent {
    private String trackerAddress;
    private String fileName;
    private long totalBlocks;
    private String filePath;
    private long fileSizeBytes;
    private String checksum;

    public Torrent(String trackerAddress, String fileName, long totalBlocks, String filePath, long fileSizeBytes,
            String checksum) {
        this.trackerAddress = trackerAddress;
        this.fileName = fileName;
        this.totalBlocks = totalBlocks;
        this.filePath = filePath;
        this.fileSizeBytes = fileSizeBytes;
        this.checksum = checksum;
    }

    public String getTrackerAddress() {
        return this.trackerAddress;
    }

    public String getFileName() {
        return this.fileName;
    }

    public String getFilePath() {
        return this.filePath;
    }

    public long getSize() {
        return this.fileSizeBytes;
    }

    public long getNumBlocks() {
        return this.totalBlocks;
    }

    public String getChecksum() {
        return this.checksum;
    }
}
