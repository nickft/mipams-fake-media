package org.mipams.fake_media.entities;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

import lombok.Getter;
import lombok.Setter;

public class ProvenanceSigner {
    private @Getter @Setter KeyPair signingCredentials; // signer.getSigningCertificate().getSubjectX500Principal()
    private @Getter @Setter String signingScheme;
    private @Getter @Setter X509Certificate signingCertificate;
}
