package org.mipams.fake_media.entities.requests;

import java.util.List;

import org.mipams.jumbf.core.entities.JumbfBox;
import org.mipams.fake_media.entities.ClaimGenerator;
import org.mipams.fake_media.entities.ProvenanceSigner;

public class ProducerRequest {

    private String assetUrl;
    private JumbfBox manifestStoreJumbfBox;
    private List<JumbfBox> componentManifestJumbfBoxList;
    private ProvenanceSigner signer;
    private ClaimGenerator claimGenerator;
    private List<JumbfBox> assertionList;
    private List<String> redactedAssertionUriList;
    private JumbfBox credentialsStoreJumbfBox;

    public String getAssetUrl() {
        return assetUrl;
    }

    public void setAssetUrl(String assetUrl) {
        this.assetUrl = assetUrl;
    }

    public JumbfBox getManifestStoreJumbfBox() {
        return manifestStoreJumbfBox;
    }

    public void setManifestStoreJumbfBox(JumbfBox manifestStoreJumbfBox) {
        this.manifestStoreJumbfBox = manifestStoreJumbfBox;
    }

    public List<JumbfBox> getComponentManifestJumbfBoxList() {
        return componentManifestJumbfBoxList;
    }

    public void setComponentManifestJumbfBoxList(List<JumbfBox> componentManifestJumbfBoxList) {
        this.componentManifestJumbfBoxList = componentManifestJumbfBoxList;
    }

    public ProvenanceSigner getSigner() {
        return signer;
    }

    public void setSigner(ProvenanceSigner signer) {
        this.signer = signer;
    }

    public ClaimGenerator getClaimGenerator() {
        return claimGenerator;
    }

    public void setClaimGenerator(ClaimGenerator claimGenerator) {
        this.claimGenerator = claimGenerator;
    }

    public List<JumbfBox> getAssertionList() {
        return assertionList;
    }

    public void setAssertionList(List<JumbfBox> assertionList) {
        this.assertionList = assertionList;
    }

    public List<String> getRedactedAssertionUriList() {
        return redactedAssertionUriList;
    }

    public void setRedactedAssertionUriList(List<String> redactedAssertionUriList) {
        this.redactedAssertionUriList = redactedAssertionUriList;
    }

    public JumbfBox getCredentialsStoreJumbfBox() {
        return credentialsStoreJumbfBox;
    }

    public void setCredentialsStoreJumbfBox(JumbfBox credentialsStoreJumbfBox) {
        this.credentialsStoreJumbfBox = credentialsStoreJumbfBox;
    }

}
