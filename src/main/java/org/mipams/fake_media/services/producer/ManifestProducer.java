package org.mipams.fake_media.services.producer;

import org.mipams.jumbf.core.entities.JumbfBox;
import org.mipams.jumbf.core.util.CoreUtils;
import org.mipams.jumbf.core.util.MipamsException;
import org.mipams.jumbf.core.util.Properties;
import org.mipams.jumbf.core.entities.JumbfBoxBuilder;
import org.mipams.fake_media.entities.ProvenanceMetadata;
import org.mipams.fake_media.entities.requests.ProducerRequest;
import org.mipams.fake_media.services.ManifestDiscovery;
import org.mipams.fake_media.services.content_types.ManifestContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ManifestProducer {

        private static final Logger logger = LoggerFactory.getLogger(ManifestProducer.class);

        @Autowired
        ClaimProducer claimProducer;

        @Autowired
        AssertionStoreProducer assertionStoreProducer;

        @Autowired
        ClaimSignatureProducer claimSignatureProducer;

        @Autowired
        ManifestDiscovery manifestDiscovery;

        @Autowired
        Properties properties;

        public final JumbfBox produceManifestJumbfBox(ProducerRequest producerRequest) throws MipamsException {

                JumbfBoxBuilder manifestJumbfBoxBuilder = new JumbfBoxBuilder();
                manifestJumbfBoxBuilder.setJumbfBoxAsRequestable();

                final String manifestId = CoreUtils.randomStringGenerator();
                manifestJumbfBoxBuilder.setLabel(manifestId);
                logger.debug(String.format("Issue new manifest Id %s", manifestId));

                String manifestDirectory = CoreUtils.createSubdirectory(properties.getFileDirectory(),
                                manifestId);

                ProvenanceMetadata manifestMetadata = new ProvenanceMetadata();
                manifestMetadata.setParentDirectory(manifestDirectory);

                ManifestContentType manifestContentType = manifestDiscovery
                                .discoverManifestType(producerRequest.getAssertionList());
                manifestJumbfBoxBuilder.setContentType(manifestContentType);

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
                assertionStoreProducer.addContentBindingAssertion(assertionStoreJumbfBox, assetUrl, provenanceMetadata);

                return assertionStoreJumbfBox;
        }

}
