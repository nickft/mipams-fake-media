package org.mipams.fake_media.services.consumer;

import org.mipams.jumbf.core.entities.JumbfBox;
import org.mipams.jumbf.core.services.CoreGeneratorService;
import org.mipams.jumbf.core.util.CoreUtils;
import org.mipams.jumbf.core.util.MipamsException;
import org.mipams.jumbf.core.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import org.mipams.fake_media.entities.Claim;
import org.mipams.fake_media.entities.ProvenanceErrorMessages;
import org.mipams.fake_media.entities.UriReference;
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

        public void verifyManifestUriReference(JumbfBox manifestJumbfBox, UriReference manifestUriReference)
                        throws MipamsException {

                if (manifestUriReference.getAlgorithm() != UriReference.SUPPORTED_HASH_ALGORITHM) {
                        throw new MipamsException(ProvenanceErrorMessages.UNSUPPORTED_HASH_METHOD);
                }

                byte[] computedDigest = getManifestSha256Digest(manifestJumbfBox);

                if (!Arrays.equals(manifestUriReference.getDigest(), computedDigest)) {
                        throw new MipamsException(
                                        String.format(ProvenanceErrorMessages.INGREDIENT_REFERENCE_DIGEST_MISMATCH,
                                                        manifestUriReference.getUri()));
                }

        }

        public byte[] getManifestSha256Digest(JumbfBox manifestJumbfBox) throws MipamsException {
                String tempFile = CoreUtils.randomStringGenerator();
                String tempFilePath = CoreUtils.getFullPath(properties.getFileDirectory(), tempFile);
                try {
                        coreGeneratorService.generateJumbfMetadataToFile(List.of(manifestJumbfBox), tempFilePath);
                        return ProvenanceUtils.computeSha256DigestOfFileContents(tempFilePath);
                } finally {
                        CoreUtils.deleteFile(tempFilePath);
                }
        }
}
