package org.mipams.provenance.entities.responses;

import java.util.ArrayList;
import java.util.List;

import org.mipams.jumbf.entities.JumbfBox;
import org.mipams.provenance.entities.Claim;
import org.mipams.provenance.entities.ClaimSignature;

public class DefaultManifestResponse implements ManifestResponse {
    private List<JumbfBox> assertionJumbfBoxList;
    private Claim claim;
    private ClaimSignature claimSignature;

    public DefaultManifestResponse() {

    }

    public DefaultManifestResponse(DefaultManifestResponse templateResponse) {
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
