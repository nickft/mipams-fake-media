package org.mipams.fake_media.entities;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

public class ProvenanceSigner {
    private KeyPair signingCredentials;
    private String signingScheme;
    private X509Certificate signingCertificate;

    public KeyPair getSigningCredentials() {
        return signingCredentials;
    }

    public void setSigningCredentials(KeyPair signingCredentials) {
        this.signingCredentials = signingCredentials;
    }

    public String getSigningScheme() {
        return signingScheme;
    }

    public void setSigningScheme(String signingScheme) {
        this.signingScheme = signingScheme;
    }

    public X509Certificate getSigningCertificate() {
        return signingCertificate;
    }

    public void setSigningCertificate(X509Certificate signingCertificate) {
        this.signingCertificate = signingCertificate;
    }

}
