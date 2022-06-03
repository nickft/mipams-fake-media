package org.mipams.fake_media.services;

import java.util.ArrayList;
import java.util.List;

import org.mipams.jumbf.core.entities.BmffBox;
import org.mipams.jumbf.core.entities.JumbfBox;
import org.mipams.jumbf.core.util.MipamsException;
import org.mipams.fake_media.entities.Claim;
import org.mipams.fake_media.entities.ProvenanceErrorMessages;
import org.mipams.fake_media.entities.assertions.Assertion;
import org.mipams.fake_media.entities.assertions.AssertionFactory;
import org.mipams.fake_media.entities.requests.ConsumerRequest;
import org.mipams.fake_media.services.consumer.AssertionStoreConsumer;
import org.mipams.fake_media.services.consumer.ClaimSignatureConsumer;
import org.mipams.fake_media.services.content_types.AssertionStoreContentType;
import org.mipams.fake_media.services.content_types.ClaimContentType;
import org.mipams.fake_media.services.content_types.ClaimSignatureContentType;
import org.mipams.fake_media.services.content_types.ManifestContentType;
import org.mipams.fake_media.services.content_types.ProvenanceContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProvenanceConsumer {

    @Autowired
    ManifestDiscovery manifestDiscovery;

    @Autowired
    ClaimSignatureContentType claimSignatureContentType;

    @Autowired
    ClaimContentType claimContentType;

    @Autowired
    AssertionStoreConsumer assertionStoreConsumer;

    @Autowired
    AssertionStoreContentType assertionStoreContentType;

    @Autowired
    AssertionFactory assertionFactory;

    @Autowired
    ClaimSignatureConsumer claimSignatureConsumer;

    public List<Assertion> verifyAndConsumeManifestJumbfBox(ConsumerRequest consumerRequest) throws MipamsException {

        verifyIntegrityOfManifestJumbfBox(consumerRequest);

        List<Assertion> assertionList = new ArrayList<>();

        JumbfBox manifestJumbfBox = consumerRequest.getManifestContentTypeJumbfBox();

        JumbfBox assertionStoreJumbfBox = getProvenanceJumbfBox(manifestJumbfBox, assertionStoreContentType);

        for (BmffBox contentBox : assertionStoreJumbfBox.getContentBoxList()) {
            JumbfBox assertionJumbfBox = (JumbfBox) contentBox;

            Assertion assertion = assertionFactory.convertJumbfBoxToAssertion(assertionJumbfBox);
            if (assertion != null) {
                assertionList.add(assertion);
            }
        }

        return assertionList;
    }

    public void verifyIntegrityOfManifestJumbfBox(ConsumerRequest consumerRequest) throws MipamsException {

        JumbfBox manifestJumbfBox = consumerRequest.getManifestContentTypeJumbfBox();
        final String manifestId = manifestJumbfBox.getDescriptionBox().getLabel();

        ManifestContentType manifestContentType = manifestDiscovery.discoverManifestType(manifestJumbfBox);

        if (manifestDiscovery.isStandardManifestRequest(manifestContentType)) {
            assertionStoreConsumer.validateContentBinding(consumerRequest);
        }

        JumbfBox claimSignatureJumbfBox = getProvenanceJumbfBox(manifestJumbfBox, claimSignatureContentType);
        JumbfBox claimJumbfBox = getProvenanceJumbfBox(manifestJumbfBox, claimContentType);

        Claim claim = claimSignatureConsumer.validateClaimSignature(manifestId, claimJumbfBox, claimSignatureJumbfBox);

        JumbfBox assertionStoreJumbfBox = getProvenanceJumbfBox(manifestJumbfBox, assertionStoreContentType);
        assertionStoreConsumer.validateAssertionsIntegrity(manifestId, claim.getAssertionReferenceList(),
                assertionStoreJumbfBox);
    }

    private JumbfBox getProvenanceJumbfBox(JumbfBox manifestJumbfBox, ProvenanceContentType contentType)
            throws MipamsException {

        JumbfBox result = null;

        for (BmffBox contentBox : manifestJumbfBox.getContentBoxList()) {
            JumbfBox jumbfBox = (JumbfBox) contentBox;

            if (contentType.getContentTypeUuid().equals(jumbfBox.getDescriptionBox().getUuid())) {
                result = jumbfBox;
            }
        }

        if (result == null) {
            throw new MipamsException(
                    String.format(ProvenanceErrorMessages.MANIFEST_CONTENT_BOX_NOT_FOUND, contentType.getLabel()));
        }

        return result;
    }
}
