package org.mipams.provenance.services.consumer;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.mipams.jumbf.entities.JumbfBox;
import org.mipams.jumbf.services.CoreGeneratorService;
import org.mipams.jumbf.util.MipamsException;
import org.mipams.provenance.entities.Claim;
import org.mipams.provenance.services.ManifestDiscovery;
import org.mipams.provenance.services.content_types.AssertionStoreContentType;
import org.mipams.provenance.services.content_types.ClaimContentType;
import org.mipams.provenance.services.content_types.ClaimSignatureContentType;
import org.mipams.provenance.utils.ProvenanceUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ManifestConsumer {

        private static final Logger logger = Logger.getLogger(AssertionStoreConsumer.class.getName());

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

        public void verifyManifestIntegrityAndContentBinding(JumbfBox manifestJumbfBox, String assetUrl)
                        throws MipamsException {

                verifyManifestIntegrity(manifestJumbfBox);

                logger.log(Level.FINE, "Verifying integrity of manifest with id "
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
