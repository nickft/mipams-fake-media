package org.mipams.fake_media.entities;

import lombok.Getter;
import lombok.Setter;

public class ClaimSignature implements ProvenanceEntity {
    private @Getter @Setter String algorithm;
    private @Getter @Setter byte[] certificate;
    private @Getter @Setter String date;
    private @Getter @Setter byte[] signature;
    private @Getter @Setter byte[] publicKey;
}
