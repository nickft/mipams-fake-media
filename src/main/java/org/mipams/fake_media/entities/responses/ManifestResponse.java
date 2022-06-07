package org.mipams.fake_media.entities.responses;

import java.util.ArrayList;
import java.util.List;

import org.mipams.jumbf.core.entities.JumbfBox;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
public class ManifestResponse {
    private @Getter @Setter List<JumbfBox> assertionJumbfBoxList;
    private @Getter @Setter JumbfBox claim;
    private @Getter @Setter byte[] claimCertificate;

    public ManifestResponse(ManifestResponse templateResponse) {
        setAssertionJumbfBoxList(new ArrayList<>(templateResponse.getAssertionJumbfBoxList()));
        setClaim(templateResponse.getClaim());
        setClaimCertificate(templateResponse.getClaimCertificate().clone());

    }
}
