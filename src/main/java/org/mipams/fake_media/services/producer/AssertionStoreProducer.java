package org.mipams.fake_media.services.producer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.mipams.jumbf.core.entities.BinaryDataBox;
import org.mipams.jumbf.core.entities.JsonBox;
import org.mipams.jumbf.core.entities.JumbfBox;
import org.mipams.jumbf.core.entities.JumbfBoxBuilder;
import org.mipams.jumbf.core.services.boxes.JumbfBoxService;
import org.mipams.jumbf.core.util.MipamsException;
import org.mipams.jumbf.crypto.entities.CryptoException;
import org.mipams.jumbf.crypto.services.CryptoService;
import org.mipams.jumbf.privacy_security.services.content_types.ProtectionContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.mipams.fake_media.entities.ProvenanceMetadata;
import org.mipams.fake_media.entities.assertions.BindingAssertion;
import org.mipams.fake_media.services.AssertionFactory;
import org.mipams.fake_media.services.content_types.AssertionStoreContentType;
import org.mipams.fake_media.utils.ProvenanceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AssertionStoreProducer {

    private static final Logger logger = LoggerFactory.getLogger(AssertionStoreProducer.class);

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

        List<JumbfBox> assertionJumbfBoxList = addEntropyToAssertions(deterministicAssertionJumbfBoxList,
                provenanceMetadata);

        AssertionStoreContentType contentType = new AssertionStoreContentType();

        JumbfBoxBuilder assertionStoreBuilder = new JumbfBoxBuilder(contentType);
        assertionStoreBuilder.setJumbfBoxAsRequestable();
        assertionStoreBuilder.setLabel(contentType.getLabel());

        assertionJumbfBoxList.stream()
                .forEach(assertionJumbfBox -> assertionStoreBuilder.appendContentBox(assertionJumbfBox));

        return assertionStoreBuilder.getResult();
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

    private List<JumbfBox> addEntropyToAssertions(List<JumbfBox> assertionJumbfBoxList,
            ProvenanceMetadata provenanceMetadata) throws MipamsException {

        int numOfRandomBytes = 16;

        List<JumbfBox> resultAssertionJumbfBoxList = new ArrayList<>();
        JumbfBoxBuilder builder;

        for (JumbfBox jumbfBox : assertionJumbfBoxList) {
            try {

                byte[] randomBytes = cryptoService.getRandomNumber(numOfRandomBytes);
                JsonBox jsonBox = new JsonBox();
                jsonBox.setContent(randomBytes);
                jsonBox.updateBmffHeadersBasedOnBox();

                builder = new JumbfBoxBuilder(jumbfBox);
                builder.setPrivateField(jsonBox);

                resultAssertionJumbfBoxList.add(builder.getResult());
            } catch (CryptoException e) {
                throw new MipamsException(e);
            }
        }

        return resultAssertionJumbfBoxList;
    }

    public JumbfBox getAssertionStoreWithContentBindingAssertion(JumbfBox assertionStoreJumbfBox, String assetUrl,
            ProvenanceMetadata metadata) throws MipamsException {

        JumbfBoxBuilder newAssertionStoreBuilder = new JumbfBoxBuilder(assertionStoreJumbfBox);

        byte[] digest = ProvenanceUtils.computeSha256DigestOfFileContents(assetUrl);
        logger.debug(String.format("Hashing file %s : %s", assetUrl, DatatypeConverter.printHexBinary(digest)));

        BindingAssertion assertion = new BindingAssertion("SHA-256", null, digest,
                "This digest is composed from hasing the entire digital asset");

        JumbfBox contentBindingJumbfBox = assertionFactory.convertAssertionToJumbfBox(assertion, metadata);

        newAssertionStoreBuilder.appendContentBox(contentBindingJumbfBox);

        return newAssertionStoreBuilder.getResult();
    }
}
