package org.mipams.fake_media.entities.responses;

import java.util.ArrayList;
import java.util.List;

import org.mipams.fake_media.entities.Claim;
import org.mipams.fake_media.entities.ClaimSignature;
import org.mipams.jumbf.core.entities.JumbfBox;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
public class ManifestResponse {
    private @Getter @Setter List<JumbfBox> assertionJumbfBoxList;
    private @Getter @Setter Claim claim;
    private @Getter @Setter ClaimSignature claimSignature;

    public ManifestResponse(ManifestResponse templateResponse) {
        setAssertionJumbfBoxList(new ArrayList<>(templateResponse.getAssertionJumbfBoxList()));
        setClaim(templateResponse.getClaim());
        setClaimSignature(templateResponse.getClaimSignature());

    }
}
