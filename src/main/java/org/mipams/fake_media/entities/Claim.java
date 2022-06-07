package org.mipams.fake_media.entities;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class Claim implements ProvenanceEntity {

    private @Getter @Setter List<UriReference> assertionReferenceList;
    private @Getter @Setter String claimSignatureReference;
    private @Getter @Setter List<String> redactedAssertionsUriList;
    private @Getter @Setter List<String> encryptedAssertionUriList;
    private @Getter @Setter String claimGeneratorDescription;
}
