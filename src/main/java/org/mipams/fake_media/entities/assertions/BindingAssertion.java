package org.mipams.fake_media.entities.assertions;

public class BindingAssertion implements NonRedactableAssertion {
    private String algorithm;
    private String padding;
    private byte[] digest;
    private String description;

    public BindingAssertion() {
    }

    public BindingAssertion(String algorithm, String padding, byte[] digest, String description) {
        setAlgorithm(algorithm);
        setPadding(padding);
        setDigest(digest);
        setDescription(description);
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getPadding() {
        return padding;
    }

    public void setPadding(String padding) {
        this.padding = padding;
    }

    public byte[] getDigest() {
        return digest;
    }

    public void setDigest(byte[] digest) {
        this.digest = digest;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
