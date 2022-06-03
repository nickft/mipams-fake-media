package org.mipams.fake_media.entities;

import java.security.KeyPair;
import java.security.cert.Certificate;

import javax.crypto.SecretKey;

import lombok.Getter;
import lombok.Setter;

public class ProvenanceSigner {
    private @Getter @Setter KeyPair signingCredentials;
    private @Getter @Setter String signingScheme;
    private @Getter @Setter Certificate signingCertificate;
    private @Getter @Setter SecretKey encryptionKey;
    private @Getter @Setter String encryptionScheme;
}
