package org.mipams.fake_media.services.producer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mipams.jumbf.core.entities.JumbfBox;
import org.mipams.jumbf.core.entities.JumbfBoxBuilder;
import org.mipams.jumbf.core.services.boxes.JumbfBoxService;
import org.mipams.jumbf.core.util.CoreUtils;
import org.mipams.jumbf.core.util.MipamsException;
import org.mipams.jumbf.crypto.entities.CryptoException;
import org.mipams.jumbf.crypto.services.CryptoService;
import org.mipams.jumbf.privacy_security.services.content_types.ProtectionContentType;
import org.mipams.fake_media.entities.ProvenanceMetadata;
import org.mipams.fake_media.entities.assertions.BindingAssertion;
import org.mipams.fake_media.services.AssertionFactory;
import org.mipams.fake_media.services.content_types.AssertionStoreContentType;
import org.mipams.fake_media.utils.ProvenanceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AssertionStoreProducer {

    @Autowired
    AssertionStoreContentType assertionStoreContentType;

    @Autowired
    AssertionFactory assertionFactory;

    @Autowired
    JumbfBoxService jumbfBoxService;

    @Autowired
    ProtectionContentType protectionContentType;

    @Autowired
    CryptoService cryptoService;

    public JumbfBox produce(List<JumbfBox> requestAssertionJumbfBoxList, ProvenanceMetadata provenanceMetadata)
            throws MipamsException {

        List<JumbfBox> deterministicAssertionJumbfBoxList = new ArrayList<>(requestAssertionJumbfBoxList);

        ensureLabelUniquenessInAssertionStore(deterministicAssertionJumbfBoxList);

        List<JumbfBox> assertionJumbfBoxList = addRandomnessToAssertions(deterministicAssertionJumbfBoxList,
                provenanceMetadata);

        JumbfBoxBuilder assertionStoreBuilder = new JumbfBoxBuilder();

        AssertionStoreContentType contentType = new AssertionStoreContentType();

        assertionStoreBuilder.setJumbfBoxAsRequestable();
        assertionStoreBuilder.setContentType(contentType);
        assertionStoreBuilder.setLabel(contentType.getLabel());

        assertionJumbfBoxList.stream()
                .forEach(assertionJumbfBox -> assertionStoreBuilder.appendContentBox(assertionJumbfBox));

        return assertionStoreBuilder.getResult();
    }

    // private List<JumbfBox> generateAssertionJumbfBoxes(ProducerRequest
    // producerRequest,
    // ProvenanceMetadata provenanceMetadata) throws MipamsException {
    // List<JumbfBox> assertionStore = new ArrayList<>();
    // String jumbfFilePath = "";

    // for (JumbfBox assertion : producerRequest.getAssertionList()) {

    // if (assertion.isProtectedFlagSet()) {

    // try {
    // jumbfFilePath = writeJumbfBoxToAFile(assertionJumbfBox, provenanceMetadata);

    // String encryptedContentFilePath =
    // encryptFileContent(producerRequest.getSigner(),
    // producerRequest.getIvHexEncoded(), jumbfFilePath);

    // String jumbfBoxLabel = assertionJumbfBox.getDescriptionBox().getLabel();

    // JumbfBox accessRulesJumbfBox = assertion.getAccessRulesJumbfBoxOrNull();
    // String accessRulesJumbfBoxLabel = null;

    // if (accessRulesJumbfBox != null) {

    // accessRulesJumbfBoxLabel =
    // accessRulesJumbfBox.getDescriptionBox().getLabel();
    // if (accessRulesJumbfBoxLabel == null) {
    // throw new MipamsException(
    // String.format(ProvenanceErrorMessages.EMPTY_LABEL, "Access Rules JUMBF
    // Box"));
    // }

    // assertionStore.add(accessRulesJumbfBox);
    // }

    // JumbfBox protectionJumbfBox = buildProtectionBox(producerRequest.getSigner(),
    // producerRequest.getIvHexEncoded(), jumbfBoxLabel, accessRulesJumbfBoxLabel,
    // encryptedContentFilePath);

    // assertionStore.add(protectionJumbfBox);

    // } finally {
    // ProvenanceUtils.deleteFile(jumbfFilePath);
    // }
    // } else {
    // assertionStore.add(assertionJumbfBox);
    // }
    // }

    // return assertionStore;
    // }

    // private String writeJumbfBoxToAFile(JumbfBox assertionJumbfBox,
    // ProvenanceMetadata provenanceMetadata)
    // throws MipamsException {

    // String jumbfBoxFileName = CoreUtils.randomStringGenerator();
    // String jumbfBoxFilePath =
    // CoreUtils.getFullPath(provenanceMetadata.getParentDirectory(),
    // jumbfBoxFileName);

    // try (OutputStream fos = new FileOutputStream(jumbfBoxFilePath)) {
    // jumbfBoxService.writeToJumbfFile(assertionJumbfBox, fos);
    // } catch (IOException e) {
    // throw new MipamsException(ProvenanceErrorMessages.JUMBF_BOX_CREATION_ERROR,
    // e);
    // }

    // return jumbfBoxFilePath;
    // }

    // private String encryptFileContent(ProvenanceSigner signer, String ivAsString,
    // String jumbfFilePath)
    // throws MipamsException {

    // if (!signer.getEncryptionScheme().equals("AES-256")) {
    // throw new MipamsException(ProvenanceErrorMessages.UNSUPPORTED_ENCRYPTION);
    // }

    // CryptoRequest encryptionRequest = new CryptoRequest();

    // encryptionRequest.setCryptoMethod(signer.getEncryptionScheme());
    // encryptionRequest.setIv(ivAsString);
    // encryptionRequest.setContentFileUrl(jumbfFilePath);

    // try {
    // cryptoService.encryptDocument(signer.getEncryptionKey(), encryptionRequest);
    // } catch (CryptoException e) {
    // throw new MipamsException(ProvenanceErrorMessages.ENCRYPTION_ERROR, e);
    // }

    // return null;
    // }

    // private JumbfBox buildProtectionBox(ProvenanceSigner signer, String
    // ivAsString, String assertionLabel,
    // String accessRulesLabel, String encryptedContentFilePath) throws
    // MipamsException {

    // JumbfBoxBuilder builder = new JumbfBoxBuilder();

    // builder.setJumbfBoxAsRequestable();
    // builder.setLabel(assertionLabel);
    // builder.setContentType(protectionContentType);

    // ProtectionDescriptionBox pdBox = new ProtectionDescriptionBox();
    // pdBox.setAes256CbcWithIvProtection();
    // pdBox.setIv(DatatypeConverter.parseBase64Binary(ivAsString));
    // pdBox.setArLabel(accessRulesLabel);

    // BinaryDataBox bdBox = new BinaryDataBox();
    // bdBox.setFileUrl(encryptedContentFilePath);

    // builder.appendContentBox(pdBox);
    // builder.appendContentBox(bdBox);

    // return builder.getResult();
    // }

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

            String uniqueLabel = String.format(ProvenanceUtils.ASSERTION_LABEL_MULTIPLE_INSTANCE_FORMAT, commonLabel,
                    occurences);
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

    private List<JumbfBox> addRandomnessToAssertions(List<JumbfBox> assertionJumbfBoxList,
            ProvenanceMetadata provenanceMetadata) throws MipamsException {

        int numOfRandomBytes = 32;
        String randomFileUrl, randomFileName;

        List<JumbfBox> resultAssertionJumbfBoxList = new ArrayList<>();
        JumbfBoxBuilder builder;

        for (JumbfBox jumbfBox : assertionJumbfBoxList) {
            try {
                byte[] randomBytes = cryptoService.getRandomNumber(numOfRandomBytes);

                try (InputStream is = new ByteArrayInputStream(randomBytes);) {

                    randomFileName = CoreUtils.randomStringGenerator();
                    randomFileUrl = CoreUtils.getFullPath(provenanceMetadata.getParentDirectory(), randomFileName);
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

    public void addContentBindingAssertion(JumbfBox assertionStoreJumbfBox, String assetUrl,
            ProvenanceMetadata metadata) throws MipamsException {

        byte[] digest = ProvenanceUtils.computeSha256DigestOfFileContents(assetUrl);

        BindingAssertion assertion = new BindingAssertion("SHA-256", null, digest,
                "This digest is composed from hasing the entire digital asset");

        JumbfBox contentBindingJumbfBox = assertionFactory.convertAssertionToJumbfBox(assertion, metadata);

        assertionStoreJumbfBox.getContentBoxList().add(contentBindingJumbfBox);
    }
}
