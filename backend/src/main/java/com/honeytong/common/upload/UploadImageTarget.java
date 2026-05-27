package com.honeytong.common.upload;

public enum UploadImageTarget {
    PLACE("places"),
    PROFILE("profiles"),
    VISIT("visits");

    private final String folderName;

    UploadImageTarget(String folderName) {
        this.folderName = folderName;
    }

    public String folderName() {
        return folderName;
    }
}
