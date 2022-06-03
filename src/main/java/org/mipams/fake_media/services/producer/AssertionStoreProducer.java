package org.mipams.fake_media.services.producer;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.mipams.jumbf.core.entities.BinaryDataBox;
import org.mipams.jumbf.core.entities.JumbfBox;
import org.mipams.jumbf.core.entities.JumbfBoxBuilder;
import org.mipams.jumbf.core.services.boxes.JumbfBoxService;
import org.mipams.jumbf.core.util.CoreUtils;
import org.mipams.jumbf.core.util.MipamsException;
import org.mipams.jumbf.core.util.Properties;
import org.mipams.jumbf.crypto.entities.CryptoException;
import org.mipams.jumbf.crypto.entities.request.CryptoRequest;
import org.mipams.jumbf.crypto.services.CryptoService;
import org.mipams.jumbf.privacy_security.entities.ProtectionDescriptionBox;
import org.mipams.jumbf.privacy_security.services.content_types.ProtectionContentType;
import org.mipams.fake_media.entities.ProvenanceErrorMessages;
import org.mipams.fake_media.entities.ProvenanceSigner;
import org.mipams.fake_media.entities.assertions.Assertion;
import org.mipams.fake_media.entities.assertions.AssertionFactory;
import org.mipams.fake_media.entities.assertions.BindingAssertion;
import org.mipams.fake_media.entities.assertions.RedactableAssertion;
import org.mipams.fake_media.entities.requests.ProducerRequest;
import org.mipams.fake_media.services.content_types.AssertionStoreContentType;
import org.mipams.fake_media.utils.ProvenanceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AssertionStoreProducer {

    @Autowired
    AssertionStoreContentType assertionStoreContentType;

    @Autowired
    Properties properties;

    @Autowired
    AssertionFactory assertionFactory;

    @Autowired
    JumbfBoxService jumbfBoxService;

    @Autowired
    ProtectionContentType protectionContentType;

    @Autowired
    CryptoService cryptoService;

    public JumbfBox produce(ProducerRequest producerRequest) throws MipamsException {

        List<JumbfBox> deterministicAssertionJumbfBoxList = generateAssertionJumbfBoxes(producerRequest);

        ensureLabelUniquenessInAssertionStore(deterministicAssertionJumbfBoxList);

        List<JumbfBox> assertionJumbfBoxList = addRandomnessToAssertions(deterministicAssertionJumbfBoxList);

        JumbfBoxBuilder assertionStoreBuilder = new JumbfBoxBuilder();

        AssertionStoreContentType contentType = new AssertionStoreContentType();

        assertionStoreBuilder.setJumbfBoxAsRequestable();
        assertionStoreBuilder.setContentType(contentType);
        assertionStoreBuilder.setLabel(contentType.getLabel());

        assertionJumbfBoxList.stream()
                .forEach(assertionJumbfBox -> assertionStoreBuilder.appendContentBox(assertionJumbfBox));

        return assertionStoreBuilder.getResult();
    }

    private List<JumbfBox> generateAssertionJumbfBoxes(ProducerRequest producerRequest)
            throws MipamsException {
        List<JumbfBox> assertionStore = new ArrayList<>();
        String jumbfFilePath = "";

        for (Assertion assertion : producerRequest.getAssertionList()) {

            JumbfBox assertionJumbfBox = assertionFactory.convertAssertionToJumbfBox(assertion);
            JumbfBox accessRulesJumbfBox = assertion.getAccessRulesJumbfBoxOrNull();

            if (accessRulesJumbfBox != null) {

                String accessRulesJumbfBoxLabel = accessRulesJumbfBox.getDescriptionBox().getLabel();

                if (accessRulesJumbfBoxLabel == null) {
                    throw new MipamsException(
                            String.format(ProvenanceErrorMessages.EMPTY_LABEL, "Access Rules JUMBF Box"));
                }

                try {
                    jumbfFilePath = writeJumbfBoxToAFile(assertionJumbfBox);

                    String encryptedContentFilePath = encryptFileContent(producerRequest.getSigner(),
                            producerRequest.getIvHexEncoded(), jumbfFilePath);

                    String jumbfBoxLabel = assertionJumbfBox.getDescriptionBox().getLabel();

                    JumbfBox protectionJumbfBox = buildProtectionBox(producerRequest.getSigner(),
                            producerRequest.getIvHexEncoded(), jumbfBoxLabel, accessRulesJumbfBoxLabel,
                            encryptedContentFilePath);

                    assertionStore.add(protectionJumbfBox);
                    assertionStore.add(accessRulesJumbfBox);
                } finally {
                    ProvenanceUtils.deleteFile(jumbfFilePath);
                }
            } else {
                assertionStore.add(assertionJumbfBox);
            }
        }

        return assertionStore;
    }

    private String writeJumbfBoxToAFile(JumbfBox assertionJumbfBox)
            throws MipamsException {

        String jumbfBoxFileName = CoreUtils.randomStringGenerator();
        String jumbfBoxFilePath = CoreUtils.getFullPath(properties.getFileDirectory(), jumbfBoxFileName);

        try (OutputStream fos = new FileOutputStream(jumbfBoxFilePath)) {
            jumbfBoxService.writeToJumbfFile(assertionJumbfBox, fos);
        } catch (IOException e) {
            throw new MipamsException(ProvenanceErrorMessages.JUMBF_BOX_CREATION_ERROR, e);
        }

        return jumbfBoxFilePath;
    }

    private String encryptFileContent(ProvenanceSigner signer, String ivAsString, String jumbfFilePath)
            throws MipamsException {

        if (!signer.getEncryptionScheme().equals("AES-256")) {
            throw new MipamsException(ProvenanceErrorMessages.UNSUPPORTED_ENCRYPTION);
        }

        CryptoRequest encryptionRequest = new CryptoRequest();

        encryptionRequest.setCryptoMethod(signer.getEncryptionScheme());
        encryptionRequest.setIv(ivAsString);
        encryptionRequest.setContentFileUrl(jumbfFilePath);

        try {
            cryptoService.encryptDocument(signer.getEncryptionKey(), encryptionRequest);
        } catch (CryptoException e) {
            throw new MipamsException(ProvenanceErrorMessages.ENCRYPTION_ERROR, e);
        }

        return null;
    }

    private JumbfBox buildProtectionBox(ProvenanceSigner signer, String ivAsString, String assertionLabel,
            String accessRulesLabel, String encryptedContentFilePath) throws MipamsException {

        JumbfBoxBuilder builder = new JumbfBoxBuilder();

        builder.setJumbfBoxAsRequestable();
        builder.setLabel(assertionLabel);
        builder.setContentType(protectionContentType);

        ProtectionDescriptionBox pdBox = new ProtectionDescriptionBox();
        pdBox.setAes256CbcWithIvProtection();
        pdBox.setIv(DatatypeConverter.parseBase64Binary(ivAsString));
        pdBox.setArLabel(accessRulesLabel);

        BinaryDataBox bdBox = new BinaryDataBox();
        bdBox.setFileUrl(encryptedContentFilePath);

        builder.appendContentBox(pdBox);
        builder.appendContentBox(bdBox);

        return builder.getResult();
    }

    private void ensureLabelUniquenessInAssertionStore(List<JumbfBox> assertionJumbfBoxList) throws MipamsException {

        Map<String, Integer> labelOccurenceMap = computeDuplicateLabelOccurrenceMap(assertionJumbfBoxList);

        Integer occurences;
        String commonLabel;

        for (JumbfBox assertion : assertionJumbfBoxList) {
            commonLabel = assertion.getDescriptionBox().getLabel();
            occurences = labelOccurenceMap.get(commonLabel);

            if (occurences == null) {
                continue;
            }

            String uniqueLabel = String.format("%s_%d", commonLabel, occurences);
            assertion.getDescriptionBox().setLabel(uniqueLabel);
            assertion.getDescriptionBox().updateBmffHeadersBasedOnBox();

            labelOccurenceMap.put(commonLabel, --occurences);
        }
    }

    private Map<String, Integer> computeDuplicateLabelOccurrenceMap(List<JumbfBox> assertionJumbfBoxList)
            throws MipamsException {
        Integer occurences;
        String label;

        Map<String, Integer> labelOccurenceMap = new HashMap<>();

        for (JumbfBox assertion : assertionJumbfBoxList) {

            label = assertion.getDescriptionBox().getLabel();
            occurences = labelOccurenceMap.get(label);

            if (occurences == null) {
                occurences = 0;
            }

            labelOccurenceMap.put(label, ++occurences);
        }

        removeSingleOccurences(labelOccurenceMap);

        return labelOccurenceMap;
    }

    private void removeSingleOccurences(Map<String, Integer> labelOccurenceMap) {
        Integer occurences;

        for (String label : (new HashMap<>(labelOccurenceMap)).keySet()) {
            occurences = labelOccurenceMap.get(label);

            if (occurences == 1) {
                labelOccurenceMap.remove(label);
            }
        }
    }

    private List<JumbfBox> addRandomnessToAssertions(List<JumbfBox> assertionJumbfBoxList) throws MipamsException {

        int numOfRandomBytes = 32;
        String randomFileUrl, randomFileName;

        List<JumbfBox> resultAssertionJumbfBoxList = new ArrayList<>();
        JumbfBoxBuilder builder;

        for (JumbfBox jumbfBox : assertionJumbfBoxList) {
            try {
                byte[] randomBytes = cryptoService.getRandomNumber(numOfRandomBytes);

                try (InputStream is = new ByteArrayInputStream(randomBytes);) {

                    randomFileName = CoreUtils.randomStringGenerator();
                    randomFileUrl = CoreUtils.getFullPath(properties.getFileDirectory(), randomFileName);
                    CoreUtils.writeBytesFromInputStreamToFile(is, numOfRandomBytes, randomFileUrl);

                    builder = new JumbfBoxBuilder(jumbfBox);
                    builder.setPrivateField(randomFileUrl);

                    resultAssertionJumbfBoxList.add(builder.getResult());
                }

            } catch (CryptoException | IOException e) {
                throw new MipamsException(e);
            }
        }

        return resultAssertionJumbfBoxList;
    }

    public void addContentBindingAssertion(JumbfBox assertionStoreJumbfBox, String assetUrl) throws MipamsException {

        byte[] digest = ProvenanceUtils.computeSha256DigestOfFileContents(assetUrl);

        BindingAssertion assertion = new BindingAssertion("SHA-256", null, digest,
                "This digest is composed from hasing the entire digital asset");

        JumbfBox contentBindingJumbfBox = assertionFactory.convertAssertionToJumbfBox(assertion);

        assertionStoreJumbfBox.getContentBoxList().add(contentBindingJumbfBox);
    }

    public List<String> getRedactedAssertionsReferenceList(List<RedactableAssertion> redactedAssertionList)
            throws MipamsException {

        if (redactedAssertionList == null) {
            return null;
        }

        List<String> redactedAssertionUriList = new ArrayList<>();

        Integer occurences;
        String label;

        Map<String, Integer> labelOccurenceMap = new HashMap<>();

        for (Assertion assertion : redactedAssertionList) {

            label = assertionFactory.getBaseLabel(assertion);
            occurences = labelOccurenceMap.get(label);

            if (occurences == null) {
                occurences = 0;
            }

            labelOccurenceMap.put(label, ++occurences);
        }

        String updatedLabel;

        for (Assertion assertion : new ArrayList<>(redactedAssertionList)) {
            label = assertionFactory.getBaseLabel(assertion);

            if (labelOccurenceMap.get(label) == 1) {
                updatedLabel = String.format("%s_%s", label, "redacted");
                redactedAssertionUriList.add(updatedLabel);
                redactedAssertionList.remove(assertion);
            }
        }

        for (Assertion assertion : redactedAssertionList) {
            label = assertionFactory.getBaseLabel(assertion);

            occurences = labelOccurenceMap.get(label);

            String uniqueLabel = String.format("%s_%s_%d", label, "redacted", occurences);

            redactedAssertionUriList.add(uniqueLabel);
        }

        return redactedAssertionUriList;
    }

}
