package org.mipams.fake_media.services.producer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mipams.jumbf.core.entities.BmffBox;
import org.mipams.jumbf.core.entities.JumbfBox;
import org.mipams.jumbf.core.services.CoreGeneratorService;
import org.mipams.jumbf.core.util.CoreUtils;
import org.mipams.jumbf.core.util.MipamsException;
import org.mipams.jumbf.core.util.Properties;
import org.mipams.jumbf.privacy_security.entities.ProtectionDescriptionBox;
import org.mipams.jumbf.privacy_security.services.content_types.ProtectionContentType;
import org.mipams.fake_media.entities.assertions.AssertionRef;
import org.mipams.fake_media.utils.ProvenanceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AssertionRefProducer {

    public static final String HASH_ALGORITHM = "SHA-256";

    @Autowired
    Properties properties;

    @Autowired
    ProtectionContentType protectionContentType;

    @Autowired
    CoreGeneratorService coreGeneratorService;

    public List<AssertionRef> getAssertionReferenceListFromAssertionStore(String manifestId, JumbfBox assertionStore)
            throws MipamsException {

        List<AssertionRef> result = new ArrayList<>();

        List<String> accessRulesJumbfBoxLabelList = getAccessRulesBoxLabelList(assertionStore);
        Map<String, JumbfBox> contentBoxMap = initializeContentBoxMap(assertionStore);

        for (BmffBox contentBox : assertionStore.getContentBoxList()) {

            JumbfBox jumbfBox = (JumbfBox) contentBox;

            if (jumbfBoxContainsAccessRules(jumbfBox, accessRulesJumbfBoxLabelList)) {
                continue;
            }

            byte[] digest = calculateDigestForJumbfBox(jumbfBox, contentBoxMap);
            String uri = computeUriForAssertion(manifestId, assertionStore, jumbfBox);
            AssertionRef ref = new AssertionRef(digest, uri, HASH_ALGORITHM);
            result.add(ref);
        }
        return result;
    }

    private List<String> getAccessRulesBoxLabelList(JumbfBox assertionStore) {

        List<String> accessRulesJumbfBoxLabelList = new ArrayList<>();

        for (BmffBox contentBox : assertionStore.getContentBoxList()) {
            JumbfBox jumbfBox = (JumbfBox) contentBox;

            if (jumbfBox.getDescriptionBox().getUuid().equals(protectionContentType.getContentTypeUuid())) {
                ProtectionDescriptionBox pdBox = (ProtectionDescriptionBox) jumbfBox.getContentBoxList().get(0);
                accessRulesJumbfBoxLabelList.add(pdBox.getArLabel());
            }
        }

        return accessRulesJumbfBoxLabelList;
    }

    private Map<String, JumbfBox> initializeContentBoxMap(JumbfBox assertionStore) {

        Map<String, JumbfBox> contentBoxMap = new HashMap<>();

        for (BmffBox contentBox : assertionStore.getContentBoxList()) {
            JumbfBox jumbfBox = (JumbfBox) contentBox;

            contentBoxMap.put(jumbfBox.getDescriptionBox().getLabel(), (JumbfBox) contentBox);
        }

        return contentBoxMap;
    }

    private boolean jumbfBoxContainsAccessRules(JumbfBox jumbfBox, List<String> accessRulesJumbfBoxLabelList) {
        return accessRulesJumbfBoxLabelList.contains(jumbfBox.getDescriptionBox().getLabel());
    }

    private byte[] calculateDigestForJumbfBox(JumbfBox jumbfBox, Map<String, JumbfBox> contentBoxMap)
            throws MipamsException {

        String tempFilePath = "";
        try {

            String tempFile = CoreUtils.randomStringGenerator();
            tempFilePath = CoreUtils.getFullPath(properties.getFileDirectory(), tempFile);

            List<JumbfBox> boxesToHash = getBoxesToHash(jumbfBox, contentBoxMap);

            coreGeneratorService.generateJumbfMetadataToFile(boxesToHash, tempFilePath);

            return ProvenanceUtils.computeSha256DigestOfFileContents(tempFilePath);

        } finally {
            deleteFile(tempFilePath);
        }
    }

    private List<JumbfBox> getBoxesToHash(JumbfBox jumbfBox, Map<String, JumbfBox> contentBoxMap) {

        List<JumbfBox> boxesToHash;

        if (isProtectionContentTypeJumbfBox(jumbfBox)) {
            ProtectionDescriptionBox pdBox = (ProtectionDescriptionBox) jumbfBox.getContentBoxList().get(0);
            JumbfBox accessRulesJumbfBox = contentBoxMap.get(pdBox.getArLabel());

            boxesToHash = List.of(jumbfBox, accessRulesJumbfBox);
        } else {
            boxesToHash = List.of(jumbfBox);
        }

        return boxesToHash;
    }

    private boolean isProtectionContentTypeJumbfBox(JumbfBox jumbfBox) {
        return jumbfBox.getDescriptionBox().getUuid().equals(protectionContentType.getContentTypeUuid());
    }

    private void deleteFile(String tempFilePath) {
        File f = new File(tempFilePath);
        if (f.exists()) {
            f.delete();
        }
    }

    private String computeUriForAssertion(String manifestId, JumbfBox assertionStore, JumbfBox jumbfBox) {
        String assertionStoreLabel = assertionStore.getDescriptionBox().getLabel();
        String assertionLabel = jumbfBox.getDescriptionBox().getLabel();

        return ProvenanceUtils.getProvenanceJumbfURL(manifestId, assertionStoreLabel, assertionLabel);
    }
}