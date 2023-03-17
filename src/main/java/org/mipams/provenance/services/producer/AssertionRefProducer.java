package org.mipams.provenance.services.producer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.mipams.jumbf.entities.BmffBox;
import org.mipams.jumbf.entities.JumbfBox;
import org.mipams.jumbf.services.CoreGeneratorService;
import org.mipams.jumbf.util.CoreUtils;
import org.mipams.jumbf.util.MipamsException;
import org.mipams.privsec.entities.ProtectionDescriptionBox;
import org.mipams.privsec.services.content_types.ProtectionContentType;
import org.mipams.provenance.entities.HashedUriReference;
import org.mipams.provenance.utils.ProvenanceUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AssertionRefProducer {

    private static final Logger logger = Logger.getLogger(AssertionRefProducer.class.getName());

    @Autowired
    ProtectionContentType protectionContentType;

    @Autowired
    CoreGeneratorService coreGeneratorService;

    public List<HashedUriReference> getAssertionReferenceListFromAssertionStore(String manifestId,
            JumbfBox assertionStore) throws MipamsException {

        List<HashedUriReference> result = new ArrayList<>();

        List<String> accessRulesJumbfBoxLabelList = getAccessRulesBoxLabelList(assertionStore);
        Map<String, JumbfBox> contentBoxMap = initializeContentBoxMap(assertionStore);

        for (BmffBox contentBox : assertionStore.getContentBoxList()) {

            JumbfBox jumbfBox = (JumbfBox) contentBox;

            if (jumbfBoxContainsAccessRules(jumbfBox, accessRulesJumbfBoxLabelList)) {
                continue;
            }

            byte[] digest = calculateDigestForJumbfBox(jumbfBox, contentBoxMap);
            String uri = computeUriForAssertion(manifestId, assertionStore, jumbfBox);
            HashedUriReference ref = new HashedUriReference(digest, uri, HashedUriReference.SUPPORTED_HASH_ALGORITHM);
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
            tempFilePath = CoreUtils.createTempFile(tempFile, CoreUtils.JUMBF_FILENAME_SUFFIX);

            List<JumbfBox> boxesToHash = getBoxesToHash(jumbfBox, contentBoxMap);

            logger.info("Boxes to hash length: " + boxesToHash.size());
            logger.info(boxesToHash.toString());

            coreGeneratorService.generateJumbfMetadataToFile(boxesToHash, tempFilePath);

            return ProvenanceUtils.computeSha256DigestOfFileContents(tempFilePath);

        } finally {
            CoreUtils.deleteFile(tempFilePath);
        }
    }

    private List<JumbfBox> getBoxesToHash(JumbfBox jumbfBox, Map<String, JumbfBox> contentBoxMap)
            throws MipamsException {

        List<JumbfBox> boxesToHash = new ArrayList<>();

        if (isProtectionContentTypeJumbfBox(jumbfBox)) {
            ProtectionDescriptionBox pdBox = (ProtectionDescriptionBox) jumbfBox.getContentBoxList().get(0);

            boxesToHash.add(jumbfBox);

            if (pdBox.getArLabel() != null) {
                JumbfBox accessRulesJumbfBox = contentBoxMap.get(pdBox.getArLabel());

                if (accessRulesJumbfBox == null) {
                    throw new MipamsException(
                            "Could not find access rules JUMBF box with label: " + pdBox.getArLabel());
                }

                boxesToHash.add(accessRulesJumbfBox);
            }
        } else {
            boxesToHash.add(jumbfBox);
        }

        return boxesToHash;
    }

    private boolean isProtectionContentTypeJumbfBox(JumbfBox jumbfBox) {
        return jumbfBox.getDescriptionBox().getUuid().equals(protectionContentType.getContentTypeUuid());
    }

    private String computeUriForAssertion(String manifestId, JumbfBox assertionStore, JumbfBox jumbfBox) {
        String assertionStoreLabel = assertionStore.getDescriptionBox().getLabel();
        String assertionLabel = jumbfBox.getDescriptionBox().getLabel();

        return ProvenanceUtils.getProvenanceJumbfURL(manifestId, assertionStoreLabel, assertionLabel);
    }
}
