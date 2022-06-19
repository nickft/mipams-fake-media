package org.mipams.fake_media.entities.responses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mipams.fake_media.entities.Claim;
import org.mipams.fake_media.entities.ClaimSignature;
import org.mipams.fake_media.services.consumer.ClaimConsumer;
import org.mipams.fake_media.services.consumer.ClaimSignatureConsumer;
import org.mipams.fake_media.services.content_types.AssertionStoreContentType;
import org.mipams.fake_media.services.content_types.ClaimContentType;
import org.mipams.fake_media.services.content_types.ClaimSignatureContentType;
import org.mipams.fake_media.utils.ProvenanceUtils;
import org.mipams.jumbf.core.entities.BmffBox;
import org.mipams.jumbf.core.entities.JumbfBox;
import org.mipams.jumbf.core.util.MipamsException;

import lombok.Getter;

public class ManifestStoreResponse {
    private @Getter Map<String, ManifestResponse> manifestResponseMap;

    public ManifestStoreResponse() {
        this.manifestResponseMap = new HashMap<>();
    }

    public void addManifestResponse(JumbfBox manifestJumbfBox) throws MipamsException {

        String manifestId = manifestJumbfBox.getDescriptionBox().getLabel();

        JumbfBox claim = ProvenanceUtils.getProvenanceJumbfBox(manifestJumbfBox, new ClaimContentType());
        JumbfBox claimSignatue = ProvenanceUtils.getProvenanceJumbfBox(manifestJumbfBox,
                new ClaimSignatureContentType());

        List<JumbfBox> assertionJumbfBoxList = getAssertionStoreContent(manifestJumbfBox);

        ManifestResponse manifestResponse = new ManifestResponse();
        manifestResponse.setAssertionJumbfBoxList(assertionJumbfBoxList);
        manifestResponse.setClaim(getCertificateContent(claim));
        manifestResponse.setClaimSignature(getCertificateSignatureContent(claimSignatue));

        getManifestResponseMap().put(manifestId, manifestResponse);
    }

    private Claim getCertificateContent(JumbfBox claimJumbfBox) throws MipamsException {
        ClaimConsumer claimConsumer = new ClaimConsumer();
        return claimConsumer.deserializeClaimJumbfBox(claimJumbfBox);
    }

    private ClaimSignature getCertificateSignatureContent(JumbfBox claimSignatureJumbfBox) throws MipamsException {

        ClaimSignatureConsumer claimSignatureConsumer = new ClaimSignatureConsumer();

        return claimSignatureConsumer
                .deserializeClaimSignatureJumbfBox(claimSignatureJumbfBox);
    }

    private List<JumbfBox> getAssertionStoreContent(JumbfBox manifestJumbfBox) throws MipamsException {
        JumbfBox assertionStore = ProvenanceUtils.getProvenanceJumbfBox(manifestJumbfBox,
                new AssertionStoreContentType());

        List<JumbfBox> result = new ArrayList<>();

        for (BmffBox contentBox : assertionStore.getContentBoxList()) {
            result.add((JumbfBox) contentBox);
        }
        return result;
    }
}
