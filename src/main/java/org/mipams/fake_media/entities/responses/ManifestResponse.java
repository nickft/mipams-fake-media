package org.mipams.fake_media.entities.responses;

import java.util.ArrayList;
import java.util.List;

import org.mipams.fake_media.entities.Claim;
import org.mipams.fake_media.entities.ClaimSignature;
import org.mipams.jumbf.core.entities.JumbfBox;

public class ManifestResponse {
    private List<JumbfBox> assertionJumbfBoxList;
    private Claim claim;
    private ClaimSignature claimSignature;

    public ManifestResponse() {

    }

    public ManifestResponse(ManifestResponse templateResponse) {
        setAssertionJumbfBoxList(new ArrayList<>(templateResponse.getAssertionJumbfBoxList()));
        setClaim(templateResponse.getClaim());
        setClaimSignature(templateResponse.getClaimSignature());
    }

    public List<JumbfBox> getAssertionJumbfBoxList() {
        return assertionJumbfBoxList;
    }

    public void setAssertionJumbfBoxList(List<JumbfBox> assertionJumbfBoxList) {
        this.assertionJumbfBoxList = assertionJumbfBoxList;
    }

    public Claim getClaim() {
        return claim;
    }

    public void setClaim(Claim claim) {
        this.claim = claim;
    }

    public ClaimSignature getClaimSignature() {
        return claimSignature;
    }

    public void setClaimSignature(ClaimSignature claimSignature) {
        this.claimSignature = claimSignature;
    }
}
