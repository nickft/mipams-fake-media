package org.mipams.provenance.entities;

public class HashedUriReference {

    public final static String SUPPORTED_HASH_ALGORITHM = "SHA-256";

    public HashedUriReference() {

    }

    public HashedUriReference(byte[] digest, String uri, String algorithm) {
        setDigest(digest);
        setUri(uri);
        setAlgorithm(algorithm);
    }

    private byte[] digest;
    private String uri;
    private String algorithm;

    public byte[] getDigest() {
        return digest;
    }

    public void setDigest(byte[] digest) {
        this.digest = digest;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

}