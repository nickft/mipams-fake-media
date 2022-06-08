package org.mipams.fake_media.services.consumer;

import org.mipams.jumbf.core.entities.JumbfBox;
import org.mipams.jumbf.core.services.CoreGeneratorService;
import org.mipams.jumbf.core.util.MipamsException;
import org.mipams.jumbf.core.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mipams.fake_media.entities.Claim;
import org.mipams.fake_media.services.ManifestDiscovery;
import org.mipams.fake_media.services.content_types.AssertionStoreContentType;
import org.mipams.fake_media.services.content_types.ClaimContentType;
import org.mipams.fake_media.services.content_types.ClaimSignatureContentType;
import org.mipams.fake_media.utils.ProvenanceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ManifestConsumer {

        private static final Logger logger = LoggerFactory.getLogger(ManifestConsumer.class);

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
        ClaimSignatureConsumer claimSignatureConsumer;

        @Autowired
        CoreGeneratorService coreGeneratorService;

        @Autowired
        Properties properties;

        public void verifyManifestIntegrityAndContentBinding(JumbfBox manifestJumbfBox, String assetUrl)
                        throws MipamsException {

                verifyManifestIntegrity(manifestJumbfBox);

                logger.debug("Verifying integrity of manifest with id "
                                + manifestJumbfBox.getDescriptionBox().getLabel() + " with asset " + assetUrl);

                assertionStoreConsumer.validateContentBinding(manifestJumbfBox, assetUrl);
        }

        public void verifyManifestIntegrity(JumbfBox manifestJumbfBox) throws MipamsException {

                final String manifestId = manifestJumbfBox.getDescriptionBox().getLabel();

                JumbfBox claimSignatureJumbfBox = ProvenanceUtils.getProvenanceJumbfBox(manifestJumbfBox,
                                claimSignatureContentType);
                JumbfBox claimJumbfBox = ProvenanceUtils.getProvenanceJumbfBox(manifestJumbfBox, claimContentType);

                Claim claim = claimSignatureConsumer.validateClaimSignature(manifestId, claimJumbfBox,
                                claimSignatureJumbfBox);

                JumbfBox assertionStoreJumbfBox = ProvenanceUtils.getProvenanceJumbfBox(manifestJumbfBox,
                                assertionStoreContentType);
                assertionStoreConsumer.validateAssertionsIntegrity(manifestId, claim.getAssertionReferenceList(),
                                assertionStoreJumbfBox);
        }
}
