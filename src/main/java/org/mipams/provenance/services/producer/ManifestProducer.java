package org.mipams.provenance.services.producer;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.mipams.jumbf.entities.JumbfBox;
import org.mipams.jumbf.util.CoreUtils;
import org.mipams.jumbf.util.MipamsException;
import org.mipams.jumbf.entities.JumbfBoxBuilder;
import org.mipams.provenance.entities.ProvenanceMetadata;
import org.mipams.provenance.entities.requests.ProducerRequest;
import org.mipams.provenance.services.ManifestDiscovery;
import org.mipams.provenance.services.content_types.ManifestContentType;
import org.mipams.provenance.utils.ProvenanceUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ManifestProducer {

        private static final Logger logger = Logger.getLogger(ManifestProducer.class.getName());

        @Autowired
        ClaimProducer claimProducer;

        @Autowired
        AssertionStoreProducer assertionStoreProducer;

        @Autowired
        ClaimSignatureProducer claimSignatureProducer;

        @Autowired
        ManifestDiscovery manifestDiscovery;

        public final JumbfBox produceManifestJumbfBox(ProducerRequest producerRequest) throws MipamsException {

                ManifestContentType manifestContentType = manifestDiscovery
                                .discoverManifestType(producerRequest.getAssertionList());

                JumbfBoxBuilder manifestJumbfBoxBuilder = new JumbfBoxBuilder(manifestContentType);

                manifestJumbfBoxBuilder.setJumbfBoxAsRequestable();

                final String manifestId = ProvenanceUtils.issueNewManifestId();
                manifestJumbfBoxBuilder.setLabel(manifestId);
                logger.log(Level.FINE, String.format("Issue new manifest Id %s", manifestId));

                String manifestDirectory = CoreUtils.createSubdirectory(
                                CoreUtils.getParentDirectory(producerRequest.getAssetUrl()), manifestId);

                ProvenanceMetadata manifestMetadata = new ProvenanceMetadata();
                manifestMetadata.setParentDirectory(manifestDirectory);

                JumbfBox assertionStoreJumbfBox = generateAssertionStoreJumbfBox(manifestContentType, producerRequest,
                                manifestMetadata);

                JumbfBox claimJumbfBox = claimProducer.produce(manifestId, producerRequest, assertionStoreJumbfBox,
                                manifestMetadata);

                JumbfBox claimSignatureJumbfBox = claimSignatureProducer.produce(producerRequest.getSigner(),
                                claimJumbfBox, manifestMetadata);

                manifestJumbfBoxBuilder.appendContentBox(claimJumbfBox);
                manifestJumbfBoxBuilder.appendContentBox(claimSignatureJumbfBox);
                manifestJumbfBoxBuilder.appendContentBox(assertionStoreJumbfBox);

                if (producerRequest.getCredentialsStoreJumbfBox() != null) {
                        manifestJumbfBoxBuilder.appendContentBox(producerRequest.getCredentialsStoreJumbfBox());
                }

                return manifestJumbfBoxBuilder.getResult();

        }

        private JumbfBox generateAssertionStoreJumbfBox(ManifestContentType manifestContentType,
                        ProducerRequest producerRequest, ProvenanceMetadata provenanceMetadata) throws MipamsException {

                JumbfBox assertionStoreJumbfBox;

                if (manifestDiscovery.isUpdateManifestRequest(manifestContentType)) {
                        assertionStoreJumbfBox = generateAssertionStoreForUpdateManifest(producerRequest,
                                        provenanceMetadata);
                } else {
                        assertionStoreJumbfBox = generateAssertionStoreForStandardManifest(producerRequest,
                                        provenanceMetadata);
                }

                return assertionStoreJumbfBox;
        }

        private JumbfBox generateAssertionStoreForUpdateManifest(ProducerRequest producerRequest,
                        ProvenanceMetadata provenanceMetadata)
                        throws MipamsException {
                return assertionStoreProducer.produce(producerRequest.getAssertionList(), provenanceMetadata);
        }

        private JumbfBox generateAssertionStoreForStandardManifest(ProducerRequest producerRequest,
                        ProvenanceMetadata provenanceMetadata)
                        throws MipamsException {

                JumbfBox assertionStoreJumbfBox = assertionStoreProducer.produce(producerRequest.getAssertionList(),
                                provenanceMetadata);

                String assetUrl = producerRequest.getAssetUrl();
                return assertionStoreProducer.getAssertionStoreWithContentBindingAssertion(assertionStoreJumbfBox,
                                assetUrl, provenanceMetadata);
        }

}
