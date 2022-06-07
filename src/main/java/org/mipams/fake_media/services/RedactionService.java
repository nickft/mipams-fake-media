package org.mipams.fake_media.services;

import java.util.List;

import org.mipams.fake_media.entities.ProvenanceErrorMessages;
import org.mipams.fake_media.services.content_types.AssertionStoreContentType;
import org.mipams.fake_media.utils.ProvenanceUtils;
import org.mipams.jumbf.core.entities.BmffBox;
import org.mipams.jumbf.core.entities.JumbfBox;
import org.mipams.jumbf.core.entities.JumbfBoxBuilder;
import org.mipams.jumbf.core.util.MipamsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RedactionService {

    @Autowired
    AssertionStoreContentType assertionStoreContentType;

    public JumbfBox redactAssertionsFromJumbfBox(JumbfBox manifestJumbfBox, List<String> redactedAssertionList)
            throws MipamsException {

        JumbfBox assertionStoreJumbfBox = ProvenanceUtils.getProvenanceJumbfBox(manifestJumbfBox,
                assertionStoreContentType);

        JumbfBox redactedManifest = (new JumbfBoxBuilder(manifestJumbfBox)).getResult();
        JumbfBox redactedAssertionStoreJumbfBox = ProvenanceUtils.getProvenanceJumbfBox(redactedManifest,
                assertionStoreContentType);

        String manifestLabel = redactedManifest.getDescriptionBox().getLabel();
        String assertionStoreLabel = redactedAssertionStoreJumbfBox.getDescriptionBox().getLabel();

        int redactedAssertionsCounter = 0;
        String uriReference, assertionLabel;

        for (BmffBox contentBox : assertionStoreJumbfBox.getContentBoxList()) {
            JumbfBox assertionJumbfBox = (JumbfBox) contentBox;
            assertionLabel = assertionJumbfBox.getDescriptionBox().getLabel();

            uriReference = ProvenanceUtils.getProvenanceJumbfURL(manifestLabel, assertionStoreLabel, assertionLabel);

            if (redactedAssertionList.contains(uriReference)) {
                redactedAssertionsCounter++;
                redactedAssertionStoreJumbfBox.getContentBoxList().remove(assertionJumbfBox);
            }
        }

        if (redactedAssertionsCounter != redactedAssertionList.size()) {
            throw new MipamsException(
                    getRedactionMismatchMessage(redactedAssertionsCounter, redactedAssertionList.size()));
        }

        return redactedManifest;
    }

    private String getRedactionMismatchMessage(int actual, int expected) {
        return String.format(ProvenanceErrorMessages.REDACTION_PROCESS_MISMATCH, expected, actual);
    }

}
