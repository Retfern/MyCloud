package common.message;

import common.StatusFile;

public class InfoFileClass {

    private String path;
    private String fileName;
    private long size;
    private StatusFile status;

    public InfoFileClass (String path, String fileName, long size, StatusFile status) {
        this.path = path;
        this.fileName = fileName;
        this.size = size;
        this.status = status;
    }

    public long getSize() {
        return size;
    }

    public String getPath() {
        return path;
    }

    public String getFileName() {
        return fileName;
    }

    public StatusFile getStatus() {
        return status;
    }
}
