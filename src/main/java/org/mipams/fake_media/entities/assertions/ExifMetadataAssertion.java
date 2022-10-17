package org.mipams.fake_media.entities.assertions;

public class ExifMetadataAssertion implements RedactableAssertion {
    private String exifMetadata;

    public ExifMetadataAssertion() {
    }

    public ExifMetadataAssertion(String exifMetadata) {
        setExifMetadata(exifMetadata);
    }

    public String getExifMetadata() {
        return exifMetadata;
    }

    public void setExifMetadata(String exifMetadata) {
        this.exifMetadata = exifMetadata;
    }
}
