package org.mipams.fake_media.entities.requests;

import java.util.List;

import org.mipams.jumbf.core.entities.JumbfBox;
import org.mipams.fake_media.entities.ClaimGenerator;
import org.mipams.fake_media.entities.ProvenanceSigner;

import lombok.Getter;
import lombok.Setter;

public class ProducerRequest {
    private @Getter @Setter String assetUrl;
    private @Getter @Setter JumbfBox manifestStoreJumbfBox;
    private @Getter @Setter List<JumbfBox> componentManifestJumbfBoxList;
    private @Getter @Setter ProvenanceSigner signer;
    private @Getter @Setter ClaimGenerator claimGenerator;
    private @Getter @Setter List<JumbfBox> assertionList;
    private @Getter @Setter List<String> redactedAssertionUriList;
    private @Getter @Setter JumbfBox credentialsStoreJumbfBox;
}
