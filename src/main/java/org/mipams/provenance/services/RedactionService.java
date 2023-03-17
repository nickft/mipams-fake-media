package org.mipams.provenance.services;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.mipams.jumbf.entities.BmffBox;
import org.mipams.jumbf.entities.JumbfBox;
import org.mipams.jumbf.entities.JumbfBoxBuilder;
import org.mipams.jumbf.util.MipamsException;
import org.mipams.provenance.entities.ProvenanceErrorMessages;
import org.mipams.provenance.services.content_types.AssertionStoreContentType;
import org.mipams.provenance.utils.ProvenanceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RedactionService {

    @Autowired
    AssertionStoreContentType assertionStoreContentType;

    public JumbfBox redactAssertionsFromJumbfBox(JumbfBox manifestJumbfBox, List<String> redactedAssertionList)
            throws MipamsException {

        JumbfBoxBuilder redactedManifestJumbfBoxBuilder = new JumbfBoxBuilder(manifestJumbfBox);

        JumbfBox assertionStoreJumbfBox = ProvenanceUtils.getProvenanceJumbfBox(manifestJumbfBox,
                assertionStoreContentType);

        redactedManifestJumbfBoxBuilder.removeContentBox(assertionStoreJumbfBox);

        JumbfBox redactedAssertionStoreJumbfBox = redactFromAssertionStore(
                manifestJumbfBox.getDescriptionBox().getLabel(), assertionStoreJumbfBox, redactedAssertionList);

        redactedManifestJumbfBoxBuilder.appendContentBox(redactedAssertionStoreJumbfBox);

        return redactedManifestJumbfBoxBuilder.getResult();
    }

    private JumbfBox redactFromAssertionStore(String manifestId, JumbfBox assertionStoreJumbfBox,
            List<String> redactedAssertionReferenceList) throws MipamsException {

        JumbfBoxBuilder redactedAssertionStoreJumbfBoxBuilder = new JumbfBoxBuilder(assertionStoreJumbfBox);

        List<String> filterdReferenceList = filterAssertionListBasedOnManiestUri(manifestId,
                redactedAssertionReferenceList);

        String assertionStoreLabel = assertionStoreJumbfBox.getDescriptionBox().getLabel();

        int redactedAssertionsCounter = 0;
        String uriReference, assertionLabel;

        for (BmffBox contentBox : assertionStoreJumbfBox.getContentBoxList()) {
            JumbfBox assertionJumbfBox = (JumbfBox) contentBox;
            assertionLabel = assertionJumbfBox.getDescriptionBox().getLabel();

            uriReference = ProvenanceUtils.getProvenanceJumbfURL(manifestId, assertionStoreLabel, assertionLabel);

            if (filterdReferenceList.contains(uriReference)) {
                redactedAssertionsCounter++;
            } else {
                redactedAssertionStoreJumbfBoxBuilder.appendContentBox(contentBox);
            }
        }

        if (redactedAssertionsCounter != filterdReferenceList.size()) {
            throw new MipamsException(
                    getRedactionMismatchMessage(redactedAssertionsCounter, filterdReferenceList.size()));
        }

        return redactedAssertionStoreJumbfBoxBuilder.getResult();
    }

    private List<String> filterAssertionListBasedOnManiestUri(String manifestId, List<String> assertionReferenceList) {
        String manifestUri = ProvenanceUtils.getProvenanceJumbfURL(manifestId);

        List<String> filteredList = new ArrayList<>(assertionReferenceList);
        return filteredList.stream().filter(uri -> uri.startsWith(manifestUri)).collect(Collectors.toList());

    }

    private String getRedactionMismatchMessage(int actual, int expected) {
        return String.format(ProvenanceErrorMessages.REDACTION_PROCESS_MISMATCH, expected, actual);
    }

}
