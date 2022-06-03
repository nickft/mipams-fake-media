package org.mipams.fake_media.services;

import org.mipams.jumbf.core.entities.JumbfBox;
import org.mipams.jumbf.core.util.CoreUtils;
import org.mipams.jumbf.core.util.MipamsException;
import org.mipams.jumbf.core.entities.JumbfBoxBuilder;
import org.mipams.fake_media.entities.requests.ProducerRequest;
import org.mipams.fake_media.services.content_types.ManifestContentType;
import org.mipams.fake_media.services.producer.AssertionStoreProducer;
import org.mipams.fake_media.services.producer.ClaimProducer;
import org.mipams.fake_media.services.producer.ClaimSignatureProducer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProvenanceProducer {

        private static final Logger logger = LoggerFactory.getLogger(ProvenanceProducer.class);

        @Autowired
        ClaimProducer claimProducer;

        @Autowired
        AssertionStoreProducer assertionStoreProducer;

        @Autowired
        ClaimSignatureProducer claimSignatureProducer;

        @Autowired
        ManifestDiscovery manifestDiscovery;

        public final JumbfBox produceManifestJumbfBox(ProducerRequest producerRequest) throws MipamsException {

                JumbfBoxBuilder manifestJumbfBoxBuilder = new JumbfBoxBuilder();
                manifestJumbfBoxBuilder.setJumbfBoxAsRequestable();

                final String manifestId = CoreUtils.randomStringGenerator();
                manifestJumbfBoxBuilder.setLabel(manifestId);
                logger.debug(String.format("Issue new manifest Id %s", manifestId));

                ManifestContentType manifestContentType = manifestDiscovery
                                .discoverManifestType(producerRequest.getAssertionList());
                manifestJumbfBoxBuilder.setContentType(manifestContentType);

                JumbfBox assertionStoreJumbfBox = generateAssertionStoreJumbfBox(manifestContentType, producerRequest);

                JumbfBox claimJumbfBox = claimProducer.produce(manifestId, producerRequest, assertionStoreJumbfBox);

                JumbfBox claimSignatureJumbfBox = claimSignatureProducer.produce(producerRequest.getSigner(),
                                claimJumbfBox);

                manifestJumbfBoxBuilder.appendContentBox(claimJumbfBox);
                manifestJumbfBoxBuilder.appendContentBox(claimSignatureJumbfBox);
                manifestJumbfBoxBuilder.appendContentBox(assertionStoreJumbfBox);

                if (producerRequest.getCredentialsStoreJumbfBox() != null) {
                        manifestJumbfBoxBuilder.appendContentBox(producerRequest.getCredentialsStoreJumbfBox());
                }

                return manifestJumbfBoxBuilder.getResult();

        }

        private JumbfBox generateAssertionStoreJumbfBox(ManifestContentType manifestContentType,
                        ProducerRequest producerRequest) throws MipamsException {

                JumbfBox assertionStoreJumbfBox;

                if (manifestDiscovery.isUpdateManifestRequest(manifestContentType)) {
                        assertionStoreJumbfBox = generateAssertionStoreForUpdateManifest(producerRequest);
                } else {
                        assertionStoreJumbfBox = generateAssertionStoreForStandardManifest(producerRequest);
                }

                return assertionStoreJumbfBox;
        }

        private JumbfBox generateAssertionStoreForUpdateManifest(ProducerRequest producerRequest)
                        throws MipamsException {
                return assertionStoreProducer.produce(producerRequest);
        }

        private JumbfBox generateAssertionStoreForStandardManifest(ProducerRequest producerRequest)
                        throws MipamsException {
                JumbfBox assertionStoreJumbfBox = assertionStoreProducer.produce(producerRequest);

                String assetUrl = producerRequest.getAssetUrl();
                assertionStoreProducer.addContentBindingAssertion(assertionStoreJumbfBox, assetUrl);

                return assertionStoreJumbfBox;
        }

}
