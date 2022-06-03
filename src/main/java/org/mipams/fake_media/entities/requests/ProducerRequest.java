package org.mipams.fake_media.entities.requests;

import java.util.List;

import org.mipams.jumbf.core.entities.JumbfBox;
import org.mipams.fake_media.entities.ClaimGenerator;
import org.mipams.fake_media.entities.ProvenanceSigner;
import org.mipams.fake_media.entities.assertions.Assertion;
import org.mipams.fake_media.entities.assertions.RedactableAssertion;

import lombok.Getter;
import lombok.Setter;

public class ProducerRequest {
    private @Getter @Setter String assetUrl;
    private @Getter @Setter ProvenanceSigner signer;
    private @Getter @Setter String ivHexEncoded;
    private @Getter @Setter ClaimGenerator claimGenerator;
    private @Getter @Setter List<Assertion> assertionList;
    private @Getter @Setter List<RedactableAssertion> redactedAssertionList;
    private @Getter @Setter JumbfBox credentialsStoreJumbfBox;
}
