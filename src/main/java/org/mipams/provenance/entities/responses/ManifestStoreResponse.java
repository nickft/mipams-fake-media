package org.mipams.provenance.entities.responses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mipams.jumbf.entities.BmffBox;
import org.mipams.jumbf.entities.JumbfBox;
import org.mipams.jumbf.util.MipamsException;
import org.mipams.provenance.entities.Claim;
import org.mipams.provenance.entities.ClaimSignature;
import org.mipams.provenance.services.consumer.ClaimConsumer;
import org.mipams.provenance.services.consumer.ClaimSignatureConsumer;
import org.mipams.provenance.services.content_types.AssertionStoreContentType;
import org.mipams.provenance.services.content_types.ClaimContentType;
import org.mipams.provenance.services.content_types.ClaimSignatureContentType;
import org.mipams.provenance.utils.ProvenanceUtils;

public class ManifestStoreResponse {
    private Map<String, ManifestResponse> manifestResponseMap;

    public ManifestStoreResponse() {
        this.manifestResponseMap = new HashMap<>();
    }

    public void addManifestResponse(JumbfBox manifestJumbfBox) throws MipamsException {

        String manifestId = manifestJumbfBox.getDescriptionBox().getLabel();

        JumbfBox claim = ProvenanceUtils.getProvenanceJumbfBox(manifestJumbfBox, new ClaimContentType());
        JumbfBox claimSignatue = ProvenanceUtils.getProvenanceJumbfBox(manifestJumbfBox,
                new ClaimSignatureContentType());

        List<JumbfBox> assertionJumbfBoxList = getAssertionStoreContent(manifestJumbfBox);

        DefaultManifestResponse manifestResponse = new DefaultManifestResponse();
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

    public Map<String, ManifestResponse> getManifestResponseMap() {
        return manifestResponseMap;
    };

}
