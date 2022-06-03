package org.mipams.fake_media.services.consumer;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;

import org.mipams.jumbf.core.entities.BmffBox;
import org.mipams.jumbf.core.entities.CborBox;
import org.mipams.jumbf.core.entities.JumbfBox;
import org.mipams.jumbf.core.services.CoreGeneratorService;
import org.mipams.jumbf.core.util.CoreUtils;
import org.mipams.jumbf.core.util.MipamsException;
import org.mipams.jumbf.core.util.Properties;
import org.mipams.fake_media.entities.ProvenanceErrorMessages;
import org.mipams.fake_media.entities.assertions.AssertionFactory;
import org.mipams.fake_media.entities.assertions.AssertionRef;
import org.mipams.fake_media.entities.assertions.BindingAssertion;
import org.mipams.fake_media.entities.requests.ConsumerRequest;
import org.mipams.fake_media.services.content_types.AssertionStoreContentType;
import org.mipams.fake_media.utils.ProvenanceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AssertionStoreConsumer {

    private static final Logger logger = LoggerFactory.getLogger(AssertionStoreConsumer.class);

    @Autowired
    AssertionStoreContentType assertionStoreContentType;

    @Autowired
    AssertionFactory assertionFactory;

    @Autowired
    CoreGeneratorService coreGeneratorService;

    @Autowired
    Properties properties;

    public void validateContentBinding(ConsumerRequest consumerRequest) throws MipamsException {
        JumbfBox manifestJumbfBox = consumerRequest.getManifestContentTypeJumbfBox();

        JumbfBox assertionStoreJumbfBox = getAssertionStoreJumbfBox(manifestJumbfBox);

        JumbfBox contentBindingAssertionJumbfBox = getContentBindingAssertionJumbfBox(assertionStoreJumbfBox);

        BindingAssertion assertion = deserializeBindingAssertion(contentBindingAssertionJumbfBox);

        if (!assertion.getAlgorithm().equals("SHA-256")) {
            throw new MipamsException(ProvenanceErrorMessages.UNSUPPORTED_HASH_METHOD);
        }

        byte[] digest = ProvenanceUtils.computeSha256DigestOfFileContents(consumerRequest.getAssetUrl());

        if (!Arrays.equals(assertion.getDigest(), digest)) {

            logger.info(DatatypeConverter.printHexBinary(digest));
            logger.info(DatatypeConverter.printHexBinary(assertion.getDigest()));

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

        String cborFilePath = contentBindingCborBox.getFileUrl();

        ObjectMapper mapper = new CBORMapper();
        try {
            return mapper.readValue(new File(cborFilePath), BindingAssertion.class);
        } catch (IOException e) {
            throw new MipamsException(ProvenanceErrorMessages.CBOR_DESERIALIZE_ERROR, e);
        }
    }

    public void validateAssertionsIntegrity(String manifestId, List<AssertionRef> assertionReferenceList,
            JumbfBox assertionStoreJumbfBox) throws MipamsException {

        Map<String, AssertionRef> uriToReferenceMap = new HashMap<>();

        assertionReferenceList.forEach(assertionRef -> uriToReferenceMap.put(assertionRef.getUri(), assertionRef));

        final String assertionStoreLabel = assertionStoreJumbfBox.getDescriptionBox().getLabel();

        for (BmffBox contentBox : assertionStoreJumbfBox.getContentBoxList()) {
            JumbfBox assertionJumbfBox = (JumbfBox) contentBox;

            verifyAssertionIntegrity(manifestId, assertionStoreLabel, assertionJumbfBox, uriToReferenceMap);
        }
    }

    private void verifyAssertionIntegrity(String manifestId, String assertionStoreLabel, JumbfBox assertionJumbfBox,
            Map<String, AssertionRef> uriToReferenceMap) throws MipamsException {

        String uri;
        AssertionRef assertionRef;
        String label = assertionJumbfBox.getDescriptionBox().getLabel();

        uri = ProvenanceUtils.getProvenanceJumbfURL(manifestId, assertionStoreLabel, label);

        assertionRef = uriToReferenceMap.get(uri);

        if (assertionRef == null) {
            throw new MipamsException(ProvenanceErrorMessages.UNREFERENCED_ASSERTION);
        }

        if (!assertionRef.getAlgorithm().equals("SHA-256")) {
            throw new MipamsException(ProvenanceErrorMessages.UNSUPPORTED_HASH_METHOD);
        }

        String tempFile = CoreUtils.randomStringGenerator();
        String tempFilePath = CoreUtils.getFullPath(properties.getFileDirectory(), tempFile);
        try {

            coreGeneratorService.generateJumbfMetadataToFile(List.of(assertionJumbfBox), tempFilePath);

            byte[] computedDigest = ProvenanceUtils.computeSha256DigestOfFileContents(tempFilePath);

            if (!Arrays.equals(assertionRef.getDigest(), computedDigest)) {
                throw new MipamsException(String.format(ProvenanceErrorMessages.ASSERTION_DIGEST_MISMATCH, label));
            }
        } finally {
            deleteFile(tempFilePath);
        }
    }

    private void deleteFile(String tempFilePath) {
        File f = new File(tempFilePath);
        if (f.exists()) {
            f.delete();
        }
    }
}
