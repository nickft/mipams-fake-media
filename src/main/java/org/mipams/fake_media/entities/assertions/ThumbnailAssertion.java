package org.mipams.fake_media.entities.assertions;

public class ThumbnailAssertion implements NonRedactableAssertion {
    private String fileName;
    private String mediaType;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }
}
