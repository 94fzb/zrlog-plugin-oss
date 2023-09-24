package com.zrlog.plugin.oss.entry;

import java.io.File;

public class UploadFile {

    private File file;
    private String fileKey;

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getFileKey() {
        return fileKey;
    }

    public void setFileKey(String fileKey) {
        this.fileKey = fileKey;
    }
}
