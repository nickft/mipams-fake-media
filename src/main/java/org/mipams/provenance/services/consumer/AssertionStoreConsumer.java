package org.mipams.provenance.services.consumer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;

import org.mipams.jumbf.entities.BmffBox;
import org.mipams.jumbf.entities.CborBox;
import org.mipams.jumbf.entities.JumbfBox;
import org.mipams.jumbf.services.CoreGeneratorService;
import org.mipams.jumbf.util.CoreUtils;
import org.mipams.jumbf.util.MipamsException;
import org.mipams.provenance.entities.HashedUriReference;
import org.mipams.provenance.entities.ProvenanceErrorMessages;
import org.mipams.provenance.entities.assertions.BindingAssertion;
import org.mipams.provenance.services.AssertionFactory;
import org.mipams.provenance.services.content_types.AssertionStoreContentType;
import org.mipams.provenance.services.producer.AssertionRefProducer;
import org.mipams.provenance.utils.ProvenanceUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AssertionStoreConsumer {

    private static final Logger logger = Logger.getLogger(AssertionStoreConsumer.class.getName());

    @Autowired
    AssertionStoreContentType assertionStoreContentType;

    @Autowired
    AssertionFactory assertionFactory;

    @Autowired
    AssertionRefProducer assertionRefProducer;

    @Autowired
    CoreGeneratorService coreGeneratorService;

    public void validateContentBinding(JumbfBox manifestJumbfBox, String assetUrl) throws MipamsException {

        JumbfBox assertionStoreJumbfBox = getAssertionStoreJumbfBox(manifestJumbfBox);

        JumbfBox contentBindingAssertionJumbfBox = getContentBindingAssertionJumbfBox(assertionStoreJumbfBox);

        BindingAssertion assertion = deserializeBindingAssertion(contentBindingAssertionJumbfBox);

        if (!assertion.getAlgorithm().equals("SHA-256")) {
            throw new MipamsException(ProvenanceErrorMessages.UNSUPPORTED_HASH_METHOD);
        }

        byte[] digest = ProvenanceUtils.computeSha256DigestOfFileContents(assetUrl);
        logger.log(Level.FINE, String.format("Checking hash digest for asset: %s", assetUrl));

        if (!Arrays.equals(assertion.getDigest(), digest)) {

            logger.info(CoreUtils.convertByteArrayToHex(digest));
            logger.info(CoreUtils.convertByteArrayToHex(assertion.getDigest()));

            throw new MipamsException(ProvenanceErrorMessages.CONTENT_BINDING_MISMATCH);
        }
    }

    private JumbfBox getAssertionStoreJumbfBox(JumbfBox manifestJumbfBox) throws MipamsException {
        for (BmffBox contentBox : manifestJumbfBox.getContentBoxList()) {

            JumbfBox jumbfBox = (JumbfBox) contentBox;

            if (assertionStoreContentType.getLabel().equals(jumbfBox.getDescriptionBox().getLabel())) {
                return jumbfBox;
            }
        }
        throw new MipamsException(ProvenanceErrorMessages.ASSERTION_STORE_NOT_FOUND);
    }

    private JumbfBox getContentBindingAssertionJumbfBox(JumbfBox assertionStoreJumbfBox) throws MipamsException {
        for (BmffBox contentBox : assertionStoreJumbfBox.getContentBoxList()) {

            JumbfBox jumbfBox = (JumbfBox) contentBox;

            if (assertionFactory.labelReferencesContentBindingAssertion(jumbfBox.getDescriptionBox().getLabel())) {
                return jumbfBox;
            }
        }
        throw new MipamsException(ProvenanceErrorMessages.CONTENT_BINDING_ASSERTION_NOT_FOUND);
    }

    private BindingAssertion deserializeBindingAssertion(JumbfBox contentBindingAssertionJumbfBox)
            throws MipamsException {

        CborBox contentBindingCborBox = (CborBox) contentBindingAssertionJumbfBox.getContentBoxList().get(0);
        ObjectMapper mapper = new CBORMapper();
        try {
            return mapper.readValue(new ByteArrayInputStream(contentBindingCborBox.getContent()),
                    BindingAssertion.class);
        } catch (IOException e) {
            throw new MipamsException(ProvenanceErrorMessages.CBOR_DESERIALIZE_ERROR, e);
        }
    }

    public void validateAssertionsIntegrity(String manifestId, List<HashedUriReference> assertionReferenceList,
            JumbfBox assertionStoreJumbfBox) throws MipamsException {

        List<HashedUriReference> computedUriReferenceList = assertionRefProducer
                .getAssertionReferenceListFromAssertionStore(manifestId, assertionStoreJumbfBox);

        Map<String, HashedUriReference> uriToReferenceMap = new HashMap<>();
        assertionReferenceList.forEach(assertionRef -> uriToReferenceMap.put(assertionRef.getUri(), assertionRef));

        for (HashedUriReference computedUriReference : computedUriReferenceList) {

            HashedUriReference claimedUriReference = uriToReferenceMap.get(computedUriReference.getUri());

            if (claimedUriReference == null) {
                throw new MipamsException(ProvenanceErrorMessages.UNREFERENCED_ASSERTION);
            }

            if (!claimedUriReference.getAlgorithm().equals(computedUriReference.getAlgorithm())) {
                throw new MipamsException(String.format(ProvenanceErrorMessages.ASSERTION_DIGEST_MISMATCH,
                        computedUriReference.getUri()));
            }

            if (!Arrays.equals(computedUriReference.getDigest(), claimedUriReference.getDigest())) {
                throw new MipamsException(String.format(ProvenanceErrorMessages.ASSERTION_DIGEST_MISMATCH,
                        computedUriReference.getUri()));
            }
        }
    }
}
